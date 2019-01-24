package org.openqa.selenium.grid.distributor.httpd;

import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.grid.config.Config;
import org.openqa.selenium.grid.config.MapConfig;

public class DefaultDistributorConfig extends MapConfig {

  public DefaultDistributorConfig() {
    super(ImmutableMap.of(
        "events", ImmutableMap.of(
            "address", "tcp://*:4445",
            "bind", true)));
  }
 
}
