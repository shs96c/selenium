// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.bidi.browsingcontext;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.internal.Require;

public class NavigationInfo {

  private final String browsingContextId;

  @Nullable private final String navigationId;

  private final long timestamp;

  private final String url;

  protected NavigationInfo(
      String browsingContextId, @Nullable String navigationId, long timestamp, String url) {
    this.browsingContextId = Require.nonNull("browsingContext", browsingContextId);
    this.navigationId = navigationId;
    this.timestamp = timestamp;
    this.url = Require.nonNull("URL", url);
  }

  @SuppressWarnings("unused")
  private static Map<String, String> jsonAliases() {
    return Map.of("context", "browsingContextId", "navigation", "navigationId");
  }

  public String getBrowsingContextId() {
    return browsingContextId;
  }

  @Nullable
  public String getNavigationId() {
    return navigationId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getUrl() {
    return url;
  }
}
