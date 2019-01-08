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

package org.openqa.selenium.events.zeromq;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.openqa.selenium.events.Event;
import org.openqa.selenium.events.EventBus;
import org.openqa.selenium.events.Type;
import org.openqa.selenium.json.Json;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ZeroMqEventBus implements EventBus {

  private static final Json JSON = new Json();

  private final ZMQ.Socket publisher;
  private final Map<Type, List<Consumer<Event>>> listeners = new ConcurrentHashMap<>();
  private final AtomicBoolean isRunning = new AtomicBoolean(true);
  private final ExecutorService service;
  private final Future<?> awaitable;

  public ZeroMqEventBus(ZContext context, String connection, boolean bind) {
    Objects.requireNonNull(context, "Context must be set.");
    Objects.requireNonNull(connection, "Connection string must be set.");

    service = Executors.newSingleThreadExecutor();
    awaitable =
        service.submit(
            () -> {
              ZMQ.Poller poller = context.createPoller(1);

              ZMQ.Socket subscriber = context.createSocket(ZMQ.SUB);
              subscriber.setImmediate(true);
              subscriber.connect(connection);
              subscriber.subscribe("".getBytes(UTF_8)); // Subscribe to everything

              poller.register(subscriber, ZMQ.Poller.POLLIN);

              while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                poller.poll(150);

                if (poller.pollin(0)) {
                  Type type = new Type(new String(subscriber.recv(ZMQ.DONTWAIT), UTF_8));
                  String data = new String(subscriber.recv(ZMQ.DONTWAIT), UTF_8);

                  List<Consumer<Event>> typeListeners = listeners.get(type);
                  if (typeListeners == null) {
                    continue;
                  }

                  Object converted = JSON.toType(data, Object.class);
                  Event event = new Event(type, converted);
                  typeListeners.parallelStream().forEach(listener -> listener.accept(event));
                }
              }

              poller.close();
              subscriber.close();
            });

    publisher = context.createSocket(ZMQ.PUB);
    publisher.setImmediate(true);

    boolean connected;
    if (bind) {
      connected = publisher.bind(connection);
    } else {
      connected = publisher.connect(connection);
    }

    if (!connected) {
      throw new RuntimeException("Unable to bind socket: " + publisher.errno());
    }

    // The docs say not to do this. We should really have a REQ/REP socket pair we use,
    // but we're cowboys so this is okay.
    try {
      Thread.sleep(250);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void addListener(Type type, Consumer<Event> listener) {
    Objects.requireNonNull(type, "Event type must be set.");
    Objects.requireNonNull(listener, "Event listener must be set.");

    List<Consumer<Event>> typeListeners = listeners.computeIfAbsent(type, t -> new LinkedList<>());
    typeListeners.add(listener);
  }

  @Override
  public void fire(Event event) {
    Objects.requireNonNull(event, "Event to fire must be set.");

    publisher.sendMore(event.getType().getName().getBytes(UTF_8));

    byte[] payload = JSON.toJson(event.getData()).getBytes(UTF_8);

    publisher.send(payload);
  }

  @Override
  public void close() {
    isRunning.set(false);
    try {
      awaitable.get(500, MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (Exception e) {
      // Nothing sane to do
    }
    publisher.close();
    service.shutdown();
  }
}
