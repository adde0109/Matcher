package org.adde0109.matcher;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Plugin(id = "matcher", name = "Matcher", version = "0.1.0", authors = {"adde0109"})
public class MatcherPlugin {

  ProxyServer server;
  final Logger logger;
  private final Path dataDirectory;

  PersistentFileManager persistentFileManager;

  @Inject
  public MatcherPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {

    try {
      Files.createDirectories(dataDirectory);
      Path path = dataDirectory.resolve("players.json");

      persistentFileManager = new PersistentFileManager(this, path);

      server.getEventManager().register(this, new EventHandler(this));
      server.getEventManager().register(this, persistentFileManager);

      server.getScheduler()
              .buildTask(this, () -> {
                try {
                  persistentFileManager.updatePersistentFile();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              })
              .repeat(2L, TimeUnit.SECONDS)
              .schedule();

    } catch (Exception e) {
      logger.error("An error prevented Matcher to load correctly: ", e);
      logger.warn("Matcher will be disabled!");
    }
  }

  public Optional<RegisteredServer> getPreferredServer(Player player) {
    return persistentFileManager.getPreferredServer(player);
  }

  public void setPreferredServer(Player player, RegisteredServer server) {
    persistentFileManager.setPreferredServer(player, server);
  }

}




