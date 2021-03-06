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
package com.cloudera.exhibit.core;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Iterator;

public abstract class ObsDescriptor extends AbstractList<ObsDescriptor.Field> implements Serializable {

  public static class Field implements Serializable {
    public final String name;
    public final FieldType type;

    public Field(String name, FieldType type) {
      this.name = Preconditions.checkNotNull(name);
      this.type = Preconditions.checkNotNull(type);
    }

    @Override
    public int hashCode() {
      return name.hashCode() + 17 * type.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (other == null || !(other instanceof Field)) {
        return false;
      }
      Field field = (Field) other;
      return name.equals(field.name) && type.equals(field.type);
    }
    @Override
    public String toString() {
      return name + ": " + type.toString().toLowerCase();
    }
  }

  public abstract int indexOf(String name);

  public static final ObsDescriptor EMPTY = new ObsDescriptor() {
    @Override
    public Field get(int i) {
      throw new ArrayIndexOutOfBoundsException("Empty ObsDescriptor");
    }

    @Override
    public int indexOf(String name) {
      return -1;
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public Iterator<Field> iterator() {
      return Collections.emptyIterator();
    }

    @Override
    public String toString() { return "<empty>"; }
  };
}
