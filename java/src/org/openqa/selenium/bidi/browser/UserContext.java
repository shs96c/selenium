package org.openqa.selenium.bidi.browser;

import org.openqa.selenium.internal.Require;
import org.openqa.selenium.json.JsonInput;

public class UserContext {

  private final String context;

  public UserContext(String context) {
    this.context = Require.nonNull("context", context);
  }

  private String toJson() {
    return context;
  }

  private static UserContext fromJson(JsonInput context) {
    return new UserContext(context.nextString());
  }
}
