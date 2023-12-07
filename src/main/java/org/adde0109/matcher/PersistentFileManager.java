package org.adde0109.matcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PersistentFileManager {

  private final MatcherPlugin plugin;

  private Map<ProtocolVersion, Map<UUID, RegisteredServer>> persistentPlayers = Collections.synchronizedMap(new HashMap<>());

  private boolean modified = false;

  Gson persistantGson;
  BufferedWriter writer;

  public PersistentFileManager(MatcherPlugin plugin, Path path) throws Exception {
    this.plugin = plugin;

    loadPersistentFile(path);

    writer = Files.newBufferedWriter(path);
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event, Continuation continuation) throws IOException {
    writer.flush();
  }

  private synchronized void updatePersistentFile() {
    if (!modified) {
      return;
    }
    persistantGson.toJson(persistentPlayers.entrySet().stream().map((entry) -> {
      PersistentPlayers persistentPlayers1 = new PersistentPlayers();
      persistentPlayers1.clientProtocolVersion = entry.getKey().getProtocol();
      persistentPlayers1.players = entry.getValue().entrySet().stream().map((e) -> {
        PersistentPlayers.Player player = new PersistentPlayers.Player();
        player.uuid = e.getKey().toString();
        player.preferredServer = e.getValue().toString();
        return player;
      }).toList();
      return persistentPlayers1;
    }).toArray(), writer);
    modified = false;
  }

  Optional<RegisteredServer> getPreferredServer(Player player) {
    Map<UUID, RegisteredServer> currentVersionMap = persistentPlayers.get(player.getProtocolVersion());
    if (currentVersionMap != null) {
      return Optional.ofNullable(currentVersionMap.get(player.getUniqueId()));
    }
    return Optional.empty();
  }

  void setPreferredServer(Player player, RegisteredServer server) {
    Map<UUID, RegisteredServer> currentVersionMap = persistentPlayers.computeIfAbsent(player.getProtocolVersion(), (i) -> {
      return new HashMap<>();
    });
    persistentPlayers.get(player.getProtocolVersion()).put(player.getUniqueId(), server);
    modified = true;
  }

  private void loadPersistentFile(Path path) throws Exception {

    GsonBuilder builder = new GsonBuilder();
    builder.setPrettyPrinting();

    Gson gson = builder.create();
    PersistentPlayers[] persistent = gson.fromJson(Files.newBufferedReader(path, StandardCharsets.UTF_8), PersistentPlayers[].class);

    for (PersistentPlayers persistentPlayers1: persistent) {
      Map<UUID, RegisteredServer> playerMap = new HashMap<>();
      for (PersistentPlayers.Player player: persistentPlayers1.players) {
        playerMap.put(UUID.fromString(player.uuid), plugin.server.getServer(player.preferredServer).orElseThrow());
      }
      persistentPlayers.put(ProtocolVersion.getProtocolVersion(persistentPlayers1.clientProtocolVersion), playerMap);
    }
  }



  static class PersistentPlayers {
    Integer clientProtocolVersion;
    List<Player> players;

    static class Player {
      String uuid;

      String preferredServer;
    }
  }
}
