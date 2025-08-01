/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groundplatform.android.ui.home.mapcontainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import org.groundplatform.android.R
import org.groundplatform.android.coroutines.ApplicationScope
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.coroutines.MainDispatcher
import org.groundplatform.android.databinding.BasemapLayoutBinding
import org.groundplatform.android.databinding.MenuButtonBinding
import org.groundplatform.android.model.locationofinterest.LOI_NAME_PROPERTY
import org.groundplatform.android.proto.Survey.DataSharingTerms
import org.groundplatform.android.repository.SubmissionRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.home.DataSharingTermsDialog
import org.groundplatform.android.ui.home.HomeScreenFragmentDirections
import org.groundplatform.android.ui.home.HomeScreenViewModel
import org.groundplatform.android.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import org.groundplatform.android.ui.home.mapcontainer.jobs.DataCollectionEntryPointData
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComposables
import org.groundplatform.android.ui.home.mapcontainer.jobs.SelectedLoiSheetData
import org.groundplatform.android.ui.map.MapFragment
import org.groundplatform.android.usecases.datasharingterms.GetDataSharingTermsUseCase
import org.groundplatform.android.util.createComposeView
import org.groundplatform.android.util.renderComposableDialog
import timber.log.Timber

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint
class HomeScreenMapContainerFragment : AbstractMapContainerFragment() {

  @Inject lateinit var ephemeralPopups: EphemeralPopups
  @Inject lateinit var submissionRepository: SubmissionRepository
  @Inject lateinit var userRepository: UserRepository
  @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
  @Inject @MainDispatcher lateinit var mainDispatcher: CoroutineDispatcher
  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope

  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var binding: BasemapLayoutBinding
  private lateinit var jobMapComposables: JobMapComposables
  private lateinit var infoPopup: EphemeralPopups.InfoPopup

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapContainerViewModel = getViewModel(HomeScreenMapContainerViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
    jobMapComposables = JobMapComposables()
    jobMapComposables.setSelectedFeature { mapContainerViewModel.selectLocationOfInterest(it) }

    // Bind data for cards
    mapContainerViewModel.processDataCollectionEntryPoints().launchWhenStartedAndCollect {
      (loiCard, jobCards) ->
      jobMapComposables.updateData(loiCard, jobCards)
    }

    map.featureClicks.launchWhenStartedAndCollect { mapContainerViewModel.onFeatureClicked(it) }
  }

  private fun hasValidTasks(cardUiData: DataCollectionEntryPointData) =
    when (cardUiData) {
      // LOI tasks are filtered out of the tasks list for pre-defined tasks.
      is SelectedLoiSheetData -> cardUiData.loi.job.tasks.values.count { !it.isAddLoiTask } > 0
      is AdHocDataCollectionButtonData -> cardUiData.job.tasks.values.isNotEmpty()
    }

  private fun showDataSharingTermsDialog(
    cardUiData: DataCollectionEntryPointData,
    dataSharingTerms: DataSharingTerms,
  ) {
    renderComposableDialog {
      DataSharingTermsDialog(dataSharingTerms) {
        mapContainerViewModel.grantDataSharingConsent()
        navigateToDataCollectionFragment(cardUiData)
      }
    }
  }

