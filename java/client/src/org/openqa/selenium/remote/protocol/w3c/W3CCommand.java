package org.openqa.selenium.remote.protocol.w3c;

import org.openqa.selenium.internal.Require;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonInput;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.protocol.HttpCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.openqa.selenium.json.Json.JSON_UTF_8;
import static org.openqa.selenium.remote.ErrorCodes.SUCCESS_STRING;
import static org.openqa.selenium.remote.http.HttpMethod.POST;

public class W3CCommand implements HttpCommand {

  private final HttpMethod method;
  private final String[] template;
  private final Type typeOfValue;
  private static final Json JSON = new Json();

  public W3CCommand(HttpMethod method, String template, Type typeOfValue) {
    this.method = Require.nonNull("HTTP method", method);
    this.template = Require.nonNull("URL Template", template).split("/");
    this.typeOfValue = Require.nonNull("Type of response value", typeOfValue);
  }

  @Override
  public HttpRequest toRequest(Command command) {
    String path = Arrays.stream(template)
      .map(part -> {
        if (":sessionId".equals(part)) {
          if (command.getSessionId() == null) {
            throw new IllegalArgumentException("Unable to find session id, but it is expected: " + command);
          }
          return command.getSessionId().toString();
        }

        if (part.startsWith(":")) {
          Object value = command.getParameters().get(part.substring(1));
          if (value == null) {
            throw new IllegalArgumentException(
              String.format("Unable to find %s, but it is expected: %s", part.substring(1), command));
          }
        }

        return part;
      })
      .collect(Collectors.joining("/"));

    HttpRequest request = new HttpRequest(method, path)
      .addHeader("Content-Type", JSON_UTF_8)
      .addHeader("Cache-Control", "no-cache");

    if (method == POST) {
      request = request.setContent(Contents.asJson(command.getParameters()));
    }

    return request;
  }

  @Override
  public Response onSuccess(SessionId id, HttpResponse response) {
    Response res = new Response(id);
    res.setState(SUCCESS_STRING);

    try (InputStream is = response.getContent().get();
         InputStreamReader isr = new InputStreamReader(is, response.getContentEncoding());
         BufferedReader reader = new BufferedReader(isr);
         JsonInput json = JSON.newInput(reader)) {
      json.beginObject();
      while (json.hasNext()) {
        if ("value".equals(json.nextName())) {
          res.setValue(json.read(typeOfValue));
          break;
        }
        json.skipValue();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return res;
  }

  @Override
  public Response onError(SessionId id, HttpResponse response) {
    return new W3CErrors().create(id, response);
  }
}
