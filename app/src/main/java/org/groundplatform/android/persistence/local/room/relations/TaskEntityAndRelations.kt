/*
 * Copyright 2019 Google LLC
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
package org.groundplatform.android.persistence.local.room.relations

import androidx.room.Embedded
import androidx.room.Relation
import org.groundplatform.android.persistence.local.room.entity.ConditionEntity
import org.groundplatform.android.persistence.local.room.entity.MultipleChoiceEntity
import org.groundplatform.android.persistence.local.room.entity.OptionEntity
import org.groundplatform.android.persistence.local.room.entity.TaskEntity

/**
 * Represents relationship between TaskEntity, MultipleChoiceEntity, OptionEntity, and
 * ConditionEntity.
 *
 * Querying any of the below data classes automatically loads the task annotated as @Relation.
 */
data class TaskEntityAndRelations(
  @Embedded val taskEntity: TaskEntity,
  @Relation(parentColumn = "id", entityColumn = "task_id")
  val multipleChoiceEntities: List<MultipleChoiceEntity>,
  @Relation(parentColumn = "id", entityColumn = "task_id", entity = OptionEntity::class)
  val optionEntities: List<OptionEntity>,
  @Relation(parentColumn = "id", entityColumn = "parent_task_id", entity = ConditionEntity::class)
  val conditionEntityAndRelations: List<ConditionEntityAndRelations>,
)
