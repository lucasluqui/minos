package com.lucasluqui.minos.discord.embed;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.List;

public class ResultEmbed
{
  public static EmbedCreateSpec getFail (String message)
  {
    return getFail(message, null);
  }

  public static EmbedCreateSpec getFail (String message, String trace)
  {
    List<EmbedCreateFields.Field> fields = new ArrayList<>();
    if (message != null) {
      EmbedCreateFields.Field field = EmbedCreateFields.Field.of("Message", message, false);
      fields.add(field);
    }
    if (trace != null) {
      EmbedCreateFields.Field field = EmbedCreateFields.Field.of("Trace", trace, false);
      fields.add(field);
    }

    return EmbedFactory.create(
      "Fail",
      "Operation failed.",
      fields,
      null
    );
  }

  public static EmbedCreateSpec getSuccess ()
  {
    return EmbedFactory.create(
      "Success",
      "Operation completed successfully.",
      null
    );
  }
}