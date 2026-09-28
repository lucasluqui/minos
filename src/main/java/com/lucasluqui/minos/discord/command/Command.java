package com.lucasluqui.minos.discord.command;

import com.lucasluqui.minos.discord.DiscordManager;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import reactor.core.publisher.Mono;

/**
 * A simple interface defining our command class contract.
 * a getName() method to provide the case-sensitive name of the command.
 * and a handle() method which will hook into operation() for all the logic for processing each command.
 */
public interface Command<D extends DiscordManager, T extends ApplicationCommandInteractionEvent>
{
  String getName();

  default Mono<Void> handle (D mgr, T event)
  {
    return event.deferReply().then(Mono.defer(() -> operation(mgr, event)));
  }

  Mono<Void> operation (D mgr, T event);
}
