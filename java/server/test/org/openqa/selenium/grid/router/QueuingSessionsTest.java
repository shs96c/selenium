package org.openqa.selenium.grid.router;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.grid.config.TomlConfig;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.testing.drivers.Browser;

import java.io.StringReader;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.json.Json.MAP_TYPE;
import static org.openqa.selenium.remote.http.HttpMethod.POST;

public class QueuingSessionsTest {

  @Test
  public void shouldAllowASessionToBeQueued() throws InterruptedException {
    Browser browser = Browser.detect();

    DeploymentTypes.Deployment deployment = DeploymentTypes.DISTRIBUTED.start(
      browser.getCapabilities(),
      new TomlConfig(new StringReader(
        "[node]\n" +
          "detect-drivers = true\n" +
          "drivers = " + browser.displayName() + "\n" +
          "max-sessions = 2")));

    WebDriver driver = RemoteWebDriver.builder()
      .addAlternative(browser.getCapabilities())
      .address(deployment.getServer().getUrl())
      .build();

    HttpClient client = HttpClient.Factory.createDefault().createClient(deployment.getServer().getUrl());
    HttpResponse res = client.execute(
      new HttpRequest(POST, "/graphql")
        .setContent(Contents.string("{\"query\": \"{grid { totalSlots maxSession } }\"}", UTF_8)));
    Map<?,?> out = Contents.fromJson(res, MAP_TYPE);
    out = (Map<?, ?>) out.get("data");
    out = (Map<?, ?>) out.get("grid");
    int max = Math.min(((Number) out.get("totalSlots")).intValue(), ((Number) out.get("maxSession")).intValue());

    assertThat(max).isGreaterThan(0);

    for (int i = 1; i < max; i++) {
      RemoteWebDriver.builder()
        .addAlternative(browser.getCapabilities())
        .address(deployment.getServer().getUrl())
        .build();
    }

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<WebDriver> other = new AtomicReference<>();
    new Thread(() -> {
      other.set(RemoteWebDriver.builder()
        .addAlternative(browser.getCapabilities())
        .address(deployment.getServer().getUrl())
        .build());
    }).start();

    new FluentWait<>("").withTimeout(Duration.ofSeconds(5)).until(ignored -> {
      HttpResponse queryRes = client.execute(
        new HttpRequest(POST, "/graphql")
          .setContent(Contents.string("{\"query\": \"{grid { sessionQueueSize } }\"}", UTF_8)));
      Map<?,?> query = Contents.fromJson(queryRes, MAP_TYPE);
      query = (Map<?, ?>) query.get("data");
      query = (Map<?, ?>) query.get("grid");
      Number size = (Number) query.get("sessionQueueSize");
      return size.intValue() > 0;
    });
    driver.quit();

    assertThat(latch.await(20, SECONDS)).isTrue();
    other.get().quit();
  }

}
