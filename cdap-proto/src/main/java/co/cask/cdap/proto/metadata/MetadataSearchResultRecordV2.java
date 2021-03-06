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
package co.cask.cdap.proto.metadata;

import co.cask.cdap.api.annotation.Beta;
import co.cask.cdap.api.metadata.Metadata;
import co.cask.cdap.api.metadata.MetadataEntity;
import co.cask.cdap.api.metadata.MetadataScope;
import co.cask.cdap.proto.id.EntityId;
import co.cask.cdap.proto.id.NamespacedEntityId;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represent the Metadata search result record.
 */
@Beta
public class MetadataSearchResultRecordV2 {
  private final MetadataEntity metadataEntity;
  private final Map<MetadataScope, Metadata> metadata;

  public MetadataSearchResultRecordV2(NamespacedEntityId entityId) {
    this(entityId.toMetadataEntity());
  }

  public MetadataSearchResultRecordV2(MetadataEntity metadataEntity) {
    this(metadataEntity, Collections.emptyMap());
  }

  public MetadataSearchResultRecordV2(NamespacedEntityId entityId, Map<MetadataScope, Metadata> metadata) {
    this(entityId.toMetadataEntity(), metadata);
  }

  public MetadataSearchResultRecordV2(MetadataEntity metadataEntity, Map<MetadataScope, Metadata> metadata) {
    this.metadataEntity = metadataEntity;
    this.metadata = new HashMap<>(metadata);
  }

  public NamespacedEntityId getEntityId() {
    return EntityId.fromMetadataEntity(metadataEntity);
  }

  public MetadataEntity getMetadataEntity() {
    return metadataEntity;
  }

  public Map<MetadataScope, Metadata> getMetadata() {
    return metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MetadataSearchResultRecordV2)) {
      return false;
    }
    MetadataSearchResultRecordV2 that = (MetadataSearchResultRecordV2) o;
    return Objects.equals(metadataEntity, that.metadataEntity) &&
      Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadataEntity, metadata);
  }

  @Override
  public String toString() {
    return "MetadataSearchResultRecordV2{" +
      "metadataEntity=" + metadataEntity +
      ", metadata=" + metadata +
      '}';
  }
}
