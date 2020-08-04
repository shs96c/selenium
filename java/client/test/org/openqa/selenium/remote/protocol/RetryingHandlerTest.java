package org.openqa.selenium.remote.protocol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpHandler;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.protocol.common.CommonProtocol;
import org.openqa.selenium.remote.protocol.w3c.W3CCommand;
import org.openqa.selenium.remote.protocol.w3c.W3CProtocol;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.openqa.selenium.json.Json.JSON_UTF_8;
import static org.openqa.selenium.remote.ErrorCodes.SUCCESS_STRING;
import static org.openqa.selenium.remote.http.HttpMethod.GET;

public class RetryingHandlerTest {

  @Test
  public void doesNotRetrySuccessfulUrls() throws IOException {
    HttpHandler handler = new CannedResponses(
      req -> new HttpResponse()
        .setHeader("Content-Type", JSON_UTF_8)
        .setContent(Contents.asJson(ImmutableMap.of("value", "cheddar"))));

    CommandExecutor executor = new RetryingHandler(
      handler,
      ImmutableMap.of("cheese", ImmutableList.of(new W3CCommand(GET, "/se/cheese", String.class))));

    Response res = executor.execute(new Command(null, "cheese"));

    assertThat(res.getState()).isEqualTo(SUCCESS_STRING);
    assertThat(res.getValue()).isEqualTo("cheddar");
  }

  @Test
  public void ioExceptionsShouldCauseARetry() throws IOException {
    HttpHandler handler = new CannedResponses(
      req -> { throw new UncheckedIOException(new IOException("Oh noes!")); },
      req -> new HttpResponse()
        .setHeader("Content-Type", JSON_UTF_8)
        .setContent(Contents.asJson(ImmutableMap.of("value", "cheddar"))));

    CommandExecutor executor = new RetryingHandler(
      handler,
      ImmutableMap.of("cheese", ImmutableList.of(new W3CCommand(GET, "/se/cheese", String.class))));

    Response res = executor.execute(new Command(null, "cheese"));

    assertThat(res.getState()).isEqualTo(SUCCESS_STRING);
    assertThat(res.getValue()).isEqualTo("cheddar");
  }

  @Test
  public void a404WithoutAnErrorShouldTriggerAFallback() throws IOException {
    HttpHandler handler = new CannedResponses(
      req -> new HttpResponse().setStatus(HTTP_NOT_FOUND),
      req -> {
        assertThat(req.getUri()).isEqualTo("/cheese");
        return new HttpResponse()
          .setHeader("Content-Type", JSON_UTF_8)
          .setContent(Contents.asJson(ImmutableMap.of("value", "cheddar")));
      });

    CommandExecutor executor = new RetryingHandler(
      handler,
      ImmutableMap.of(
        "cheese", ImmutableList.of(
          new W3CCommand(GET, "/se/cheese", String.class),
          new W3CCommand(GET, "/cheese", String.class))));

    Response res = executor.execute(new Command(null, "cheese"));

    assertThat(res.getState()).isEqualTo(SUCCESS_STRING);
    assertThat(res.getValue()).isEqualTo("cheddar");
  }

  @Test
  public void anUnknownCommandShouldTriggerAFallback() throws IOException {
    HttpHandler handler = new CannedResponses(
      req -> new HttpResponse()
        .setStatus(HTTP_NOT_FOUND)
        .setContent(Contents.asJson(
          ImmutableMap.of("value", ImmutableMap.of(
            "error", "unknown command",
            "message", "oh noes!",
            "stacktrace", "")))),
      req -> {
        assertThat(req.getUri()).isEqualTo("/cheese");
        return new HttpResponse()
          .setHeader("Content-Type", JSON_UTF_8)
          .setContent(Contents.asJson(ImmutableMap.of("value", "cheddar")));
      });

    CommandExecutor executor = new RetryingHandler(
      handler,
      ImmutableMap.of(
        "cheese", ImmutableList.of(
          new W3CCommand(GET, "/se/cheese", String.class),
          new W3CCommand(GET, "/cheese", String.class))));

    Response res = executor.execute(new Command(null, "cheese"));

    assertThat(res.getState()).isEqualTo(SUCCESS_STRING);
    assertThat(res.getValue()).isEqualTo("cheddar");
  }

  @Test
  public void beingUnableToFindSomethingIsNotAReasonToRetryACommand() throws IOException {
    HttpHandler handler = new CannedResponses(
      req -> new HttpResponse()
        .setStatus(HTTP_NOT_FOUND)
        .setContent(Contents.asJson(
          ImmutableMap.of("value", ImmutableMap.of(
            "error", "no such element",
            "message", "oh noes!",
            "stacktrace", "")))),
      req -> { throw new AssertionError("I should not be called;"); });

    CommandExecutor executor = new RetryingHandler(
      handler,
      ImmutableMap.of(
        "find element", ImmutableList.of(new W3CCommand(GET, "/se/find", WebElement.class))));

    Response res = executor.execute(new Command(null, "find element"));

    assertThat(res.getState()).isEqualTo("no such element");
  }

  @Test
  public void letsTryThis() throws Exception {
    HttpClient client = HttpClient.Factory.createDefault().createClient(new URL("http://localhost:4444"));
    WebDriver driver = new RemoteWebDriver(
      new RetryingHandler(client, new CommonProtocol().getCommands(), new W3CProtocol().getCommands()),
      new ImmutableCapabilities("browserName", "firefox"));

    driver.get("http://www.google.com");
    driver.findElement(By.name("q"));
  }

  private static class CannedResponses implements HttpHandler {

    private final Iterator<Function<HttpRequest, HttpResponse>> responses;

    public CannedResponses(Function<HttpRequest, HttpResponse>... responses) {
      this.responses = Arrays.asList(responses).iterator();
    }

    @Override
    public HttpResponse execute(HttpRequest req) throws UncheckedIOException {
      if (!responses.hasNext()) {
        fail("No more responses");
      }

      try {
        return responses.next().apply(req);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
