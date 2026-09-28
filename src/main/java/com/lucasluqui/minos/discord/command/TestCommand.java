package com.lucasluqui.minos.discord.command;

import com.lucasluqui.minos.discord.DiscordManager;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public class TestCommand
  implements SlashCommand<DiscordManager>
{
  @Override
  public String getName ()
  {
    return "test";
  }

  public Mono<Void> operation (DiscordManager mgr, ChatInputInteractionEvent event)
  {
    return Mono.fromSupplier(() -> "it's alive").flatMap(event::createFollowup).then();
  }
}