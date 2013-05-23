package com.continuuity.internal.app.runtime.batch.inmemory;

import com.continuuity.app.guice.BigMamaModule;
import com.continuuity.common.conf.CConfiguration;
import com.continuuity.common.conf.Constants;
import com.continuuity.data.runtime.DataFabricLevelDBModule;
import com.continuuity.data.runtime.DataFabricModules;
import com.continuuity.internal.app.runtime.batch.AbstractMapReduceContextBuilder;
import com.continuuity.runtime.MetadataModules;
import com.continuuity.runtime.MetricsModules;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.hadoop.conf.Configuration;

/**
 * Builds an instance of {@link com.continuuity.internal.app.runtime.batch.BasicMapReduceContext} good for
 * in-memory environment
 */
public class InMemoryMapReduceContextBuilder extends AbstractMapReduceContextBuilder {
  private final CConfiguration cConf;

  public InMemoryMapReduceContextBuilder(CConfiguration cConf) {
    this.cConf = cConf;
  }

  protected Injector createInjector() {
    // TODO: this logic should go into DataFabricModules. We'll move it once Guice modules are refactored
    Constants.InMemoryPersistenceType persistenceType = Constants.InMemoryPersistenceType.valueOf(
      cConf.get(Constants.CFG_DATA_INMEMORY_PERSISTENCE, Constants.DEFAULT_DATA_INMEMORY_PERSISTENCE));

    if (Constants.InMemoryPersistenceType.MEMORY == persistenceType) {
      return createInMemoryModules();
    } else {
      return createPersistentModules(persistenceType);
    }
  }

  private Injector createInMemoryModules() {
    ImmutableList<Module> inMemoryModules = ImmutableList.of(
      new BigMamaModule(cConf),
      new MetricsModules().getInMemoryModules(),
      new DataFabricModules().getInMemoryModules(),
      new MetadataModules().getInMemoryModules()
    );

    return Guice.createInjector(inMemoryModules);
  }

  private Injector createPersistentModules(Constants.InMemoryPersistenceType persistenceType) {
    ImmutableList<Module> singleNodeModules = ImmutableList.of(
      new BigMamaModule(cConf),
      new MetricsModules().getSingleNodeModules(),
      Constants.InMemoryPersistenceType.LEVELDB == persistenceType ?
        new DataFabricLevelDBModule(cConf) : new DataFabricModules().getSingleNodeModules(),
      new MetadataModules().getSingleNodeModules()
    );
    return Guice.createInjector(singleNodeModules);
  }
}
