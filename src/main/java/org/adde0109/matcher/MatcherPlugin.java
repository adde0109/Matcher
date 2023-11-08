package org.adde0109.matcher;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Plugin(id = "matcher", name = "Matcher", version = "0.0.0", authors = {"adde0109"})
public class MatcherPlugin {

  public ProxyServer server;
  public final Logger logger;
  private final Path dataDirectory;

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
      Files.createDirectories(dataDirectory);
      Path configPath = dataDirectory.resolve("Matcher.toml");
      config = MatcherConfig.read(configPath);

    } catch (Exception e) {
      logger.error("An error prevented Matcher to load correctly: "+ e.toString());
    }
  }



  @Subscribe
  public void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event, Continuation continuation) {
    String playerVersion = event.getPlayer().getProtocolVersion().toString();
    Optional<Map.Entry<String, String>> entry =
            config.getRules().entrySet().stream().filter(e -> e.getKey().equals(playerVersion)).findFirst();
    if (entry.isPresent()) {
      Optional<RegisteredServer> initServer = server.getServer(entry.get().getValue());
      if (initServer.isPresent()) {
        event.setInitialServer(initServer.get());
      }
    }

  }
}
