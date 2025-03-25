package org.openqa.selenium.bidi.browser;

import org.openqa.selenium.json.JsonInput;

public class UserContextInfo {

  private final UserContext context;

  public UserContextInfo(UserContext context) {
    this.context = context;
  }

  private static UserContextInfo fromJson(JsonInput input) {
    String userContext = null;

    input.beginObject();
    while(input.hasNext()) {
      switch(input.nextName()) {
        case "userContext":
          userContext = input.nextString();
          break;

        default:
          input.skipValue();
          break;
      }
    }
    input.endObject();

    return new UserContextInfo(new UserContext(userContext));
  };
}
