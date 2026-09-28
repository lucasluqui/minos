package com.lucasluqui.minos.discord.listener;

import com.lucasluqui.minos.discord.DiscordManager;
import com.lucasluqui.minos.discord.command.SlashCommand;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class SlashCommandListener<D extends DiscordManager>
{
  public SlashCommandListener (D manager)
  {
    _discordManager = manager;
  }

  public void init ()
  {
    // empty.
  }

  public void setCommands (List<SlashCommand<D>> commands)
  {
    _commands.addAll(commands);
  }

  public Mono<Void> handle (ChatInputInteractionEvent event)
  {
    return Flux.fromIterable(_commands)
      .filter(command -> command.getName().equals(event.getCommandName()))
      .next()
      .flatMap(command -> {
        _discordManager.logCommand(event);
        return command.handle(_discordManager, event);
      });
  }

  public boolean handles (String commandName)
  {
    for (SlashCommand<D> command : _commands) {
      if (command.getName().equals(commandName)) {
        return true;
      }
    }
    return false;
  }

  /** Discord bits. */
  protected D _discordManager;

  /** List of slash command objects to associate. */
  private final List<SlashCommand<D>> _commands = new ArrayList<>();
}
