package org.openqa.selenium.bidi.browser;

import org.openqa.selenium.json.JsonInput;
import org.openqa.selenium.json.TypeToken;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GetUserContextsResult {

  private final Set<UserContextInfo> allContexts;

  public GetUserContextsResult(Collection<UserContextInfo> allContexts) {
    this.allContexts = Set.copyOf(allContexts);
  }

  public Set<UserContextInfo> getUserContexts() {
    return allContexts;
  }

  private static GetUserContextsResult fromJson(JsonInput input) {
    Set<UserContextInfo> allContexts = new HashSet<>();

    input.beginObject();
    while (input.hasNext()) {
      switch(input.nextName()) {
        case "userContexts":
          allContexts = input.read(new TypeToken<Set<UserContextInfo>>(){}.getType());
          break;

        default:
          input.skipValue();
          break;
      }
    }
    input.endObject();

    return new GetUserContextsResult(allContexts);
  }

}
