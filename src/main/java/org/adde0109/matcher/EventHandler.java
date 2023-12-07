package org.adde0109.matcher;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class EventHandler {

  private final MatcherPlugin plugin;

  public EventHandler(MatcherPlugin plugin) {
    this.plugin = plugin;
  }

  @Subscribe(order = PostOrder.FIRST)
  public void onPlayerChooseInitialServerEvent(PlayerChooseInitialServerEvent event, Continuation continuation) {
    Player player = event.getPlayer();
    RegisteredServer preferredServer = plugin.getPreferredServer(player).orElse(null);

    if(preferredServer != null) {
      event.setInitialServer(preferredServer);
    } else {
      List<String> toTry = plugin.server.getConfiguration().getAttemptConnectionOrder();

      Stream<RegisteredServer> matchedVersions = toTry.stream().map((name) -> plugin.server.getServer(name).
                      orElse(null)).filter(Objects::nonNull).
              filter((registeredServer -> registeredServer.getPlayersConnected().isEmpty() ||
                      registeredServer.getPlayersConnected().stream().filter((player1 ->
                              !player1.getProtocolVersion().equals(event.getPlayer().getProtocolVersion()))).toList().isEmpty()));
      event.setInitialServer(matchedVersions.findFirst().orElse(null));
    }
    continuation.resume();
  }


  @Subscribe(order = PostOrder.FIRST)
  public void onKickedFromServerEvent(KickedFromServerEvent event, Continuation continuation) {
    if (event.getResult() instanceof KickedFromServerEvent.RedirectPlayer) {
      if (event.getServerKickReason().isPresent() &&
              event.getServerKickReason().get() instanceof TranslatableComponent disconnectComponent) {
        if (disconnectComponent.key().equals("multiplayer.disconnect.incompatible")
                || disconnectComponent.key().startsWith("Outdated server!")) {
          event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                  ((KickedFromServerEvent.RedirectPlayer) event.getResult()).getServer(), Component.empty()));
        }
      }
    }
    continuation.resume();
  }
}