  /** Invoked when user clicks on the map cards to collect data. */
  private fun onCollectData(cardUiData: DataCollectionEntryPointData) {
    if (!cardUiData.canCollectData) {
      // Skip data collection screen if the user can't submit any data
      // TODO: Revisit UX for displaying view only mode
      // Issue URL: https://github.com/google/ground-android/issues/1667
      ephemeralPopups.ErrorPopup().show(getString(R.string.collect_data_viewer_error))
      return
    }
    if (!hasValidTasks(cardUiData)) {
      // NOTE(#2539): The DataCollectionFragment will crash if there are no tasks.
      ephemeralPopups.ErrorPopup().show(getString(R.string.no_tasks_error))
      return
    }

    mapContainerViewModel
      .getDataSharingTerms()
      .onSuccess { terms ->
        if (terms == null) {
          // Data sharing terms already accepted or missing.
          navigateToDataCollectionFragment(cardUiData)
        } else {
          showDataSharingTermsDialog(cardUiData, terms)
        }
      }
      .onFailure {
        Timber.e(it, "Failed to get data sharing terms")
        ephemeralPopups
          .ErrorPopup()
          .show(
            if (it is GetDataSharingTermsUseCase.InvalidCustomSharingTermsException) {
              R.string.invalid_data_sharing_terms
            } else {
              R.string.something_went_wrong
            }
          )
      }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = BasemapLayoutBinding.inflate(inflater, container, false)
    binding.fragment = this
    binding.viewModel = mapContainerViewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val menuBinding = setupMenuFab()
    val onOpen = {
      binding.mapTypeBtn.hide()
      binding.locationLockBtn.hide()
      menuBinding.hamburgerBtn.hide()
    }
    val onDismiss = {
      binding.mapTypeBtn.show()
      binding.locationLockBtn.show()
      menuBinding.hamburgerBtn.show()
    }
    binding.bottomContainer.addView(
      createComposeView {
        jobMapComposables.Render(onOpen, onDismiss, onCollectData = { onCollectData(it) })
      }
    )
    binding.bottomContainer.bringToFront()
    showDataCollectionHint()

    // LOIs associated with the survey have been synced to the local db by this point. We can
    // enable location lock if no LOIs exist or a previous camera position doesn't exist.
    launchWhenStarted { mapContainerViewModel.maybeEnableLocationLock() }
  }

  /**
   * Displays a popup hint informing users how to begin collecting data, based on the properties of
   * the active survey and zoomed in state.
   *
   * This method should only be called after view creation and should only trigger once per view
   * create.
   */
  private fun showDataCollectionHint() {
    if (!this::mapContainerViewModel.isInitialized) {
      return Timber.w("showDataCollectionHint() called before mapContainerViewModel initialized")
    }
    if (!this::binding.isInitialized) {
      return Timber.w("showDataCollectionHint() called before binding initialized")
    }
    // Combine the survey properties and the current zoomed-out state to determine which popup to
    // show, or not.
    mapContainerViewModel.surveyUpdateFlow
      .combine(mapContainerViewModel.isZoomedInFlow) { surveyProperties, isZoomedIn ->
        // Negated since we only want to show certain popups when the user is zoomed out.
        Pair(surveyProperties, !isZoomedIn)
      }
      .launchWhenStartedAndCollectFirst {
        val (surveyProperties, isZoomedOut) = it
        when {
          surveyProperties.noLois && !surveyProperties.addLoiPermitted ->
            R.string.read_only_data_collection_hint
          isZoomedOut && surveyProperties.addLoiPermitted -> R.string.suggest_data_collection_hint
          isZoomedOut -> R.string.predefined_data_collection_hint
          else -> null
        }?.let { message ->
          infoPopup =
            ephemeralPopups.InfoPopup(
              binding.bottomContainer,
              message,
              EphemeralPopups.PopupDuration.LONG,
            )
          infoPopup.show()
        }
      }
  }

  private fun setupMenuFab(): MenuButtonBinding {
    val mapOverlay = binding.overlay
    val menuBinding = MenuButtonBinding.inflate(layoutInflater, mapOverlay, true)
    menuBinding.homeScreenViewModel = homeScreenViewModel
    menuBinding.lifecycleOwner = this
    return menuBinding
  }

  private fun navigateToDataCollectionFragment(cardUiData: DataCollectionEntryPointData) {
    when (cardUiData) {
      is SelectedLoiSheetData ->
        findNavController()
          .navigate(
            HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
              cardUiData.loi.id,
              cardUiData.loi.properties[LOI_NAME_PROPERTY] as? String?,
              cardUiData.loi.job.id,
              false,
              null,
              "",
            )
          )
      is AdHocDataCollectionButtonData ->
        findNavController()
          .navigate(
            HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
              null,
              null,
              cardUiData.job.id,
              false,
              null,
              "",
            )
          )
    }
  }

  override fun onMapReady(map: MapFragment) {
    mapContainerViewModel.mapLoiFeatures.launchWhenStartedAndCollect { map.setFeatures(it) }
  }

  override fun getMapViewModel(): BaseMapViewModel = mapContainerViewModel
}
