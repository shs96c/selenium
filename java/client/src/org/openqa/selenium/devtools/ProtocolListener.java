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

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.openqa.selenium.json.Json.MAP_TYPE;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonOutput;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

class ProtocolListener extends WebSocketListener {

  private final Json json = new Json();
  private final Map<Long, CompletableFuture<Object>> methodFutures = new ConcurrentHashMap<>();
  private final Multimap<String, CompletableFuture<Map<String, Object>>> oneOffEvents = HashMultimap.create();
  private final Multimap<String, Consumer<Map<String, Object>>> eventListeners = HashMultimap.create();
  private WebSocket socket;
  private String sessionId;

  public ProtocolListener(OkHttpClient client, String baseUrl) {
    Objects.requireNonNull(client);
    Objects.requireNonNull(baseUrl);

    Future<URL> getRemoteUrl = registerOneOffListener(
        "onOpen",
        map -> {
          try {
            return new URL(String.valueOf(map.get("url")));
          } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
          }
        });

    Request request = new Request.Builder().url(baseUrl).build();
    this.socket = client.newWebSocket(request, this);

    await(getRemoteUrl);
    String targetId = await(registerMethod(
        "Target.getTargets",
        ImmutableMap.of(),
        obj -> {
          Map<?, ?> raw = (Map<?, ?>) obj;
          @SuppressWarnings("unchecked") Collection<Map<String, Object>> allInfos =
              (Collection<Map<String, Object>>) raw.get("targetInfos");

          return allInfos.stream()
              .filter(map -> "page".equals(map.get("type")))
              .map(map -> (String) map.get("targetId"))
              .findFirst()
              .orElse(null);
        }));

    registerListener(
        "Target.receivedMessageFromTarget",
        map -> {
          if (sessionId == null || !sessionId.equals(map.get("sessionId"))) {
            return;
          }

          Map<String, Object> actualMessage = json.toType((String) map.get("message"), MAP_TYPE);

          dispatch(actualMessage);
        });

    // Hilariously, at the time of writing, using flatten mode will always cause messages to fail
    // https://cs.chromium.org/chromium/src/content/browser/devtools/protocol/target_handler.cc?type=cs&q=%22via+the+sessionId+attribute.%22+&g=0&l=610
    this.sessionId = await(registerMethod(
        "Target.attachToTarget",
        ImmutableMap.of("targetId", targetId, "flatten", false),
        obj -> (String) ((Map<?, ?>) obj).get("sessionId")));
  }

  public void fireMethod(String methodName, Map<String, Object> parameters) {
    fire(new CdpMessage(methodName, parameters));
  }

  private void fire(CdpMessage message) {
    Objects.requireNonNull(sessionId, "Session ID has not been established");

    CdpMessage actualMessage = new CdpMessage(
        "Target.sendMessageToTarget",
        ImmutableMap.of("sessionId", sessionId, "message", json.toJson(message)));

    StringBuilder sb = new StringBuilder();
    try (JsonOutput jsonOutput = json.newOutput(sb)) {
      jsonOutput.setPrettyPrint(false).write(actualMessage);
    }
    System.out.println("Sending: " + sb.toString());
    socket.send(sb.toString());
  }

  private <X> Future<X> registerOneOffListener(
      String eventName,
      Function<Map<String, Object>, X> mappingFunction) {
    CompletableFuture<Map<String, Object>> underlying = new CompletableFuture<>();
    oneOffEvents.put(eventName, underlying);
    return underlying.thenApply(mappingFunction);
  }

  private <X> Future<X> registerMethod(
      String methodName,
      Map<String, Object> parameters,
      Function<Object, X> mapper) {

    CdpMessage message = new CdpMessage(methodName, parameters);
    socket.send(json.toJson(message));

    CompletableFuture<Object> underlying = new CompletableFuture<>();
    methodFutures.put(message.getId(), underlying);
    return underlying.thenApply(mapper);
  }

  public void registerListener(String functionName, Consumer<Map<String, Object>> consumer) {
    eventListeners.put(functionName, consumer);
  }

  private <X> X await(Future<X> future) {
    try {
      return future.get(2, MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    if (!this.socket.equals(webSocket)) {
      return;
    }

    dispatchEvent(ImmutableMap.of(
        "method", "onOpen",
        "params", ImmutableMap.of(
            "code", response.code(),
            "message", response.message(),
            "url", response.request().url())));
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    Map<String, Object> result = json.toType(text, MAP_TYPE);

    System.out.println("Received: " + result);

    dispatch(result);
  }

  @Override
  public void onClosed(WebSocket webSocket, int code, String reason) {
    webSocket.close(code, reason);
    this.socket = null;
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    if (socket != null) {
      // I have no idea how to handle failure cases
      t.printStackTrace();
    }
  }

  private void dispatch(Map<String, Object> message) {
    if (message.get("id") instanceof Number && message.containsKey("result")) {
      long key = ((Number) message.get("id")).longValue();
      CompletableFuture<Object> future = methodFutures.remove(key);
      if (future != null) {
        future.complete(message.get("result"));
      }
      return;
    }

    dispatchEvent(message);
  }

  private void dispatchEvent(Map<String, Object> message) {
    List<Map.Entry<String, CompletableFuture<Map<String, Object>>>> allOneOffEvents;

    String method = String.valueOf(message.get("method"));
    @SuppressWarnings("unchecked")
    Map<String, Object> rawParams = (Map<String, Object>) message.get("params");

    synchronized (oneOffEvents) {
      allOneOffEvents = oneOffEvents.entries().stream()
          .filter(entry -> method.equals(entry.getKey()))
          .collect(Collectors.toList());
    }

    allOneOffEvents.forEach(entry -> {
      oneOffEvents.remove(entry.getKey(), entry.getValue());

      entry.getValue().complete(rawParams);
    });

    synchronized (eventListeners) {
      eventListeners.entries().stream()
          .filter(entry -> method.equals(entry.getKey()))
          .forEach(entry -> {
            entry.getValue().accept(rawParams);
          });
    }
  }

  public void close() {
    socket.close(1000, "DevTools closing down");
    socket = null;
  }
}
