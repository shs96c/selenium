package org.openqa.selenium.bidi.browser;

import org.openqa.selenium.json.JsonInput;
import org.openqa.selenium.json.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

public class GetClientWindowsResult {

  private final static Type SET_OF_WINDOWS = new TypeToken<Set<ClientWindowInfo>>(){}.getType();
  private final Set<ClientWindowInfo> windows;

  public GetClientWindowsResult(Collection<ClientWindowInfo> windows) {
    this.windows = Set.copyOf(windows);
  }

  public Set<ClientWindowInfo> getWindows() {
    return windows;
  }

  private static GetClientWindowsResult fromJson(JsonInput input) {
    Set<ClientWindowInfo> windowInfos = Set.of();

    input.beginObject();
    while (input.hasNext()) {
      switch(input.nextName()) {
        case "clientWindows":
          windowInfos = input.read(SET_OF_WINDOWS);
          break;

        default:
          input.skipValue();
          break;
      }
    }
    input.endObject();

    return new GetClientWindowsResult(windowInfos);
  }

}
