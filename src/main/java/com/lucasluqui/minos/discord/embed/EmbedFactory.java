package com.lucasluqui.minos.discord.embed;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.util.List;

public class EmbedFactory
{
  private static final String EMBED_DEFAULT_COLOR = "#000000";

  public static EmbedCreateSpec create (
    String title,
    String description,
    String color)
  {
    if (color == null) color = EMBED_DEFAULT_COLOR;
    return EmbedCreateSpec.builder()
      .title(title)
      .description(description)
      .color(Color.of(color))
      .build();
  }

  public static EmbedCreateSpec create (
    String title,
    String description,
    List<EmbedCreateFields.Field> fields,
    String color)
  {
    if (color == null) color = EMBED_DEFAULT_COLOR;
    return EmbedCreateSpec.builder()
      .title(title)
      .description(description)
      .addAllFields(fields)
      .color(Color.of(color))
      .build();
  }

  public static EmbedCreateSpec create (
    String title,
    String description,
    List<EmbedCreateFields.Field> fields,
    String color,
    String footer)
  {
    if (color == null) color = EMBED_DEFAULT_COLOR;
    return EmbedCreateSpec.builder()
      .title(title)
      .description(description)
      .addAllFields(fields)
      .color(Color.of(color))
      .footer(EmbedCreateFields.Footer.of(footer, null))
      .build();
  }

  public static EmbedCreateSpec create (
    String title,
    String description,
    List<EmbedCreateFields.Field> fields,
    String image,
    String color,
    String footer)
  {
    if (color == null) color = EMBED_DEFAULT_COLOR;
    if (footer == null) footer = "";
    return EmbedCreateSpec.builder()
      .title(title)
      .description(description)
      .addAllFields(fields)
      .image(image)
      .color(Color.of(color))
      .footer(EmbedCreateFields.Footer.of(footer, null))
      .build();
  }

  public static EmbedCreateSpec create (
    String title,
    String description,
    String thumbnail,
    String color,
    String footer)
  {
    if (color == null) color = EMBED_DEFAULT_COLOR;

    return EmbedCreateSpec.builder()
      .title(title)
      .description(description)
      .thumbnail(thumbnail)
      .color(Color.of(color))
      .footer(EmbedCreateFields.Footer.of(footer, null))
      .build();
  }
}
