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
package org.groundplatform.android.persistence.local.stores

import kotlinx.coroutines.flow.Flow
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.persistence.local.room.entity.LocationOfInterestMutationEntity
import org.groundplatform.android.persistence.local.room.fields.MutationEntitySyncStatus

interface LocalLocationOfInterestStore :
  LocalMutationStore<LocationOfInterestMutation, LocationOfInterest> {

  /**
   * Returns the count of [LocationOfInterest] in the local db associated with the given [Survey].
   */
  suspend fun getLoiCount(surveyId: String): Int

  /**
   * Retrieves the complete set of [LocationOfInterest] associated with the given [Survey] from the
   * local database and returns a [Flow] that continually emits the complete set anew any time the
   * underlying table changes (insertions, deletions, updates).
   */
  fun getValidLois(survey: Survey): Flow<Set<LocationOfInterest>>

  /** Returns the [LocationOfInterest] with the specified UUID from the local data store. */
  suspend fun getLocationOfInterest(
    survey: Survey,
    locationOfInterestId: String,
  ): LocationOfInterest?

  /** Deletes LOI from local database. */
  suspend fun deleteLocationOfInterest(locationOfInterestId: String)

  /**
   * Returns a [Flow] that emits a [List] of all [LocationOfInterestMutation]s stored in the local
   * db related to a given [Survey]. A new [List] is emitted on each change to the underlying saved
   * data.
   */
  fun getAllSurveyMutations(survey: Survey): Flow<List<LocationOfInterestMutation>>

  fun getAllMutationsFlow(): Flow<List<LocationOfInterestMutation>>

  suspend fun findByLocationOfInterestId(
    id: String,
    vararg states: MutationEntitySyncStatus,
  ): List<LocationOfInterestMutationEntity>

  suspend fun insertOrUpdate(loi: LocationOfInterest)

  suspend fun deleteNotIn(surveyId: String, ids: List<String>)
}
