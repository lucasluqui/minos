package com.lucasluqui.minos.discord;

import com.lucasluqui.minos.discord.util.CommandUtil;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.lucasluqui.minos.discord.Log.log;

public class CommandRegistrar
{
  public CommandRegistrar (RestClient restClient, long guildId)
  {
    _restClient = restClient;
    _guildId = guildId;
  }

  public void registerCommands (List<String> commandFiles)
    throws IOException
  {
    if (commandFiles.isEmpty()) {
      log.warning("Got empty list of command files. Not registering any.");
      return;
    }

    // Create an ObjectMapper that supports Discord4J classes
    final JacksonResources d4jMapper = JacksonResources.create();

    // Convenience variables for the sake of easier to read code below
    final ApplicationService applicationService = _restClient.getApplicationService();
    final long applicationId = _restClient.getApplicationId().block();

    // Get our commands json from resources as command data
    List<ApplicationCommandRequest> commands = new ArrayList<>();
    for (String json : CommandUtil.getCommands(commandFiles)) {
      ApplicationCommandRequest request = d4jMapper.getObjectMapper()
        .readValue(json, ApplicationCommandRequest.class);

      // Add to our array list
      commands.add(request);
    }

    // Clears any global commands remnants.
    applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, new ArrayList<>())
      .subscribe();

    // Sets the application commands only for the main guild.
    applicationService.bulkOverwriteGuildApplicationCommand(applicationId, _guildId, commands)
      .doOnComplete(() -> log.info("Registered commands (" + commandFiles.size() + "): " +
        String.join(", ", commandFiles).replace(".json", "")
      ))
      .doOnError(e -> log.error("Failed to register commands", e))
      .subscribe();
  }

  /** Our rest client. */
  private RestClient _restClient;

  /** Main guild id. */
  private long _guildId;
}
