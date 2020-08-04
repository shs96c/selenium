package org.openqa.selenium.remote.protocol.w3c;

import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.InvalidArgumentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonException;
import org.openqa.selenium.json.JsonInput;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpResponse;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class W3CErrors {

  private final Map<String, ToException> converters;
  private final ToException defaultConverter;

  W3CErrors() {
    Map<String, ToException> converters = new HashMap<>();

    converters.put("element click intercepted", (msg, data) -> new NoSuchElementException(msg));
    converters.put("element not interactable", (msg, data) -> new ElementNotInteractableException(msg));
    converters.put("insecure certificate", (msg, data) -> new WebDriverException("Insecure certificate detected: " + msg));
    converters.put("invalid argument", (msg, data) -> new InvalidArgumentException(msg));

    this.converters = Collections.unmodifiableMap(converters);

    this.defaultConverter = (msg, data) -> new WebDriverException(msg);
  }

  Response create(SessionId id, HttpResponse res) {
    Require.nonNull("Response", res);

    String message = "Unable to determine the cause of the exception";
    Object data = null;
    String errorCode = "unknown error";

    try (Reader reader = Contents.reader(res);
         JsonInput json = new Json().newInput(reader)) {
      json.beginObject();
      while (json.hasNext()) {
        if ("value".equals(json.nextName())) {
          json.beginObject();
          while (json.hasNext()) {
            switch (json.nextName()) {
              case "data":
                data = json.read(Object.class);
                break;

              case "error":
                errorCode = json.nextString();
                break;

              case "message":
                message = json.nextString();
                break;

              default:
                json.skipValue();
                break;
            }
          }
          break;
        } else {
          json.skipValue();
        }
      }

      Response response = new Response(id);

      // TODO: Flesh out the stack trace
      Throwable throwable = converters.getOrDefault(errorCode, defaultConverter).convert(message, data);

      response.setValue(throwable);
      response.setState(errorCode);
      return response;
    } catch (IOException | JsonException e) {
      Response response = new Response(id);
      response.setState("unknown error");
      response.setValue(new WebDriverException(e));
      return response;
    }
  }

  @FunctionalInterface
  private interface ToException {
    Throwable convert(String message, Object data);
  }
}
