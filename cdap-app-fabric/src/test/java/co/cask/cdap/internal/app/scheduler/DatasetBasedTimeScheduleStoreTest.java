/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.cdap.internal.app.scheduler;

import co.cask.cdap.api.data.schema.UnsupportedTypeException;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.guice.ConfigModule;
import co.cask.cdap.common.guice.DiscoveryRuntimeModule;
import co.cask.cdap.common.guice.LocationRuntimeModule;
import co.cask.cdap.common.namespace.guice.NamespaceClientRuntimeModule;
import co.cask.cdap.data.runtime.DataFabricModules;
import co.cask.cdap.data.runtime.DataSetServiceModules;
import co.cask.cdap.data.runtime.DataSetsModules;
import co.cask.cdap.data2.datafabric.dataset.service.DatasetService;
import co.cask.cdap.data2.datafabric.dataset.service.executor.DatasetOpExecutor;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.explore.guice.ExploreClientModule;
import co.cask.cdap.internal.TempFolder;
import co.cask.cdap.internal.app.runtime.schedule.store.DatasetBasedTimeScheduleStore;
import co.cask.cdap.internal.app.runtime.schedule.store.ScheduleStoreTableUtil;
import co.cask.cdap.metrics.guice.MetricsClientRuntimeModule;
import co.cask.cdap.test.SlowTests;
import co.cask.tephra.TransactionExecutorFactory;
import co.cask.tephra.TransactionManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link DatasetBasedTimeScheduleStore} across scheduler restarts to verify we retain scheduler information
 * across restarts.
 */
@Category(SlowTests.class)
public class DatasetBasedTimeScheduleStoreTest {

  private static final TempFolder TEMP_FOLDER = new TempFolder();

  private static Injector injector;
  private static Scheduler scheduler;
  private static TransactionExecutorFactory factory;
  private static DatasetFramework dsFramework;
  private static TransactionManager txService;
  private static DatasetOpExecutor dsOpsService;
  private static DatasetService dsService;
  private static final String DUMMY_SCHEDULER_NAME = "dummyScheduler";

  @BeforeClass
  public static void beforeClass() throws Exception {
    CConfiguration conf = CConfiguration.create();
    conf.set(Constants.CFG_LOCAL_DATA_DIR, TEMP_FOLDER.newFolder("data").getAbsolutePath());
    injector = Guice.createInjector(new ConfigModule(conf),
                                    new LocationRuntimeModule().getInMemoryModules(),
                                    new DiscoveryRuntimeModule().getInMemoryModules(),
                                    new MetricsClientRuntimeModule().getInMemoryModules(),
                                    new DataFabricModules().getInMemoryModules(),
                                    new DataSetsModules().getStandaloneModules(),
                                    new DataSetServiceModules().getInMemoryModules(),
                                    new ExploreClientModule(),
                                    new NamespaceClientRuntimeModule().getInMemoryModules());
    txService = injector.getInstance(TransactionManager.class);
    txService.startAndWait();
    dsOpsService = injector.getInstance(DatasetOpExecutor.class);
    dsOpsService.startAndWait();
    dsService = injector.getInstance(DatasetService.class);
    dsService.startAndWait();
    dsFramework = injector.getInstance(DatasetFramework.class);
    factory = injector.getInstance(TransactionExecutorFactory.class);
  }

  @AfterClass
  public static void afterClass() {
    dsService.stopAndWait();
    dsOpsService.stopAndWait();
    txService.stopAndWait();
  }

  public static void schedulerSetup(boolean enablePersistence) throws SchedulerException {
    JobStore js;
    if (enablePersistence) {
      CConfiguration conf = injector.getInstance(CConfiguration.class);
      js = new DatasetBasedTimeScheduleStore(factory, new ScheduleStoreTableUtil(dsFramework, conf));
    } else {
      js = new RAMJobStore();
    }

    SimpleThreadPool threadPool = new SimpleThreadPool(10, Thread.NORM_PRIORITY);
    threadPool.initialize();
    DirectSchedulerFactory.getInstance().createScheduler(DUMMY_SCHEDULER_NAME, "1", threadPool, js);

    scheduler = DirectSchedulerFactory.getInstance().getScheduler(DUMMY_SCHEDULER_NAME);
    scheduler.start();
  }

  public static void schedulerTearDown() throws SchedulerException {
    scheduler.shutdown();
  }

