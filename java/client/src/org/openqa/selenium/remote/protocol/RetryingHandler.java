package org.openqa.selenium.remote.protocol;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonException;
import org.openqa.selenium.json.JsonInput;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpHandler;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.NoRouteToHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class RetryingHandler implements CommandExecutor {
  private static final Set<String> RETRIABLE_STATUSES = Collections.unmodifiableSet(
    new HashSet<>(Arrays.asList("unknown command", "unknown method", "unsupported operation")));
  private static final Json JSON = new Json();

  private final Map<String, Function<Command, Response>> handlers;

  public RetryingHandler(HttpHandler handler, Map<String, Collection<HttpCommand>> commands) {
    Require.nonNull("HTTP handler", handler);
    Require.nonNull("Command implementations", commands);

    RetryPolicy<HttpResponse> retries = new RetryPolicy<HttpResponse>()
      .handle(IOException.class)
      .handle(UncheckedIOException.class)
      .abortOn(NoRouteToHostException.class)
      .withMaxRetries(3);

    HttpHandler robustHandler = req -> Failsafe.with(retries).get(() -> handler.execute(req));

    Map<String, Function<Command, Response>> handlers = new LinkedHashMap<>();
    commands.forEach((key, fallbacks) -> {
      if (fallbacks.isEmpty()) {
        throw new IllegalStateException("Mappings for commands must be set: " + key);
      }
      handlers.put(key, createFunction(robustHandler, fallbacks.iterator()));
    });

    this.handlers = Collections.unmodifiableMap(handlers);
  }

  private Function<Command, Response> createFunction(HttpHandler handler, Iterator<HttpCommand> iterator) {
    HttpCommand command = iterator.next();
    Function<Command, Response> next = iterator.hasNext() ? createFunction(handler, iterator) : null;

    return cmd -> {
      HttpRequest req = command.toRequest(cmd);

      HttpResponse res = handler.execute(req);

      if (res.isSuccessful()) {
        return command.onSuccess(cmd.getSessionId(), res);
      }

      HttpResponse memoized = ensureContentIsMemoized(res);
      if (next != null && shouldTryNextCommand(memoized)) {
        return next.apply(cmd);
      }

      return command.onError(cmd.getSessionId(), res);
    };
  }

  @Override
  public Response execute(Command command) throws IOException {
    return handlers.getOrDefault(
      command.getName(),
      cmd -> {
        throw new UnsupportedCommandException(String.format("Unknown command: %s", cmd.getName()));
      })
      .apply(command);
  }

  private HttpResponse ensureContentIsMemoized(HttpResponse res) {
    HttpResponse toReturn = new HttpResponse();
    toReturn.setStatus(res.getStatus());
    toReturn.setTargetHost(res.getTargetHost());
    res.getAttributeNames().forEach(name -> toReturn.setAttribute(name, res.getAttribute(name)));
    res.getHeaderNames().forEach(name -> res.getHeaders(name).forEach(value -> toReturn.addHeader(name, value)));

    toReturn.setContent(Contents.memoize(res.getContent()));

    return toReturn;
  }

  private boolean shouldTryNextCommand(HttpResponse res) {
    // We should never be called if the command is successful, but if we are,
    // then we don't want to call the next command
    if (res.isSuccessful()) {
      return false;
    }

    try (Reader reader = Contents.reader(res);
         JsonInput input = JSON.newInput(reader)) {
      input.beginObject();

      while (input.hasNext()) {
        if ("value".equals(input.nextName())) {
          input.beginObject();

          while (input.hasNext()) {
            if ("error".equals(input.nextName())) {
              return RETRIABLE_STATUSES.contains(input.nextString());
            }
          }

          input.endObject();
        } else {
          input.skipValue();
        }
      }

      return true;
    } catch (JsonException | IOException e) {
      return true;
    }
  }
}
