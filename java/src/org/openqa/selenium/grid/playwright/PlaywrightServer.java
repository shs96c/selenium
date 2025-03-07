package org.openqa.selenium.grid.playwright;

import org.openqa.selenium.grid.config.MapConfig;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.openqa.selenium.netty.server.NettyServer;
import org.openqa.selenium.remote.http.HttpResponse;

import java.util.Map;

public class PlaywrightServer {

  public static void main(String[] args) {
    var options = new BaseServerOptions(new MapConfig(Map.of(
      "server", Map.of("port", 4444)
    )));
    var server = new NettyServer(
      options,
      req -> new HttpResponse().setStatus(500),
      new PlaywrightHandler("/playwright"));

    server.start();
  }

}
