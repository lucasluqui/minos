package com.lucasluqui.minos.discord.command;

import com.lucasluqui.minos.discord.DiscordManager;
import discord4j.core.event.domain.interaction.UserInteractionEvent;

public interface UserCommand<D extends DiscordManager>
  extends Command<D, UserInteractionEvent>
{

}
