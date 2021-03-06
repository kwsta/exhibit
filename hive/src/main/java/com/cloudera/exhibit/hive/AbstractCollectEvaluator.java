/*
 * Copyright (c) 2015, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.exhibit.hive;

import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.serde2.lazy.LazyArray;
import org.apache.hadoop.hive.serde2.lazybinary.LazyBinaryArray;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public abstract class AbstractCollectEvaluator extends GenericUDAFEvaluator {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractCollectEvaluator.class);

  private ObjectInspector originalOI;
  private ListObjectInspector outputOI;
  private ListObjectInspector mergeOI;

  public static class Sets extends AbstractCollectEvaluator {
    @Override
    protected Collection newCollection() {
      return new HashSet();
    }
  }

  public static class Lists extends AbstractCollectEvaluator {
    @Override
    protected Collection newCollection() {
      return new ArrayList();
    }
  }

  public static class CollectionBuffer implements AggregationBuffer {
    Collection items;

    public CollectionBuffer(Collection items) {
      this.items = items;
    }

    public void add(Object o, ObjectInspector oi) {
      if (o != null) {
        this.items.add(ObjectInspectorUtils.copyToStandardObject(o, oi));
      }
    }
  }

  protected abstract Collection newCollection();

  @Override
  public ObjectInspector init(Mode mode, ObjectInspector[] args) throws HiveException {
    super.init(mode, args);
    LOG.info("Running CollectEvaluator in mode = " + mode);
    LOG.info("ObjectInspector Type = " + args[0].getTypeName());
    if (mode == Mode.PARTIAL1 || !(args[0] instanceof ListObjectInspector)) {
      this.originalOI = args[0];
      LOG.info("Running CollectEvaluator for PARTIAL1/COMPLETE");
      return ObjectInspectorFactory.getStandardListObjectInspector(
          ObjectInspectorUtils.getStandardObjectInspector(originalOI));
    } else {
      LOG.info("Running CollectEvaluator for PARTIAL2/FINAL");
      this.mergeOI = (ListObjectInspector) args[0];
      this.originalOI = mergeOI.getListElementObjectInspector();
      this.outputOI = (ListObjectInspector) ObjectInspectorUtils.getStandardObjectInspector(mergeOI);
      return outputOI;
    }
  }

  @Override
  public AggregationBuffer getNewAggregationBuffer() throws HiveException {
    return new CollectionBuffer(newCollection());
  }

  @Override
  public void reset(AggregationBuffer buf) throws HiveException {
    ((CollectionBuffer) buf).items = newCollection();
  }

  @Override
  public void iterate(AggregationBuffer buf, Object[] args) throws HiveException {
    Object arg = args[0];
    if (arg == null) {
      return;
    }
    if (arg instanceof LazyBinaryArray) {
      LazyBinaryArray lba = (LazyBinaryArray) arg;
      for (int i = 0; i < lba.getListLength(); i++) {
        ((CollectionBuffer) buf).add(lba.getListElementObject(i), originalOI);
      }
    } else {
      ((CollectionBuffer) buf).add(arg, originalOI);
    }
  }

  @Override
  public Object terminatePartial(AggregationBuffer buf) throws HiveException {
    return new ArrayList(((CollectionBuffer) buf).items);
  }

  @Override
  public void merge(AggregationBuffer buf, Object arg) throws HiveException {
    CollectionBuffer cb = (CollectionBuffer) buf;
    List partial = mergeOI.getList(arg);
    for (Object o : partial) {
      cb.add(o, mergeOI.getListElementObjectInspector());
    }
  }

  @Override
  public Object terminate(AggregationBuffer buf) throws HiveException {
    return new ArrayList(((CollectionBuffer) buf).items);
  }
}
