package org.openqa.selenium.remote.protocol.common;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonInput;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.NewSessionPayload;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.protocol.HttpCommand;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openqa.selenium.json.Json.MAP_TYPE;
import static org.openqa.selenium.remote.http.HttpMethod.POST;

public class NewSession implements HttpCommand {

  @Override
  public HttpRequest toRequest(Command command) {
    Map<String, ?> parameters = command.getParameters();

    // TODO: alter the `New Session` DriverCommand to not do this.
    if (!parameters.containsKey("desiredCapabilities")) {
      throw new IllegalArgumentException("Unable to find capabilities: " + command);
    }

    try (NewSessionPayload payload = NewSessionPayload.create(parameters);
         StringWriter writer = new StringWriter()) {
      payload.writeTo(writer);

      return new HttpRequest(POST, "/session")
        .setContent(Contents.string(writer.toString(), UTF_8));
    } catch (IOException e) {
      throw new SessionNotCreatedException("Unable to create session payload", e);
    }
  }

  @Override
  public Response onSuccess(SessionId id, HttpResponse response) {
    try (Reader reader = Contents.reader(response);
         JsonInput json = new Json().newInput(reader)) {

      Map<String, Object> caps = null;
      SessionId sessionId = null;

      json.beginObject();
      while (json.hasNext()) {
        if (!"value".equals(json.nextName())) {
          json.skipValue();
          continue;
        }

        json.beginObject();
        while (json.hasNext()) {
          switch (json.nextName()) {
            case "capabilities":
              caps = json.read(MAP_TYPE);
              break;

            case "sessionId":
              sessionId = json.read(SessionId.class);
              break;

            default:
              json.skipValue();
              break;
          }
        }
        json.endObject();
        break;
      }

      if (caps == null || sessionId == null) {
        throw new SessionNotCreatedException("Unable to find session id or capabilities");
      }

      Response toReturn = new Response(sessionId);
      toReturn.setValue(caps);
      return toReturn;
    } catch (IOException e) {
      throw new SessionNotCreatedException("Unable to create session", e);
    }
  }

  @Override
  public Response onError(SessionId id, HttpResponse response) {
    Response toReturn = new Response();
    toReturn.setState("session not created");
    toReturn.setValue(new SessionNotCreatedException("Unable to create session"));
    return toReturn;
  }
}
