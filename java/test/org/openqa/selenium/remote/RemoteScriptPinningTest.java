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

package org.openqa.selenium.remote;

import org.junit.Test;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.ScriptKey;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.Filter;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.Routable;
import org.openqa.selenium.remote.http.Route;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.json.Json.JSON_UTF_8;

public class RemoteScriptPinningTest {

  private final SessionId id = new SessionId(UUID.randomUUID());

  @Test
  public void ifRemoteEndSupportsScriptPinningDoesNotFallBackToExecuteScript() {
    Routable route = Route.combine(
      Route.post("/session/{sessionId}/se/pin").to(() -> null),
      Route.post("/session/{sessionId}/se/pin/12345").to(() -> null));

    WebDriver driver = createDriver(route);

    JavascriptExecutor js = (JavascriptExecutor) driver;
    ScriptKey key = js.pin("return 'cheese!'");
    Object value = js.executeScript(key);

    assertThat(value).isEqualTo("cheese!");
  }

  @Test
  public void ifRemoteEndDoesNotSupportScriptPinningOrCdpShouldFallbackToRegularScriptPinning() {
  }

  @Test
  public void ifRemoteEndDoesNotSupportScriptPinningButHasCdpThenThatShouldBeUsed() {
  }

  @Test
  public void onceAPinningStrategyHasBeenSelectedItShouldBeUsed() {
  }

  private WebDriver createDriver(Routable route) {
    Filter addJson = next -> req -> next.execute(req).setHeader("Content-Type", JSON_UTF_8);

    Route createSession = Route.post("/session")
      .to(() -> req ->
        new HttpResponse()
          .setContent(Contents.asJson(
            Map.of("value", Map.of(
              "sessionId", id, "capabilities",
              new ImmutableCapabilities("browserName", "cheese"))))));

    Routable handler = Route.combine(createSession, route).with(addJson);

    return RemoteWebDriver.builder()
      .oneOf(new ImmutableCapabilities())
      .address("http://localhost:3456")
      .connectingWith(cc -> handler)
      .build();
  }

}
