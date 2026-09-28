package com.lucasluqui.minos.discord.command;

import com.lucasluqui.minos.discord.DiscordManager;
import com.lucasluqui.minos.discord.embed.ResultEmbed;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionFollowupCreateSpec;

import reactor.core.publisher.Mono;

public class RestartCommand
  implements SlashCommand<DiscordManager>
{
  @Override
  public String getName ()
  {
    return "restart";
  }

  public Mono<Void> operation (DiscordManager mgr, ChatInputInteractionEvent event)
  {
    return Mono.fromSupplier(() -> {
      mgr.restart();
      return ResultEmbed.getSuccess();
    }).flatMap(embed -> event.createFollowup(InteractionFollowupCreateSpec.builder()
      .addEmbed(embed)
      .build()))
      .then();
  }
}