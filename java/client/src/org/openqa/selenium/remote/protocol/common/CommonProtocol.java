package org.openqa.selenium.remote.protocol.common;

import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.protocol.HttpCommand;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class CommonProtocol {

  public Map<String, Collection<HttpCommand>> getCommands() {
    TreeMap<String, Collection<HttpCommand>> commands = new TreeMap<>();

    commands.put(DriverCommand.NEW_SESSION, Arrays.asList(new NewSession()));

    return Collections.unmodifiableMap(commands);
  }

}
