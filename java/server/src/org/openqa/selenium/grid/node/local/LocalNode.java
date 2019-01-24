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

package org.openqa.selenium.grid.node.local;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.concurrent.Regularly;
import org.openqa.selenium.events.EventBus;
import org.openqa.selenium.grid.component.HealthCheck;
import org.openqa.selenium.grid.data.Session;
import org.openqa.selenium.grid.node.Node;
import org.openqa.selenium.grid.node.NodeStatus;
import org.openqa.selenium.grid.sessionmap.SessionMap;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.tracing.DistributedTracer;
import org.openqa.selenium.remote.tracing.Span;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LocalNode extends Node {

  public static final Logger LOG = Logger.getLogger(LocalNode.class.getName());
  private final SessionMap sessions;
  private final URI externalUri;
  private final HealthCheck healthCheck;
  private final int maxSessionCount;
  private final List<WebDriverInstance> instances;
  private final ReadWriteLock lock = new ReentrantReadWriteLock(/* fair */ true);
  private final Consumer<WebDriverInstance> expire;

  private LocalNode(
      DistributedTracer tracer,
      SessionMap sessions,
      Clock clock,
      URI uri,
      HealthCheck healthCheck,
      int maxSessionCount,
      Duration sessionTimeout,
      List<WebDriverInstance> instances) {
    super(tracer, UUID.randomUUID());

    Preconditions.checkArgument(
        maxSessionCount > 0,
        "Only a positive number of sessions can be run: " + maxSessionCount);

    this.sessions = Objects.requireNonNull(sessions);
    this.externalUri = Objects.requireNonNull(uri);
    this.healthCheck = Objects.requireNonNull(healthCheck);
    this.maxSessionCount = Math.min(maxSessionCount, instances.size());
    this.instances = ImmutableList.copyOf(instances);

    this.expire = instance -> {
      Instant now = clock.instant();
      Instant end = instance.getLastAccessTime().plus(sessionTimeout);
      if (now.isAfter(end)) {
        // This will call instances that are not running, but that's okay because the first
        // thing `stop` does is check to see if it's running. *shrug* We could do the check
        // here, but this makes the code simpler.
        instance.stop();
      }
    };

    Regularly regularly = new Regularly("Local node session clean up");
    regularly.submit(
        () -> {
          // We don't expect staggering numbers of instances, so while this isn't the most efficient
          // thing to do, it shouldn't impact CPU usage too much.
          instances.parallelStream().forEach(expire);
        },
        Duration.ofSeconds(30),
        Duration.ofSeconds(30));
  }

  @VisibleForTesting
  public int getCurrentSessionCount() {
    Lock readLock = lock.readLock();
    readLock.lock();
    try {
      long count = instances.parallelStream()
          .mapToInt(instance -> instance.isAvailable() ? 0 : 1)
          .sum();

      // It seems wildly unlikely we'll overflow an int
      return Math.toIntExact(count);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean isSupporting(Capabilities capabilities) {
    try (Span span = tracer.createSpan("node.is-supporting", tracer.getActiveSpan())) {
      span.addTag("capabilities", capabilities);
      boolean toReturn = instances.parallelStream().anyMatch(factory -> factory.test(capabilities));
      span.addTag("match-made", toReturn);
      return toReturn;
    }
  }

  @Override
  public Optional<Session> newSession(Capabilities capabilities) {
    try (Span span = tracer.createSpan("node.new-session", tracer.getActiveSpan())) {
      span.addTag("capabilities", capabilities);
      LOG.info("Attempting to create session for " + capabilities);

      if (getCurrentSessionCount() >= maxSessionCount) {
        span.addTag("result", "session count exceeded");
        LOG.info("Maximum session count exceeded (" + maxSessionCount + ")");
        return Optional.empty();
      }

      // TODO: Creating the actual session within the lock prevents all the reads.
      // Since we only create sessions infrequently, we should be okay, but it would be better to
      // get instance, and then try and create the session outside of the lock.
      Lock writeLock = lock.writeLock();
      writeLock.lock();
      try {
        Optional<WebDriverInstance> possibleInstance = instances.stream()
            // We only want to try and set up a session on an instance that has nothing running...
            .filter(WebDriverInstance::isAvailable)
            // ... and which supports the capabilities (the "is free" check is cheaper, so placed
            // first)
            .filter(instance -> instance.test(capabilities))
            // Now we try and start the session. If it's successful, then the instance will no
            // longer be available. If it's not, then the instance remains available...
            .peek(instance -> instance.apply(capabilities))
            // ... so we should filter on availability
            .filter(instance -> !instance.isAvailable())
            .findFirst();

        if (!possibleInstance.isPresent()) {
          span.addTag("result", "No possible session detected");
          LOG.info("No session factory available");
          return Optional.empty();
        }

        LOG.info("Attempting to create session");
        WebDriverInstance instance = possibleInstance.get();
        Session session = instance.getSession();
        span.addTag("session.id", session.getId());
        span.addTag("session.capabilities", session.getCapabilities());
        span.addTag("session.uri", session.getUri());
        LOG.info("Created session: " + session);

        try {
          sessions.add(session);
        } catch (Exception e) {
          LOG.info(Throwables.getStackTraceAsString(e));
          // We couldn't register, so this session has become unreachable. Kill it.
          instance.stop();
          return Optional.empty();
        }

        return Optional.of(session);
      } finally {
        writeLock.unlock();
      }
    }
  }

  @Override
  protected boolean isSessionOwner(SessionId id) {
    try (Span span = tracer.createSpan("node.is-session-owner", tracer.getActiveSpan())) {
      Objects.requireNonNull(id, "Session ID has not been set");
      span.addTag("session.id", id);

      return getInstance(id).isPresent();
    }
  }

  @Override
  public Session getSession(SessionId id) throws NoSuchSessionException {
    try (Span span = tracer.createSpan("node.get-session", tracer.getActiveSpan())) {
      Objects.requireNonNull(id, "Session ID has not been set");

      span.addTag("session.id", id);

      Optional<Session> session = getInstance(id).map(WebDriverInstance::getSession);
      if (!session.isPresent()) {
        span.addTag("result", false);
        throw new NoSuchSessionException("Cannot find session with id: " + id);
      }

      Session toReturn = session.get();
      span.addTag("session.capabilities", toReturn.getCapabilities());
      span.addTag("session.uri", toReturn.getUri());
      return toReturn;
    }
  }

  @Override
  public void executeWebDriverCommand(HttpRequest req, HttpResponse resp) {
    try (Span span = tracer.createSpan("node.webdriver-command", tracer.getActiveSpan())) {

      span.addTag("http.method", req.getMethod());
      span.addTag("http.url", req.getUri());

      // True enough to be good enough
      if (!req.getUri().startsWith("/session/")) {
        throw new UnsupportedCommandException(String.format(
            "Unsupported command: (%s) %s", req.getMethod(), req.getMethod()));
      }

      String[] split = req.getUri().split("/", 4);
      SessionId id = new SessionId(split[2]);

      span.addTag("session.id", id);

      Optional<WebDriverInstance> instance = getInstance(id);
      if (!instance.isPresent()) {
        span.addTag("result", "Session not found");
        throw new NoSuchSessionException("Cannot find session with id: " + id);
      }

      Session session = instance.map(WebDriverInstance::getSession).get();
      span.addTag("session.capabilities", session.getCapabilities());
      span.addTag("session.uri", session.getUri());

      try {
        instance.get().execute(req, resp);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private Optional<WebDriverInstance> getInstance(SessionId id) {
    Objects.requireNonNull(id, "Session ID must be set.");
    Lock readLock = lock.readLock();
    readLock.lock();
    try {
      List<WebDriverInstance> allInstances = instances.parallelStream()
          .filter(instance -> id.equals(instance.getSessionId()))
          .collect(ImmutableList.toImmutableList());

      if (allInstances.size() > 1) {
        throw new IllegalStateException(String.format(
            "More than one session with ID %s is registered: %s",
            id,
            allInstances));
      }

      if (allInstances.isEmpty()) {
        return Optional.empty();
      }

      WebDriverInstance instance = allInstances.get(0);
      expire.accept(instance);

      return instance.isAvailable() ? Optional.empty() : Optional.of(instance);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void stop(SessionId id) throws NoSuchSessionException {
    try (Span span = tracer.createSpan("node.stop-session", tracer.getActiveSpan())) {
      Objects.requireNonNull(id, "Session ID must be set.");
      Lock writeLock = lock.writeLock();
      writeLock.lock();
      try {
        List<WebDriverInstance> allInstances = instances.parallelStream()
            .filter(instance -> id.equals(instance.getSessionId()))
            .collect(ImmutableList.toImmutableList());

        if (allInstances.isEmpty()) {
          throw new NoSuchSessionException("Unable to find session with ID: " + id);
        }

        allInstances.parallelStream().forEach(instance -> {
          span.addTag("session.capabilities", instance.getCapabilities());
          span.addTag("session.uri", instance.getUri());

          instance.stop();
        });
      } finally {
        writeLock.unlock();
      }
    }
  }

  @Override
  public NodeStatus getStatus() {
    Map<Capabilities, Integer> available = new ConcurrentHashMap<>();
    Map<Capabilities, Integer> used = new ConcurrentHashMap<>();

    for (WebDriverInstance instance : instances) {
      Map<Capabilities, Integer> map = instance.isAvailable() ? available : used;
      Capabilities caps = instance.getCapabilities();
      Integer count = map.getOrDefault(caps, 0);
      map.put(caps, count + 1);
    }

    return new NodeStatus(
        getId(),
        externalUri,
        maxSessionCount,
        available,
        used);
  }

  @Override
  public HealthCheck getHealthCheck() {
    return healthCheck;
  }

  private Map<String, Object> toJson() {
    return ImmutableMap.of(
        "id", getId(),
        "uri", externalUri,
        "maxSessions", maxSessionCount,
        "capabilities", instances.stream()
            .map(WebDriverInstance::getCapabilities)
            .collect(Collectors.toSet()));
  }

  public static Builder builder(
      DistributedTracer tracer,
      HttpClient.Factory httpClientFactory,
      EventBus bus,
      URI uri,
      SessionMap sessions) {
    return new Builder(tracer, httpClientFactory, bus, uri, sessions);
  }

  public static class Builder {

    private final DistributedTracer tracer;
    private final HttpClient.Factory httpClientFactory;
    private final EventBus bus;
    private final URI uri;
    private final SessionMap sessions;
    private final ImmutableList.Builder<WebDriverInstance> factories;
    private int maxCount = Runtime.getRuntime().availableProcessors() * 5;
    private Clock clock = Clock.systemDefaultZone();
    private Duration sessionTimeout = Duration.ofMinutes(5);
    private HealthCheck healthCheck;

    public Builder(
        DistributedTracer tracer,
        HttpClient.Factory httpClientFactory,
        EventBus bus,
        URI uri,
        SessionMap sessions) {
      this.tracer = Objects.requireNonNull(tracer);
      this.httpClientFactory = Objects.requireNonNull(httpClientFactory);
      this.bus = bus;
      this.uri = Objects.requireNonNull(uri);
      this.sessions = Objects.requireNonNull(sessions);
      this.factories = ImmutableList.builder();
    }

    public Builder add(Capabilities stereotype, Function<Capabilities, Session> factory) {
      Objects.requireNonNull(stereotype, "Capabilities must be set.");
      Objects.requireNonNull(factory, "Session factory must be set.");

      factories.add(new WebDriverInstance(
          tracer,
          bus,
          clock,
          httpClientFactory,
          stereotype,
          factory));

      return this;
    }

    public Builder maximumConcurrentSessions(int maxCount) {
      Preconditions.checkArgument(
          maxCount > 0,
          "Only a positive number of sessions can be run: " + maxCount);

      this.maxCount = maxCount;
      return this;
    }

    public Builder sessionTimeout(Duration timeout) {
      sessionTimeout = timeout;
      return this;
    }

    public LocalNode build() {
      HealthCheck check =
          healthCheck == null ?
          () -> new HealthCheck.Result(true, uri + " is ok") :
          healthCheck;

      return new LocalNode(
          tracer,
          sessions,
          clock,
          uri,
          check,
          maxCount,
          sessionTimeout,
          factories.build());
    }

    public Advanced advanced() {
      return new Advanced();
    }

    public class Advanced {

      public Advanced clock(Clock clock) {
        Builder.this.clock = Objects.requireNonNull(clock, "Clock must be set.");
        return this;
      }

      public Advanced healthCheck(HealthCheck healthCheck) {
        Builder.this.healthCheck = Objects.requireNonNull(healthCheck, "Health check must be set.");
        return this;
      }

      public Node build() {
        return Builder.this.build();
      }
    }
  }

}
