package com.lucasluqui.minos.discord.util;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;

import java.util.Optional;

public class OptionUtil
{
  public static String getString (Optional<ApplicationCommandInteractionOption> opt)
  {
    if (opt.flatMap(ApplicationCommandInteractionOption::getValue)
      .map(ApplicationCommandInteractionOptionValue::asString)
      .isEmpty()) {
      return "";
    } else {
      return opt.flatMap(ApplicationCommandInteractionOption::getValue)
        .map(ApplicationCommandInteractionOptionValue::asString)
        .get();
    }
  }

  public static Integer getInteger (Optional<ApplicationCommandInteractionOption> opt)
  {
    if (getLong(opt) == null) {
      return 0;
    }
    return getLong(opt).intValue();
  }

  public static Long getLong (Optional<ApplicationCommandInteractionOption> opt)
  {
    if (opt.flatMap(ApplicationCommandInteractionOption::getValue)
      .map(ApplicationCommandInteractionOptionValue::asLong)
      .isEmpty()) {
      return 0L;
    } else {
      return opt.flatMap(ApplicationCommandInteractionOption::getValue)
        .map(ApplicationCommandInteractionOptionValue::asLong)
        .get();
    }
  }

  public static Boolean getBoolean (Optional<ApplicationCommandInteractionOption> opt, boolean defVal)
  {
    if (opt.flatMap(ApplicationCommandInteractionOption::getValue)
      .map(ApplicationCommandInteractionOptionValue::asBoolean)
      .isEmpty()) {
      return defVal;
    } else {
      return opt.flatMap(ApplicationCommandInteractionOption::getValue)
        .map(ApplicationCommandInteractionOptionValue::asBoolean)
        .get();
    }
  }
}
