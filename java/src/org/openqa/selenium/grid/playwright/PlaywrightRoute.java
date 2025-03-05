package org.openqa.selenium.grid.playwright;

import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.Routable;
import org.openqa.selenium.remote.http.Route;

import java.io.UncheckedIOException;

public class PlaywrightRoute implements Routable {

  private final Route route;

  public PlaywrightRoute(String prefix) {
    route = Route.prefix(prefix).to(Route.matching(req -> true).to(() -> this));
  }

  @Override
  public boolean matches(HttpRequest req) {
    return route.matches(req);
  }

  @Override
  public HttpResponse execute(HttpRequest req) throws UncheckedIOException {
    return null;
  }
}
