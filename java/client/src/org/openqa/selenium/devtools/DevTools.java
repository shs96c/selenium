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

import static org.openqa.selenium.json.Json.MAP_TYPE;

import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.logging.LogEntry;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DevTools {

  private final Json json = new Json();
  private final WebDriver driver;
  private final ProtocolListener listener;

  public <T extends WebDriver & HasCapabilities> DevTools(T driver) {
    this.driver = Objects.requireNonNull(driver, "The WebDriver instance is required.");

    // For now, we only support chrome

    Object rawOptions = driver.getCapabilities().getCapability("goog:chromeOptions");
    if (!(rawOptions instanceof Map)) {
      throw new IllegalStateException("Driver does not support dev tools API");
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> chromeOptions = (Map<String, Object>) rawOptions;

    Object address = chromeOptions.get("debuggerAddress");
    if (!(address instanceof String)) {
      throw new IllegalStateException("Address does not look like a url: " + address);
    }

    OkHttpClient client = new OkHttpClient.Builder().build();

    Request request = new Request.Builder()
        .url(String.format("http://%s/json/version", address))
        .build();

    Response response;
    try {
      response = client.newCall(request).execute();
      if (!response.isSuccessful()) {
        throw new IllegalStateException("Unable to find debugger endpoint");
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    Map<String, Object> version;
    try (Reader reader = response.body().charStream()) {
      version = json.toType(reader, MAP_TYPE);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    Object rawUrl = version.get("webSocketDebuggerUrl");
    if (!(rawUrl instanceof String)) {
      throw new IllegalStateException("Unable to find debugger url: " + rawUrl);
    }

    this.listener = new ProtocolListener(client, (String) rawUrl);

    // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
    client.dispatcher().executorService().shutdown();
  }

  public void close() {
    listener.close();
  }

  public void log(String domain, Consumer<LogEntry> consumer) {
    if ("console".equals(domain)) {
      listener.registerListener(
          "Console.messageAdded",
          raw -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> consoleMessage = (Map<String, Object>) raw.get("message");
            LogEntry entry = new LogEntry(
                Level.INFO,
                System.currentTimeMillis(),
                String.valueOf(consoleMessage.get("text")));
            consumer.accept(entry);
          });
      listener.fireMethod("Console.enable", ImmutableMap.of());
    }
  }

}
