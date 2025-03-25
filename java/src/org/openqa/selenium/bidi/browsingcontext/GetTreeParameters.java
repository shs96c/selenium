package org.openqa.selenium.bidi.browsingcontext;

import org.openqa.selenium.json.JsonInput;

import java.util.HashMap;
import java.util.Map;

public class GetTreeParameters {

  private final BrowsingContext root;
  private final int maxDepth;

  public GetTreeParameters(BrowsingContext root, int maxDepth) {
    this.root = root;
    this.maxDepth = maxDepth;
  }

  private Map<String, Object> toJson() {
    Map<String, Object> result = new HashMap<>();
    if (maxDepth != 0) {
      result.put("maxDepth", maxDepth);
    }
    if (root != null) {
      result.put("root", root);
    }

    return result;
  }
}
