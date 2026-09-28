package com.lucasluqui.minos.discord.command;

import com.lucasluqui.minos.discord.DiscordManager;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;

public interface MessageCommand<D extends DiscordManager>
  extends Command<D, MessageInteractionEvent>
{

}