  @Test
  public void testJobProperties() throws SchedulerException, UnsupportedTypeException, InterruptedException {
    schedulerSetup(true);
    JobDetail jobDetail = getJobDetail();

    Trigger trigger = TriggerBuilder.newTrigger()
      .withIdentity("g2")
      .usingJobData(LogPrintingJob.KEY, LogPrintingJob.VALUE)
      .startNow()
      .withSchedule(CronScheduleBuilder.cronSchedule("0/1 * * * * ?"))
      .build();

    //Schedule job
    scheduler.scheduleJob(jobDetail, trigger);
    //Make sure that the job gets triggered more than once.
    TimeUnit.SECONDS.sleep(3);
    scheduler.deleteJob(jobDetail.getKey());
    schedulerTearDown();
  }

  @Test
  public void testSchedulerWithoutPersistence() throws SchedulerException, UnsupportedTypeException {
    JobKey jobKey = scheduleJobWithTrigger(false);

    verifyJobAndTriggers(jobKey, 1, Trigger.TriggerState.NORMAL);

    //Shutdown scheduler.
    schedulerTearDown();
    //restart scheduler.
    schedulerSetup(false);

    //read the job
    JobDetail jobStored = scheduler.getJobDetail(jobKey);
    // The job with old job key should not exist since it is not persisted.
    Assert.assertNull(jobStored);
    schedulerTearDown();
  }

  @Test
  public void testSchedulerWithPersistenceAcrossRestarts() throws SchedulerException, UnsupportedTypeException {
    JobKey jobKey = scheduleJobWithTrigger(true);

    verifyJobAndTriggers(jobKey, 1, Trigger.TriggerState.NORMAL);

    //Shutdown scheduler.
    schedulerTearDown();
    //restart scheduler.
    schedulerSetup(true);

    // The job with old job key should exist since it is persisted.
    verifyJobAndTriggers(jobKey, 1, Trigger.TriggerState.NORMAL);
    schedulerTearDown();
  }

  @Test
  public void testPausedTriggersAcrossRestarts() throws SchedulerException, UnsupportedTypeException {
    JobKey jobKey = scheduleJobWithTrigger(true);

    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

    // pause triggers for the job
    for (Trigger trigger : triggers) {
      scheduler.pauseTrigger(trigger.getKey());
    }

    //Shutdown scheduler.
    schedulerTearDown();
    //restart scheduler.
    schedulerSetup(true);

    verifyJobAndTriggers(jobKey, 1, Trigger.TriggerState.PAUSED);

    // remove job and check if the associated trigger gets removed too
    Assert.assertTrue("Failed to delete the job", scheduler.deleteJob(jobKey));
    Assert.assertFalse("Trigger for the deleted job still exists", scheduler.checkExists(triggers.get(0).getKey()));
    // check for trigger to not exist in the datastore too from which scheduler will get initialized across restart
    //Shutdown scheduler.
    schedulerTearDown();
    //restart scheduler.
    schedulerSetup(true);
    Assert.assertFalse("Trigger for the deleted job still exists", scheduler.checkExists(triggers.get(0).getKey()));
    schedulerTearDown();
  }

  private void verifyJobAndTriggers(JobKey jobKey, int expectedTriggersSize,
                                    Trigger.TriggerState expectedTriggerState) throws SchedulerException {
    JobDetail jobStored = scheduler.getJobDetail(jobKey);
    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
    Assert.assertEquals(jobStored.getKey().getName(), jobKey.getName());
    Assert.assertEquals(expectedTriggersSize, triggers.size());
    verifyTriggerState(triggers, expectedTriggerState);
  }

  private void verifyTriggerState(List<? extends Trigger> triggers,
                                  Trigger.TriggerState expectedTriggerState) throws SchedulerException {
    for (Trigger trigger : triggers) {
      Assert.assertEquals(expectedTriggerState, scheduler.getTriggerState(trigger.getKey()));
    }
  }

  private JobKey scheduleJobWithTrigger(boolean enablePersistence) throws UnsupportedTypeException, SchedulerException {
    //start scheduler with given persistence setting
    schedulerSetup(enablePersistence);
    JobDetail jobDetail = getJobDetail();

    Trigger trigger = TriggerBuilder.newTrigger()
      .withIdentity("p1")
      .startNow()
      .withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?"))
      .build();

    //Schedule job
    scheduler.scheduleJob(jobDetail, trigger);
    return jobDetail.getKey();
  }

  private JobDetail getJobDetail() {
    return JobBuilder.newJob(LogPrintingJob.class)
      .withIdentity("developer:application1:mapreduce1")
      .build();
  }

  @AfterClass
  public static void cleanup() throws SchedulerException, InterruptedException {
    schedulerTearDown();
    Thread.sleep(10000);
  }
}
