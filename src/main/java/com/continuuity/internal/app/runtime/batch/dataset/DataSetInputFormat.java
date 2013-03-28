package com.continuuity.internal.app.runtime.batch.dataset;

import com.continuuity.api.data.DataSet;
import com.continuuity.api.data.DataSetSpecification;
import com.continuuity.api.data.batch.BatchReadable;
import com.continuuity.api.data.batch.Split;
import com.continuuity.api.data.batch.SplitReader;
import com.continuuity.internal.app.runtime.batch.BasicMapReduceContext;
import com.continuuity.internal.app.runtime.batch.inmemory.MapReduceContextAccessor;
import com.google.gson.Gson;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataSetInputFormat extends InputFormat<Object, Object> {
  public static final String INPUT_DATASET_SPEC = "input.dataset.spec";

  public static void setInput(Job job, DataSet dataSet) {
    job.setInputFormatClass(DataSetInputFormat.class);
    job.getConfiguration().set(DataSetInputFormat.INPUT_DATASET_SPEC, new Gson().toJson(dataSet.configure()));
  }

  @Override
  public List<InputSplit> getSplits(final JobContext context) throws IOException, InterruptedException {
    Configuration conf = context.getConfiguration();
    List<Split> splits = MapReduceContextAccessor.getContext(conf).getInputDataSelection();

    List<InputSplit> list = new ArrayList<InputSplit>();
    for (Split split : splits) {
      list.add(new DataSetInputSplit(split));
    }

    return list;
  }

  @Override
  public RecordReader<Object, Object> createRecordReader(final InputSplit split,
                                                                final TaskAttemptContext context)
    throws IOException, InterruptedException {

    DataSetInputSplit inputSplit = (DataSetInputSplit) split;

    Configuration conf = context.getConfiguration();
    BasicMapReduceContext mrContext = MapReduceContextAccessor.getContext(conf);
    BatchReadable dataset = (BatchReadable) mrContext.getDataSet(getInputDataSetSpec(conf).getName());
    SplitReader splitReader = dataset.createSplitReader(inputSplit.getSplit());

    return new DataSetRecordReader(dataset, splitReader, mrContext);
  }

  private DataSetSpecification getInputDataSetSpec(Configuration conf) {
    return new Gson().fromJson(conf.get(INPUT_DATASET_SPEC), DataSetSpecification.class);
  }
}
