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

import com.cloudera.exhibit.core.Vec;
import org.apache.calcite.linq4j.Enumerator;

public class VectorEnumerator implements Enumerator<Object> {

  private final Vec vector;
  private Object current;
  private int currentIndex;

  public VectorEnumerator(Vec vector) {
    this.vector = vector;
    this.current = null;
    this.currentIndex = -1;
  }

  @Override
  public Object current() {
    return current;
  }

  @Override
  public boolean moveNext() {
    currentIndex++;
    boolean hasNext = currentIndex < vector.size();
    if (hasNext) {
      this.current = vector.get(currentIndex);
    }
    return hasNext;
  }

  @Override
  public void reset() {
    currentIndex = -1;
    current = null;
  }

  @Override
  public void close() {
    // No-op
  }
}
