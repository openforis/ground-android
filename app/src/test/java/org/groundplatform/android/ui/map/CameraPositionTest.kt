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
package org.groundplatform.android.ui.map

import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.FakeData
import org.junit.Test

class CameraPositionTest {

  @Test
  fun serialize_deserialize_when_only_target_is_available() {
    serializeAndDeserialize(CameraPosition(coordinates = COORDINATES))
  }

  @Test
  fun serialize_deserialize_when_target_and_zoomLevel_are_available() {
    serializeAndDeserialize(CameraPosition(coordinates = COORDINATES, zoomLevel = ZOOM_LEVEL))
  }

  @Test
  fun serialize_deserialize_when_target_and_bounds_are_available() {
    serializeAndDeserialize(CameraPosition(coordinates = COORDINATES, bounds = BOUNDS))
  }

  @Test
  fun serialize_deserialize_when_all_fields_present() {
    serializeAndDeserialize(CameraPosition(COORDINATES, ZOOM_LEVEL, BOUNDS))
  }

  private fun serializeAndDeserialize(cameraPosition: CameraPosition) {
    val serializedValue = cameraPosition.serialize()
    val deserializedValue = CameraPosition.deserialize(serializedValue)
    assertThat(deserializedValue).isEqualTo(cameraPosition)
  }

  companion object {
    private val BOUNDS = Bounds(-10.0, -20.0, 10.0, 20.0)
    private val COORDINATES = FakeData.COORDINATES
    private const val ZOOM_LEVEL = 13.0f
  }
}
