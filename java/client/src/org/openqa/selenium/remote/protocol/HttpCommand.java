package org.openqa.selenium.remote.protocol;

import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

public interface HttpCommand {

  HttpRequest toRequest(Command command);

  Response onSuccess(SessionId id, HttpResponse response);

  Response onError(SessionId id, HttpResponse response);
}
