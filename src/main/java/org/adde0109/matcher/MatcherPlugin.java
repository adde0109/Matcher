package org.adde0109.matcher;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(id = "matcher", name = "Matcher", version = "0.0.0", authors = {"adde0109"})
public class MatcherPlugin {

  public ProxyServer server;
  public final Logger logger;
  private final Path dataDirectory;

  private ServerScout scout;

  public MatcherConfig config;
  @Inject
  public MatcherPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe(order = PostOrder.LAST)
  public void onProxyInitialization(ProxyInitializeEvent event) {

    try {
      /*
      Files.createDirectories(dataDirectory);
      Path configPath = dataDirectory.resolve("Matcher.toml");
      config = MatcherConfig.read(configPath);
      */
      this.scout = new ServerScout(server.getConfiguration().getAttemptConnectionOrder());

    } catch (Exception e) {
      logger.error("An error prevented Matcher to load correctly: "+ e.toString());
    }
  }



  @Subscribe
  public void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event, Continuation continuation) {
    scout.getServerByVersion(event.getPlayer().getProtocolVersion()).thenAccept((r) -> {
      event.setInitialServer(r.get(0));
      continuation.resume();
    });
  }

  @Subscribe
  public void onKickedFromServerEvent(KickedFromServerEvent event, Continuation continuation) {
    if (event.getServerKickReason().isPresent() &&
            event.getServerKickReason().get() instanceof TranslatableComponent disconnectComponent) {
      
    }
    scout.getServerByVersion(event.getPlayer().getProtocolVersion()).thenAccept((r) -> {
      int kickedServerIndex = r.indexOf(event.getServer());
      if (kickedServerIndex > r.size()-1) {
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                r.get(kickedServerIndex), event.getServerKickReason().orElse(Component.empty())));
      } else {
        event.setResult(KickedFromServerEvent.DisconnectPlayer.create(
                event.getServerKickReason().orElse(Component.empty())));
      }
      continuation.resume();
    });
  }


  private class ServerScout {
    private final List<RegisteredServer> serversToTry = new ArrayList<>();

    Map<ProtocolVersion, CompletableFuture<Map<RegisteredServer, ServerPing>>> discoveredServers =
            new ConcurrentHashMap<>();


    ServerScout(List<String> serverList) {
      for (String serverName: serverList) {
        Optional<RegisteredServer> existingServer = server.getServer(serverName);
          existingServer.ifPresent(registeredServer -> serversToTry.add(registeredServer));
      }
    }

    private CompletableFuture<Map<RegisteredServer, ServerPing>> startDiscovery(ProtocolVersion pingVersion) {
      Map<RegisteredServer, CompletableFuture<ServerPing>> scanFutures = new LinkedHashMap<>();
      CompletableFuture<Map<RegisteredServer, ServerPing>> discoveryFuture = new CompletableFuture<>();
      for (RegisteredServer serverToTry : serversToTry) {
        scanFutures.put(serverToTry, serverToTry.ping(PingOptions.builder().version(pingVersion).build()).
                exceptionally((ex) -> {
          return null;
        }));
      }
      CompletableFuture.allOf(scanFutures.values().toArray(new CompletableFuture[0])).thenAccept((ignored) -> {
        Map<RegisteredServer, ServerPing> discovered = new LinkedHashMap<>();
        scanFutures.forEach((k,v) -> {
          discovered.put(k, v.getNow(null));
        });
        discoveryFuture.complete(discovered);
      });
      return discoveryFuture;
    }

    public CompletableFuture<List<RegisteredServer>> getServerByVersion(ProtocolVersion version) {
      CompletableFuture<List<RegisteredServer>> future = new CompletableFuture<>();
      discoveredServers.computeIfAbsent(version, this::startDiscovery).thenAccept(result -> {
          result.entrySet().removeIf((entry -> {
            return entry.getValue().getVersion().getProtocol() == version.getProtocol();
          }));
          future.complete(result.keySet().stream().toList());
      });
      return future;
    }


  }
}
