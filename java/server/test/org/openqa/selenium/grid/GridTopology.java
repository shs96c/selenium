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

package org.openqa.selenium.grid;

import static org.openqa.selenium.remote.http.HttpMethod.GET;

import com.google.common.collect.ImmutableList;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.grid.data.Session;
import org.openqa.selenium.grid.distributor.Distributor;
import org.openqa.selenium.grid.distributor.local.LocalDistributor;
import org.openqa.selenium.grid.distributor.remote.RemoteDistributor;
import org.openqa.selenium.grid.node.Node;
import org.openqa.selenium.grid.node.NodeStatus;
import org.openqa.selenium.grid.node.local.LocalNode;
import org.openqa.selenium.grid.node.remote.RemoteNode;
import org.openqa.selenium.grid.router.Router;
import org.openqa.selenium.grid.sessionmap.SessionMap;
import org.openqa.selenium.grid.sessionmap.local.LocalSessionMap;
import org.openqa.selenium.grid.sessionmap.remote.RemoteSessionMap;
import org.openqa.selenium.grid.web.CommandHandler;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.tracing.DistributedTracer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public class GridTopology {

  private final URL gridEntryPoint;
  private final DistributedTracer tracer;
  private final RoutingHttpClientFactory httpClientFactory;
  private final SessionMap sessions;
  private final Distributor distributor;
  private final Router router;
  private int nodeCount = 0;

  public GridTopology(
      URL gridEntryPoint,
      DistributedTracer tracer,
      RoutingHttpClientFactory httpClientFactory,
      SessionMap sessions,
      Distributor distributor,
      Router router) {
    this.gridEntryPoint = gridEntryPoint;
    this.tracer = tracer;
    this.httpClientFactory = httpClientFactory;
    this.sessions = sessions;
    this.distributor = distributor;
    this.router = router;
  }

  public static Builder builder() {
    return new Builder();
  }

  public HttpClient getHttpClient() {
    return httpClientFactory.createClient(gridEntryPoint);
  }

  private LocalNode addNode(NodeSupplier node) {
    URI localUri;
    URL localUrl;
    URL remoteUrl;
    try {
      localUri = new URI(String.format("http://local-%d", nodeCount));
      localUrl = localUri.toURL();
      remoteUrl = new URL(String.format("http://remote-%d", nodeCount));
      nodeCount++;
    } catch (MalformedURLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }

    LocalNode local = node.get(tracer, localUri, sessions);
    NodeStatus ls = local.getStatus();
    List<Capabilities> allCaps =
        Stream.concat(ls.getAvailable().entrySet().stream(), ls.getUsed().entrySet().stream())
            .flatMap(entry -> {
              List<Capabilities> caps = new LinkedList<>();
              for (int i = 0; i < entry.getValue(); i++) {
                caps.add(entry.getKey());
              }
              return caps.stream();
            })
            .collect(ImmutableList.toImmutableList());

    httpClientFactory.addRoute(localUri.getHost(), local);
    Node remote = new RemoteNode(tracer, UUID.randomUUID(), localUri, allCaps, httpClientFactory.createClient(localUrl));
    httpClientFactory.addRoute(remoteUrl.getHost(), remote);
    distributor.add(remote);

    return local;
  }

  private static class Builder {

    private final DistributedTracer tracer = DistributedTracer.builder().build();
    private SessionMapSupplier sessions;
    private DistributorSupplier distributor;

    private final List<Node> allNodes = new ArrayList<>();

    private Builder() {
      this.sessions = LocalSessionMap::new;
      this.distributor = LocalDistributor::new;
    }

    private Builder addNode(LocalNode node) {
      allNodes.add(Objects.requireNonNull(node));
      return this;
    }

    private Builder sessions(SessionMapSupplier sessions) {
      this.sessions = Objects.requireNonNull(sessions);
      return this;
    }

    public Builder distributor(DistributorSupplier distributor) {
      this.distributor = Objects.requireNonNull(distributor);
      return this;
    }

    public GridTopology buildGrid() {
      RoutingHttpClientFactory httpClientFactory = new RoutingHttpClientFactory();

      LocalSessionMap localSessions = sessions.get(tracer);
      RemoteSessionMap remoteSessions = new RemoteSessionMap(
          httpClientFactory.createClient(url("http://local-session")));
      httpClientFactory.addRoute("local-sessions", localSessions);
      httpClientFactory.addRoute("remote-sessions", remoteSessions);

      LocalDistributor localDistributor = distributor.get(tracer, httpClientFactory);
      RemoteDistributor remoteDistributor = new RemoteDistributor(
          tracer,
          httpClientFactory, url("http://local-distributor"));
      httpClientFactory.addRoute("local-distributor", localDistributor);
      httpClientFactory.addRoute("remote-distributor", remoteDistributor);

      Router router = new Router(tracer, remoteSessions, remoteDistributor);
      httpClientFactory.addRoute("router", router);

      return new GridTopology(
          url("http://router"),
          tracer,
          httpClientFactory,
          remoteSessions,
          remoteDistributor,
          router);
    }

    private URL url(String url) {
      try {
        return new URL(url);
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @FunctionalInterface
  public interface SessionMapSupplier {

    LocalSessionMap get(DistributedTracer tracer);
  }

  @FunctionalInterface
  public interface DistributorSupplier {

    LocalDistributor get(DistributedTracer tracer, HttpClient.Factory factory);
  }

  @FunctionalInterface
  public interface NodeSupplier {

    LocalNode get(DistributedTracer tracer, URI uri, SessionMap sessions);
  }

  public static void main(String[] args) throws IOException {
    GridTopology grid = GridTopology.builder()
        .buildGrid();

    HttpClient client = grid.getHttpClient();
    HttpResponse response = client.execute(new HttpRequest(GET, "/status"));

    System.out.println(response.getContentString());

    grid.addNode((trace, uri, sm) -> {
      return LocalNode.builder(trace, uri, sm)
          .add(
              new ImmutableCapabilities("browserName", "cheese"),
              caps -> new Session(new SessionId(UUID.randomUUID()), uri, caps))
          .build();
    });

    response = client.execute(new HttpRequest(GET, "/status"));
    System.out.println(response.getContentString());
  }

  private static class RoutingHttpClientFactory implements HttpClient.Factory {

    private final Map<String, CommandHandler> handlers = new HashMap<>();

    private void addRoute(String domainName, CommandHandler handler) {
      Objects.requireNonNull(domainName);
      Objects.requireNonNull(handler);

      if (handlers.containsKey(domainName)) {
        throw new RuntimeException("Domain name already exists: " + domainName);
      }
      handlers.put(domainName, handler);
    }

    @Override
    public HttpClient.Builder builder() {
      return new HttpClient.Builder() {
        @Override
        public HttpClient createClient(URL url) {
          return new HttpClient() {
            @Override
            public HttpResponse execute(HttpRequest request) throws IOException {
              HttpResponse response = new HttpResponse();
              handlers.get(url.getHost()).execute(request, response);
              return response;
            }
          };
        }
      };
    }

    @Override
    public void cleanupIdleClients() {
      // no-op
    }
  }
}
