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
package com.cloudera.exhibit.sql;

import com.cloudera.exhibit.avro.AvroFrame;
import com.cloudera.exhibit.avro.AvroObsDescriptor;
import com.cloudera.exhibit.core.Exhibit;
import com.cloudera.exhibit.core.Frame;
import com.cloudera.exhibit.core.Obs;
import com.cloudera.exhibit.core.Vec;
import com.cloudera.exhibit.core.simple.SimpleExhibit;
import com.cloudera.exhibit.core.vector.Vector;
import com.cloudera.exhibit.core.vector.VectorBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AvroTableTest {

  Schema schema = SchemaBuilder.record("foo").fields()
      .nullableString("f1", "")
      .optionalBoolean("f2")
      .requiredLong("f3")
      .endRecord();

  private Frame eval(SQLCalculator calc, Exhibit e) {
    calc.initialize(e.descriptor());
    Frame frm = calc.apply(e);
    calc.cleanup();
    return frm;
  }

  @Test
  public void testEmptyFrame() throws Exception {
    AvroObsDescriptor at = new AvroObsDescriptor(schema);
    String[] queries = new String[] {
        "select f2, sum(f3) as sumf3 from t1 where f1 = 'foo' group by f2"
    };
    SQLCalculator calc = new SQLCalculator(queries);
    Frame frame = eval(calc, SimpleExhibit.of("t1", new AvroFrame(at)));
    assertFalse(frame.size() > 0);
  }

  @Test
  public void testEmptyVector() throws Exception {
    String[] queries = new String[] {
      "select count(*) as ct from v1"
    };
    SQLCalculator calc = new SQLCalculator(queries);
    Vector v = VectorBuilder.doubles(Collections.emptyList());
    Frame frame = eval(calc, SimpleExhibit.of("v1", v));
    assertEquals("Single Record returned", 1, frame.size());
    assertEquals("Zero Count", 0L, frame.get(0).get("ct"));
  }

  @Test
  public void testBasic() throws Exception {
    GenericData.Record r1 = new GenericData.Record(schema);
    r1.put("f1", "foo");
    r1.put("f2", true);
    r1.put("f3", 1729L);
    GenericData.Record r2 = new GenericData.Record(schema);
    r2.put("f1", "for");
    r2.put("f2", true);
    r2.put("f3", 17L);
    AvroFrame frame = new AvroFrame(ImmutableList.of(r1, r2));
    StringBuilder inb = new StringBuilder("1729");
    for (int i = 0; i < 19; i++) {
      inb.append(',').append(1730 + i);
    }
    String in = inb.toString();
    String[] queries = new String[] {
        "select f2, sum(f3) as sumf3 from t1 where f1 = 'foo' and f3 in ("+in+") group by f2"
    };
    SQLCalculator calc = new SQLCalculator(queries);
    Exhibit e = SimpleExhibit.of("t1", frame);
    calc.initialize(e.descriptor());
    long start = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) {
      Frame res = calc.apply(e);
      assertEquals(1, res.size());
      assertEquals(Boolean.TRUE, res.get(0).get(0));
      assertEquals(1729L, res.get(0).get(1));
    }
    System.out.println("Runtime: " + (System.currentTimeMillis() - start));
  }

  @Test
  public void testFrameVectorJoin() throws Exception {
    GenericData.Record r1 = new GenericData.Record(schema);
    r1.put("f1", "foo");
    r1.put("f2", true);
    r1.put("f3", 1729L);
    GenericData.Record r2 = new GenericData.Record(schema);
    r2.put("f1", "for");
    r2.put("f2", true);
    r2.put("f3", 17L);
    AvroFrame frame = new AvroFrame(ImmutableList.of(r1, r2));
    Vec v = VectorBuilder.doubles(ImmutableList.<Object>of(1729.0));
    SimpleExhibit se = new SimpleExhibit(Obs.EMPTY,
        ImmutableMap.<String, Frame>of("t1", frame), ImmutableMap.of("v1", v));
    String[] queries = new String[] {
        "select * from t1, v1 where t1.f3=v1.c0"
    };
    SQLCalculator calc = new SQLCalculator(queries);
    Frame res = eval(calc, se);
    assertEquals(1, res.size());
    assertEquals(Boolean.TRUE, res.get(0).get("f2"));
    assertEquals(1729L, res.get(0).get("f3"));
    assertEquals(1729.0, res.get(0).get("c0"));
  }

  @Test
  public void testMissingFields() throws Exception {
    GenericData.Record r1 = new GenericData.Record(schema);
    r1.put("f1", "foo");
    r1.put("f3", 1729L);
    GenericData.Record r2 = new GenericData.Record(schema);
    r2.put("f2", true);
    r2.put("f3", 17L);
    AvroFrame frame = new AvroFrame(ImmutableList.of(r1, r2));
    String[] queries = new String[] {
        "select f2, sum(f3) as sumf3 from t1 where f1 = 'foo' group by f2"
    };
    SQLCalculator calc = new SQLCalculator(queries);
    Frame res = eval(calc, SimpleExhibit.of("t1", frame));
    assertTrue(res.size() == 1);
    assertEquals(null, res.get(0).get(0));
    assertEquals(1729L, res.get(0).get(1));
  }
}
