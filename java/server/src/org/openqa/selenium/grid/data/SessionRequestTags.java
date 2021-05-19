package org.openqa.selenium.grid.data;

import org.openqa.selenium.remote.tracing.Span;

import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SessionRequestTags {

  private SessionRequestTags() {
    // Utility methods
  }

  public static final BiConsumer<Span, SessionRequest> SESSION_REQUEST = (span, request) -> {
    span.setAttribute(
      "session_request.dialects",
      request.getDownstreamDialects().stream().map(String::valueOf).collect(Collectors.joining(", ")));
    span.setAttribute(
      "session_request.enqueued",
      request.getEnqueued().toEpochMilli());
  };
}
