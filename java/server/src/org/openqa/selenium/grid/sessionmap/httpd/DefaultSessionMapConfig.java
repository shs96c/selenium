package org.openqa.selenium.grid.sessionmap.httpd;

import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.grid.config.MapConfig;

public class DefaultSessionMapConfig extends MapConfig {

  public DefaultSessionMapConfig() {
    super(ImmutableMap.of(
        "events", ImmutableMap.of(
            "address", "tcp://*:4445")));
  }

}
