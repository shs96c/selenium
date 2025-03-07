package org.openqa.selenium.grid.playwright;

import org.openqa.selenium.internal.Require;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.http.Message;
import org.openqa.selenium.remote.http.Routable;
import org.openqa.selenium.remote.http.TextMessage;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class PlaywrightHandler implements BiFunction<String, Consumer<Message>, Optional<Consumer<Message>>> {
  private static final Logger log = Logger.getLogger(PlaywrightHandler.class.getName());
  private static final Json JSON = new Json();

  private final String prefix;

  public PlaywrightHandler(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public Optional<Consumer<Message>> apply(String uri, Consumer<Message> downstream) {
    if (!uri.startsWith(prefix)) {
      return Optional.empty();
    }

    var uuid = UUID.randomUUID().toString();

    downstream.accept(new TextMessage(JSON.toJson(
      Map.of("id", 1,
          "result", Map.of(
          "playwright", Map.of("guid", "Playwright"))))));

    return Optional.of(new PlaywrightMessageHandler(downstream));
  }

  private static class PlaywrightMessageHandler implements Consumer<Message> {

    private final Consumer<Message> downstream;

    public PlaywrightMessageHandler(Consumer<Message> downstream) {
      this.downstream = Require.nonNull("Downstream connection", downstream);
    }

    @Override
    public void accept(Message message) {
      if (message instanceof TextMessage) {
        String text = ((TextMessage) message).text();
        log.info("Received text message: " + text);
      } else {
        log.warning("Received unexpected message type: " + message);
      }
    }
  }
}
