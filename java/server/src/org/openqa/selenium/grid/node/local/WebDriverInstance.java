package org.openqa.selenium.grid.node.local;

import static org.openqa.selenium.remote.http.HttpMethod.DELETE;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.events.EventBus;
import org.openqa.selenium.grid.data.Session;
import org.openqa.selenium.grid.data.SessionEndEvent;
import org.openqa.selenium.grid.web.CommandHandler;
import org.openqa.selenium.grid.web.ReverseProxyHandler;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.tracing.DistributedTracer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

class WebDriverInstance implements
    CommandHandler, Function<Capabilities, Optional<Session>>, Predicate<Capabilities> {

  private final DistributedTracer tracer;
  private final EventBus bus;
  private final Clock clock;
  private final HttpClient.Factory clientFactory;
  private final Capabilities stereotype;
  private final Function<Capabilities, Session> factory;
  private Session activeSession;
  private CommandHandler handler;
  private Instant lastAccessTime;

  WebDriverInstance(
      DistributedTracer tracer,
      EventBus bus,
      Clock clock,
      HttpClient.Factory clientFactory,
      Capabilities stereotype,
      Function<Capabilities, Session> factory) {
    this.tracer = tracer;
    this.bus = Objects.requireNonNull(bus, "Event bus must be set.");
    this.clock = Objects.requireNonNull(clock, "Clock must be set.");
    this.clientFactory = Objects.requireNonNull(clientFactory, "HTTP client factory must be set.");
    this.stereotype = Objects.requireNonNull(stereotype, "Stereotype must be set.");
    this.factory = factory;
    this.lastAccessTime = clock.instant();
  }

  public boolean isAvailable() {
    return activeSession == null;
  }

  public Session getSession() {
    return activeSession;
  }

  public SessionId getSessionId() {
    return activeSession == null ? null : activeSession.getId();
  }

  public URI getUri() {
    return activeSession == null ? null : activeSession.getUri();
  }

  public Capabilities getCapabilities() {
    return stereotype;
  }

  public Instant getLastAccessTime() {
    return lastAccessTime;
  }

  @Override
  public boolean test(Capabilities capabilities) {
    if (!isAvailable()) {
      return false;
    }

    return stereotype.getCapabilityNames().stream()
        .allMatch(
            name ->
                Objects.equals(stereotype.getCapability(name), capabilities.getCapability(name)));
  }

  @Override
  public synchronized Optional<Session> apply(Capabilities capabilities) {
    if (!isAvailable()) {
      throw new IllegalStateException(
          "Session has already been started. ID is " + activeSession.getId());
    }

    try {
      Session session = factory.apply(capabilities);

      if (session instanceof CommandHandler) {
        handler = (CommandHandler) session;
      } else {
        try {
          HttpClient client = clientFactory.createClient(session.getUri().toURL());
          handler = new ReverseProxyHandler(client);
        } catch (MalformedURLException e) {
          throw new UncheckedIOException(e);
        }
      }

      this.activeSession = session;
      return Optional.of(session);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public void stop() {
    if (isAvailable()) {
      return;  // Nothing to do, but it's fine to call stop anyway
    }

    // * Kill browser. — Sync
    try {
      handler.execute(new HttpRequest(DELETE, "/session/" + getSessionId()), new HttpResponse());
    } catch (Exception e) {
      // Swallow. We're closing the session anyway
    }

    handler = null;

    // There are at least two listeners for this event: the Distributor (so it can update its
    // internal state) and the SessionMap (so it can remove the entry). Neither of these are time
    // critical, and the grid will continue to function properly if these are delayed a little, so
    // async communication is fine.
    bus.fire(new SessionEndEvent(activeSession.getId()));

    // Finally, indicate that we're ready to go again
    activeSession = null;
  }

  @Override
  public void execute(HttpRequest req, HttpResponse resp) throws IOException {
    if (isAvailable()) {
      throw new IllegalStateException("There is no currently active session");
    }

    lastAccessTime = clock.instant();

    if (req.getMethod() == DELETE && req.getUri().equals("/session/" + getSessionId())) {
      stop();
    } else {
      handler.execute(req, resp);
    }
  }
}
