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
import co.cask.cdap.proto.id.DatasetId;
import co.cask.cdap.proto.id.NamespaceId;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link ParentIndexer}
 */
public class ParentIndexerTest {

  @Test
  public void testGetIndexes() {
    ParentIndexer parentIndexer = new ParentIndexer();
    DatasetId datasetId = NamespaceId.DEFAULT.dataset("someDs");
    MetadataEntity dsEntity = datasetId.toMetadataEntity();
    MetadataEntity fieldEntity = MetadataEntity.builder(dsEntity).appendAsType("field", "myField").build();
    Assert.assertTrue(parentIndexer.getIndexes(new MetadataEntry(dsEntity, "k", "v"))
                        .containsAll(ImmutableSet.of(ParentIndexer.PARENT_KEY +
                                                       MetadataDataset.KEYVALUE_SEPARATOR + datasetId)));
    Assert.assertTrue(parentIndexer.getIndexes(new MetadataEntry(fieldEntity, "k", "v"))
                        .containsAll(ImmutableSet.of(ParentIndexer.PARENT_KEY +
                                                       MetadataDataset.KEYVALUE_SEPARATOR + datasetId)));

  }
}
