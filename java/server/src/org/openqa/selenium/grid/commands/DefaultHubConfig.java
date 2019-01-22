package org.openqa.selenium.grid.commands;

import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.grid.config.MapConfig;

public class DefaultHubConfig extends MapConfig {

  public DefaultHubConfig() {
    super(ImmutableMap.of(
        "events", ImmutableMap.of(
            "bind", true,
            "address", "tcp://*:4445")));
  }

}
