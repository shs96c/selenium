package org.openqa.selenium.remote.protocol.w3c;

import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.protocol.HttpCommand;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class W3CProtocol {

  public Map<String, Collection<HttpCommand>> getCommands() {
    TreeMap<String, Collection<HttpCommand>> commands = new TreeMap<>();

    commands.put("get", Arrays.asList(new W3CCommand(HttpMethod.POST, "/session/:sessionId/url", Void.class)));

    return Collections.unmodifiableMap(commands);
  }
}
