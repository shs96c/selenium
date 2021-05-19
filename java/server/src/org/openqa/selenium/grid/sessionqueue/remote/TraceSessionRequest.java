package org.openqa.selenium.grid.sessionqueue.remote;

import org.openqa.selenium.grid.data.SessionRequest;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.tracing.HttpTracing;
import org.openqa.selenium.remote.tracing.TraceContext;
import org.openqa.selenium.remote.tracing.Tracer;

import static org.openqa.selenium.remote.http.HttpMethod.GET;

class TraceSessionRequest {

  private TraceSessionRequest() {
    // Utility methods
  }

  static void inject(Tracer tracer, HttpRequest request, SessionRequest sessionRequest) {
    Require.nonNull("Tracer", tracer);
    Require.nonNull("HTTP request", request);
    Require.nonNull("Session request", sessionRequest);

    HttpRequest spoof = new HttpRequest(GET, "/spoof");
    sessionRequest.getHttpHeaders().forEach((name, values) -> values.forEach(value -> spoof.addHeader(name, value)));

    TraceContext context = HttpTracing.extract(tracer, spoof);
    HttpTracing.inject(tracer, context, request);
  }
}
