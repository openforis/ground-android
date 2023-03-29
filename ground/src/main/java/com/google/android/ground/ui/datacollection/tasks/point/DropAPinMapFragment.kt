/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.point

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.databinding.BasemapLayoutBinding
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapFragment

class DropAPinMapFragment(
  private val viewModel: DropAPinTaskViewModel,
  private val mapViewModel: BaseMapViewModel
) : AbstractMapContainerFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding = BasemapLayoutBinding.inflate(inflater, container, false)
    binding.fragment = this
    binding.viewModel = mapViewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onMapReady(mapFragment: MapFragment) {
    viewModel.features.observe(this) { mapFragment.renderFeatures(it) }
  }

  override fun getMapViewModel(): BaseMapViewModel = mapViewModel

  override fun onMapCameraMoved(position: CameraPosition) {
    super.onMapCameraMoved(position)
    viewModel.updateResponse(position)
  }

  companion object {
    fun newInstance(
      viewModel: DropAPinTaskViewModel,
      mapViewModel: BaseMapViewModel,
      mapFragment: MapFragment
    ) = DropAPinMapFragment(viewModel, mapViewModel).apply { this.mapFragment = mapFragment }
  }
}