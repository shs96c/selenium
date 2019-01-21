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

package org.openqa.selenium.grid.distributor;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.Test;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.grid.GridTopology;
import org.openqa.selenium.grid.data.Session;
import org.openqa.selenium.grid.node.local.LocalNode;
import org.openqa.selenium.remote.SessionId;

import java.util.UUID;

public class SessionLifetimeTest {

  private final Capabilities stereotype = new ImmutableCapabilities("browserName", "cheese");

  @Test
  public void shouldBeAbleToCreateASession() {
    GridTopology grid = GridTopology.builder().buildGrid();

    assertThatExceptionOfType(SessionNotCreatedException.class)
        .isThrownBy(() -> grid.createWebDriver(stereotype));

    grid.addNode(
        (tracer, factory, bus, uri, sessions) ->
            LocalNode.builder(tracer, factory, bus, uri, sessions)
                .add(stereotype, caps -> new Session(new SessionId(UUID.randomUUID()), uri, caps))
                .build());

    // The first session should be created fine
    grid.createWebDriver(stereotype);

    // But a second should not, because we've used up all the sessions
    assertThatExceptionOfType(SessionNotCreatedException.class)
        .isThrownBy(() -> grid.createWebDriver(stereotype));
  }

  @Test
  public void closingASessionShouldAllowTheSessionFactoryToBeReused() {

  }

}
