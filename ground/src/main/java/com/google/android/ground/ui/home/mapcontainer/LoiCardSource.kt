/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.home.mapcontainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.map.CameraPosition
import com.google.common.collect.ImmutableSet
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlin.streams.toList
import timber.log.Timber

/** Provides data for displaying cards for visible LOIs at the bottom of the screen. */
class LoiCardSource
@Inject
internal constructor(
  private val surveyRepository: SurveyRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository,
) {

  private val cameraPositionSubject: @Hot Subject<CameraPosition> = PublishSubject.create()
  val locationsOfInterest: LiveData<List<LocationOfInterest>>

  init {
    locationsOfInterest =
      LiveDataReactiveStreams.fromPublisher(
        getCameraBoundUpdates()
          .flatMap { bounds ->
            getAllLocationsOfInterest().map { lois ->
              lois.stream().filter { isGeometryWithinBounds(it.geometry, bounds) }.toList()
            }
          }
          .distinctUntilChanged()
      )
  }

  fun updateCameraPosition(cameraPosition: CameraPosition) {
    cameraPositionSubject.onNext(cameraPosition)
  }

  /** Returns a flowable of [LatLngBounds] whenever camera moves. */
  private fun getCameraBoundUpdates(): Flowable<LatLngBounds> =
    cameraPositionSubject
      .filter { it.bounds != null }
      .map { it.bounds!! }
      .toFlowable(BackpressureStrategy.LATEST)
      .distinctUntilChanged()

  /** Returns a flowable of all [LocationOfInterest] for the selected [Survey]. */
  private fun getAllLocationsOfInterest(): Flowable<ImmutableSet<LocationOfInterest>> =
    surveyRepository.activeSurvey
      .switchMap { survey ->
        survey
          .map { locationOfInterestRepository.getLocationsOfInterestOnceAndStream(it) }
          .orElse(Flowable.just(ImmutableSet.of()))
      }
      .distinctUntilChanged()

  /** Returns true if the provided [geometry] is within [bounds]. */
  private fun isGeometryWithinBounds(geometry: Geometry, bounds: LatLngBounds): Boolean {
    return when (geometry) {
      is Point -> bounds.contains(geometry.coordinate.toLatLng())
      else -> {
        Timber.d("Implement LatLng comparator for $geometry")
        true
      }
    }
  }
}
