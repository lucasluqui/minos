package com.lucasluqui.minos.discord.command;

import com.lucasluqui.minos.discord.DiscordManager;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public interface SlashCommand<D extends DiscordManager>
  extends Command<D, ChatInputInteractionEvent>
{

}
