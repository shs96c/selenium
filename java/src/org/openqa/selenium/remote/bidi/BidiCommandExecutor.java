package org.openqa.selenium.remote.bidi;

import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.bidi.Connection;
import org.openqa.selenium.bidi.browser.ClientWindowInfo;
import org.openqa.selenium.bidi.browser.GetClientWindowsResult;
import org.openqa.selenium.bidi.browser.GetUserContextsResult;
import org.openqa.selenium.bidi.browsingcontext.NavigationResult;
import org.openqa.selenium.bidi.browsingcontext.ReadinessState;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

public class BidiCommandExecutor implements CommandExecutor {

    private static final String CONTEXT = "context";

    private final Connection connection;
    private final SessionId sessionId;
    private Duration pageLoadTimeout = Duration.ofMillis(300_000);
    private String currentContextId;
    private ReadinessState navigationReadinessState = ReadinessState.COMPLETE;

    public BidiCommandExecutor(HttpClient client, URI bidiEndpoint, SessionId sessionId) {
        Require.nonNull("Client", client);
        Require.nonNull("URI", bidiEndpoint);

        this.connection = new Connection(client, bidiEndpoint.toString());
        this.sessionId = sessionId;

        GetUserContextsResult allContexts = connection.sendAndWait(
                new org.openqa.selenium.bidi.Command<>(
                        "browser.getUserContexts",
                        Map.of(),
                        GetUserContextsResult.class),
                pageLoadTimeout);

        if (allContexts.getUserContexts().isEmpty()) {
            throw new IllegalStateException("No windows found. Unable to select default context");
        }
        currentContextId = allContexts.getUserContexts().iterator().next().
    }

    @Override
    public Response execute(Command command) throws IOException {
        Require.nonNull("Command", command);

        switch (command.getName()) {
            case DriverCommand.GET:
                String url = (String) command.getParameters().get("url");
                NavigationResult result = connection.sendAndWait(
                        new org.openqa.selenium.bidi.Command<>(
                                "browsingContext.navigate",
                                Map.of(CONTEXT, currentContextId, "url", url, "wait", navigationReadinessState.toString()),
                                NavigationResult.class),
                        pageLoadTimeout);
                Response response = new Response(sessionId);
                response.setState("success");
                return response;

            default:
                throw new UnsupportedCommandException(command.getName());
        }
    }
}
