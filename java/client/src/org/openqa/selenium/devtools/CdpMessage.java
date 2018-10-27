// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.devtools;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

class CdpMessage {

  private final static AtomicLong MESSAGE_IDS = new AtomicLong(1);

  private final long id;
  private final String methodName;
  private final Map<String, Object> parameters;

  public CdpMessage(String methodName, Map<String, Object> parameters) {
    this.methodName = Objects.requireNonNull(methodName);
    this.parameters = parameters == null ? ImmutableMap.of() : ImmutableMap.copyOf(parameters);

    this.id = MESSAGE_IDS.getAndIncrement();
  }

  private Map<String, Object> toJson() {
    return ImmutableMap.of(
        "id", id,
        "method", methodName,
        "params", parameters);
  }

  public long getId() {
    return id;
  }
}
