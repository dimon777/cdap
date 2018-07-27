/*
 * Copyright © 2018 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.data2.metadata.indexer;

import co.cask.cdap.api.metadata.MetadataEntity;
import co.cask.cdap.data2.metadata.dataset.MetadataDataset;
import co.cask.cdap.data2.metadata.dataset.MetadataEntry;
import co.cask.cdap.data2.metadata.dataset.SortInfo;
import co.cask.cdap.proto.element.EntityType;
import co.cask.cdap.proto.id.EntityId;

import java.util.HashSet;
import java.util.Set;

/**
 * Indexer used to index a custom {@link MetadataEntity} with it's nearest known ancestor.
 * A nearest known ancestor is defined as the nearest known {@link EntityType} in the hierarchy which is determined by
 * {@link EntityId#getNearestKnownEntity(MetadataEntity)}. If the given {@link MetadataEntity} if not a custom
 * entity and is a know CDAP entity then no indexes are generated but this indexer.
 */
public class NearestKnownAncestorIndexer implements Indexer {

  public static final String PARENT_KEY = "nearest_known_entity";

  @Override
  public Set<String> getIndexes(MetadataEntry entry) {
    Set<String> indexes = new HashSet<>();
    EntityId selfOrParentEntityId = EntityId.getNearestKnownEntity(entry.getMetadataEntity());
    // if the entity is custom entity then only we want to generate the last known nearest custom index
    if (!selfOrParentEntityId.getEntityType().toString().equalsIgnoreCase(entry.getMetadataEntity().getType())) {
      indexes.add(PARENT_KEY + MetadataDataset.KEYVALUE_SEPARATOR + selfOrParentEntityId);
    }
    return indexes;
  }

  @Override
  public SortInfo.SortOrder getSortOrder() {
    return SortInfo.SortOrder.WEIGHTED;
  }
}
