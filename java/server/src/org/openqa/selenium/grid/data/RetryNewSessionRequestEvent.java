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

package org.openqa.selenium.grid.data;

import org.openqa.selenium.events.Event;
import org.openqa.selenium.events.EventListener;
import org.openqa.selenium.events.EventName;
import org.openqa.selenium.internal.Require;

import java.util.function.Consumer;

public class RetryNewSessionRequestEvent extends Event {

  private static final EventName RETRY_NEW_SESSION_REQUEST = new EventName("retry-new-session-request");

  public RetryNewSessionRequestEvent(RequestId requestId) {
    super(RETRY_NEW_SESSION_REQUEST, requestId);
  }

  public static EventListener<RequestId> listener(Consumer<RequestId> handler) {
    Require.nonNull("Handler", handler);

    return new EventListener<>(RETRY_NEW_SESSION_REQUEST, RequestId.class, handler);
  }

}
