package com.lucasluqui.minos.discord;

import com.lucasluqui.minos.discord.command.RestartCommand;
import com.lucasluqui.minos.discord.command.SlashCommand;
import com.lucasluqui.minos.discord.command.TestCommand;
import com.lucasluqui.minos.discord.listener.SlashCommandListener;
import com.lucasluqui.minos.discord.util.CommandUtil;

import com.google.inject.Singleton;

import discord4j.common.JacksonResources;
import discord4j.common.ReactorResources;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.guild.*;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.spec.*;
import discord4j.discordjson.json.MessageReferenceData;
import discord4j.discordjson.possible.Possible;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.http.ExchangeStrategies;
import discord4j.rest.request.BucketGlobalRateLimiter;
import discord4j.rest.request.DefaultRouter;
import discord4j.rest.request.RequestQueueFactory;
import discord4j.rest.request.RouterOptions;
import discord4j.rest.route.Routes;
import discord4j.rest.service.WebhookService;
import discord4j.rest.util.Image;

import io.netty.resolver.dns.DnsNameResolverTimeoutException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.lucasluqui.minos.discord.Log.log;

@Singleton
public abstract class DiscordManager
{
  public DiscordManager ()
  {
    // empty.
  }

  protected void init ()
  {
    new Thread(this::login).start();
  }

  protected void login ()
  {
    _client = DiscordClientBuilder.create(_token)
      .build();

    _gateway = _client.gateway()
      .setAwaitConnections(true)
      .setEnabledIntents(_intents)
      .login()
      // Apply retry logic specifically targeting network/DNS issues
      .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
        .maxBackoff(Duration.ofSeconds(30))
        .filter(t -> t instanceof UnknownHostException || t instanceof DnsNameResolverTimeoutException)
        .doBeforeRetry(retrySignal -> System.err.println("Network error while attempting to log into gateway (attempt " +
          (retrySignal.totalRetries() + 1) + "). Retrying..."))
      )
      .block();

    setupDispatcher();

    _cmdReg = new CommandRegistrar(_gateway.getRestClient(), _guildId);
    setupCommands();

    try {
      _cmdReg.registerCommands(CommandUtil.getCommandsDefs());
    } catch (IOException | URISyntaxException e) {
      log.error("Error while trying to register commands", e);
    }

    _gateway
      .on(ChatInputInteractionEvent.class, this::handleCommand)
      .then(_gateway.onDisconnect())
      .block();

    log.info("Discord gateway client completed logon");
  }

  protected void setupDispatcher ()
  {
    // setup discord evt dispatcher.
    EventDispatcher dispatcher = _gateway.getEventDispatcher();

    // when we're connected to the gateway.
    dispatcher.on(ReadyEvent.class)
      .flatMap(_ -> Mono.fromRunnable(this::didLogon))
      .onErrorContinue((e, _) -> log.error(e))
      .subscribe();

    // when a server event *is deleted*, which is not the same as ending.
    dispatcher.on(ScheduledEventDeleteEvent.class)
      .flatMap(event -> Mono.fromRunnable(() -> this.scheduledEventDeleted(event.getScheduledEvent()))
        .onErrorContinue((e, _) -> log.error(e)))
      .subscribe();

    // when a server event is updated, which we use as "ending" rather.
    // since they're rarely (if ever!) updated...
    dispatcher.on(ScheduledEventUpdateEvent.class)
      .flatMap(event -> Mono.fromRunnable(() -> this.scheduledEventUpdated(event.getCurrent()))
        .onErrorContinue((e, _) -> log.error(e)))
      .subscribe();

    // when a message is sent.
    dispatcher.on(MessageCreateEvent.class)
      .doOnNext(this::onMessage)
      .subscribe();

    // when a new member joins the guild.
    dispatcher.on(MemberJoinEvent.class)
      .flatMap(event -> Mono.fromRunnable(() -> this.onMemberJoin(event.getMember()))
        .onErrorContinue((e, _) -> log.error(e)))
      .subscribe();

    // when a new member joins the guild.
    dispatcher.on(MemberLeaveEvent.class)
      .flatMap(event -> Mono.fromRunnable(() -> this.onMemberLeave(
        event.getMember().isPresent() ? event.getMember().get() : null))
        .onErrorContinue((e, _) -> log.error(e)))
      .subscribe();

    // when a guild member is updated. for example - their roles change.
    dispatcher.on(MemberUpdateEvent.class)
      .flatMap(event -> Mono.fromRunnable(() -> this.onMemberUpdate(event))
        .onErrorContinue((e, _) -> log.error(e)))
      .subscribe();
  }

  private void setupCommands ()
  {
    _slashCmdListener = new SlashCommandListener<>(this);
    List<SlashCommand<DiscordManager>> commands = new ArrayList<>();

    commands.add(new TestCommand());
    commands.add(new RestartCommand());

    _slashCmdListener.setCommands(commands);
  }

  protected void didLogon ()
  {
    // empty.
  }

  protected boolean isDefaultCommand (String commandName)
  {
    return _slashCmdListener.handles(commandName);
  }

  protected Mono<Void> handleCommand (ChatInputInteractionEvent event)
  {
    if (_slashCmdListener != null) {
      return _slashCmdListener.handle(event);
    }
    log.warning("Called for default command handling, yet can't handle...");
    return null;
  }

  public void restart ()
  {
    _gateway.logout().block();
    init();

    log.info("Restarted Discord gateway client");
  }

  protected void onMessage (MessageCreateEvent event)
  {
    // empty.
  }

  protected void onMemberJoin (Member member)
  {
    // empty.
  }

  protected void onMemberLeave (Member member)
  {
    if (member == null) {
      log.warning("Tried to handle member leave event but we got a null member");
      return;
    }
    // empty.
  }

  protected void onMemberUpdate (MemberUpdateEvent event)
  {
    // empty.
  }

  public void sendMessage (String content, long channelId)
  {
    sendMessage(content, null, channelId);
  }

  public void sendMessage (EmbedCreateSpec embed, long channelId)
  {
    sendMessage(null, embed, channelId);
  }

  public void sendMessage (String content, EmbedCreateSpec embed, long channelId)
  {
    if (content != null && embed != null) {
      _gateway.getChannelById(Snowflake.of(channelId))
        .flatMap(channel -> channel.getRestChannel().createMessage(
          MessageCreateSpec.builder()
            .content(content)
            .addEmbed(embed)
            .build()
            .asRequest()
        ))
        .subscribe();
      return;
    }

    if (embed != null) {
      _gateway.getChannelById(Snowflake.of(channelId))
        .flatMap(channel -> channel.getRestChannel().createMessage(embed.asRequest()))
        .subscribe();
      return;
    }

    _gateway.getChannelById(Snowflake.of(channelId))
      .flatMap(channel -> channel.getRestChannel().createMessage(content))
      .subscribe();
  }

  public void ban (Snowflake userId, String reason, int deleteMessageDays)
  {
    Guild guild = _gateway.getGuildById(Snowflake.of(_guildId)).block();

    guild.ban(userId, BanQuerySpec.builder()
      .reason(reason)
      .deleteMessageSeconds(deleteMessageDays * 24 * 60 * 60)
      .build()
    ).subscribe();
  }

  public void kick (Snowflake userId, String reason)
  {
    Guild guild = _gateway.getGuildById(Snowflake.of(_guildId)).block();
    guild.kick(userId, reason).subscribe();
  }

  public void timeout (long snowflake, String reason)
  {
    timeout(snowflake, Duration.ofDays(27), reason);
  }

  public void timeout (long snowflake, Duration duration, String reason)
  {
    Instant communicationDisabledUntil = Instant.now().plus(duration);
    GuildMemberEditSpec editSpec = GuildMemberEditSpec.builder()
      .communicationDisabledUntilOrNull(communicationDisabledUntil)
      .build().withReason(reason);

    _gateway.getGuildById(Snowflake.of(_guildId))
      .flatMap(guild -> guild.getMemberById(Snowflake.of(snowflake)))
      .flatMap(member -> member.edit(editSpec))
      .onErrorContinue((t, _) -> log.error("Failed to edit user timeout", t))
      .subscribe();
  }

  public void liftTimeout (long snowflake)
  {
    GuildMemberEditSpec editSpec = GuildMemberEditSpec.builder()
      .communicationDisabledUntilOrNull(null)
      .build().withReason("Timeout lifted.");

    _gateway.getGuildById(Snowflake.of(_guildId))
      .flatMap(guild -> guild.getMemberById(Snowflake.of(snowflake)))
      .flatMap(member -> member.edit(editSpec))
      .onErrorContinue((t, _) -> log.error("Failed to edit user timeout", t))
      .subscribe();
  }

  public void replyMessage (Message message, String content)
  {
    message.getChannel()
      .flatMap(channel -> channel.createMessage(
        MessageCreateSpec.builder()
          .content(content)
          .messageReference(
            MessageReferenceData.builder()
              .messageId(message.getId().asLong())
              .build())
          .build()
      ))
      .subscribe();
  }

  public void sendWebhookMessage (long id, String token, String content, EmbedCreateSpec embed, boolean publish)
  {
    Message message = _gateway.getWebhookByIdWithToken(Snowflake.of(id), token)
      .flatMap(webhook -> {
        WebhookExecuteMono mono = webhook.execute();
        if (content != null) {
          mono = mono.withContent(content);
        }
        if (embed != null) {
          mono = mono.withEmbeds(embed);
        }
        mono = mono.withWaitForMessage(true);
        return mono;
      })
      .block();

    if (publish) {
      message.publish().subscribe();
    }
  }

  public void editWebhookMessage (long id, String token, long messageId, String newContent, EmbedCreateSpec newEmbed)
  {
    _gateway.getWebhookByIdWithToken(Snowflake.of(id), token)
      .flatMap(webhook -> {
        if (newEmbed == null) {
          return webhook.editMessage(Snowflake.of(messageId))
            .withContent(newContent);
        } else if (newContent == null) {
          return webhook.editMessage(Snowflake.of(messageId))
            .withEmbeds(newEmbed);
        } else {
          return webhook.editMessage(Snowflake.of(messageId))
            .withContent(newContent)
            .withEmbeds(newEmbed);
        }
      })
      .block();
  }

  @Deprecated
  public void editExternalWebhookMessage (long id, String token, long messageId, String newContent, EmbedCreateSpec newEmbed)
  {
    WebhookService service = new WebhookService(new DefaultRouter(new RouterOptions("", ReactorResources.create(),
      ExchangeStrategies.jackson(JacksonResources.create().getObjectMapper()),
      Collections.emptyList(), BucketGlobalRateLimiter.create(), RequestQueueFactory.buffering(),
      Routes.BASE_URL)));

    if (newEmbed == null) {
      service.modifyWebhookMessage(id, token, String.valueOf(messageId), WebhookMessageEditSpec.builder()
        .content(newContent)
        .build()
        .asRequest()
      ).block();
    } else if (newContent == null) {
      service.modifyWebhookMessage(id, token, String.valueOf(messageId), WebhookMessageEditSpec.builder()
        .addEmbeds(newEmbed)
        .build()
        .asRequest()
      ).block();
    } else {
      service.modifyWebhookMessage(id, token, String.valueOf(messageId), WebhookMessageEditSpec.builder()
        .content(newContent)
        .addEmbeds(newEmbed)
        .build()
        .asRequest()
      ).block();
    }
  }

  public void sendExternalWebhookMessage (long id, String token, String content, EmbedCreateSpec embed)
  {
    WebhookService service = new WebhookService(new DefaultRouter(new RouterOptions("", ReactorResources.create(),
      ExchangeStrategies.jackson(JacksonResources.create().getObjectMapper()),
      Collections.emptyList(), BucketGlobalRateLimiter.create(), RequestQueueFactory.buffering(),
      Routes.BASE_URL)));

    if (embed == null) {
      service.executeWebhook(id, token, true, WebhookExecuteSpec.builder()
        .content(content)
        .build()
        .asRequest()
      ).block();
    } else if (content == null) {
      service.executeWebhook(id, token, true, WebhookExecuteSpec.builder()
        .addEmbeds(embed)
        .build()
        .asRequest()
      ).block();
    } else {
      service.executeWebhook(id, token, true, WebhookExecuteSpec.builder()
        .content(content)
        .addEmbeds(embed)
        .build()
        .asRequest()
      ).block();
    }
  }

  public void sendDirectMessage (String id, String message)
  {
    sendDirectMessage(Snowflake.of(id), message, false);
  }

  public void sendDirectMessage (long id, String message)
  {
    sendDirectMessage(Snowflake.of(id), message, false);
  }

  public void sendDirectMessage (String id, String message, boolean block)
  {
    sendDirectMessage(Snowflake.of(id), message, block);
  }

  public void sendDirectMessage (long id, String message, boolean block)
  {
    sendDirectMessage(Snowflake.of(id), message, block);
  }

  public void sendDirectMessage (Snowflake id, String message, boolean block)
  {
    if (block) {
      _gateway.getUserById(id)
        .flatMap(User::getPrivateChannel)
        .flatMap(channel -> channel.createMessage(message))
        .doOnSuccess(_ -> log.info("Sent DM to user", "id", id.asString(), "message", message))
        .doOnError(t -> log.info("Failed to send DM to user", "id", id.asString(), "message", message, "error", t))
        .block();
      return;
    }

    _gateway.getUserById(id)
      .flatMap(User::getPrivateChannel)
      .flatMap(channel -> channel.createMessage(message))
      .doOnSuccess(_ -> log.info("Sent DM to user", "id", id.asString(), "message", message))
      .doOnError(t -> log.info("Failed to send DM to user", "id", id.asString(), "message", message, "error", t))
      .subscribe();
  }

  public void sendDirectMessage (User user, String message, boolean block)
  {
    if (block) {
      user.getPrivateChannel()
        .flatMap(channel -> channel.createMessage(message))
        .doOnSuccess(_ -> log.info("Sent DM to user", "id", user.getId().asString(), "message", message))
        .doOnError(t -> log.info("Failed to send DM to user", "id", user.getId().asString(), "message", message, "error", t))
        .block();
      return;
    }

    user.getPrivateChannel()
      .flatMap(channel -> channel.createMessage(message))
      .doOnSuccess(_ -> log.info("Sent DM to user", "id", user.getId().asString(), "message", message))
      .doOnError(t -> log.info("Failed to send DM to user", "id", user.getId().asString(), "message", message, "error", t))
      .subscribe();
  }

  protected void scheduledEventUpdated (ScheduledEvent scheduledEvent)
  {
    log.info("Discord scheduled event updated",
      "event", scheduledEvent.getName() + "(" + scheduledEvent.getId().asLong() + ")"
    );

    if (scheduledEvent.getEndTime().get().isBefore(Instant.now())) {
      this.scheduledEventEnded(scheduledEvent);
    }
  }

  protected void scheduledEventDeleted (ScheduledEvent scheduledEvent)
  {
    log.info("Discord scheduled event deleted",
      "event", scheduledEvent.getName() + "(" + scheduledEvent.getId().asLong() + ")"
    );

    this.scheduledEventEnded(scheduledEvent);
  }

  protected void scheduledEventEnded (ScheduledEvent scheduledEvent)
  {
    log.info("Discord scheduled event ended",
      "event", scheduledEvent.getName() + "(" + scheduledEvent.getId().asLong() + ")"
    );
  }

  public void setServerIcon (String url)
  {
    GuildEditSpec editSpec = GuildEditSpec.builder()
      .icon(Possible.of(Optional.of(Image.ofUrl(url != null ? url : _defaultServerIcon).block())))
      .build();

    _gateway.getGuildById(Snowflake.of(_guildId))
      .flatMap(guild -> guild.edit(editSpec))
      .doOnError(e -> log.error("Failed to edit server icon", e))
      .block();
  }

  public void setServerBanner (String url)
  {
    Possible<Optional<Image>> image = Possible.of(Optional.of(Image.ofUrl(url != null ? url : _defaultServerBanner).block()));
    GuildEditSpec editSpec = GuildEditSpec.builder()
      .banner(image) // shown topmost of channel list
      .discoverySplash(image) // shown on invite link embeds and discovery searches
      .splash(image) // shown on server invite links
      .build();

    _gateway.getGuildById(Snowflake.of(_guildId))
      .flatMap(guild -> guild.edit(editSpec))
      .doOnError(e -> log.error("Failed to edit server banner", e))
      .block();
  }

  public void logCommand (ChatInputInteractionEvent event)
  {
    String options = event.getOptions().isEmpty() ? "none" : event.getOptions().get(0).getName();
    log.info("Discord command ran",
      "command", event.getCommandName(),
      "subcommand", options,
      "who", event.getUser().getUsername() + " (" + event.getUser().getId().asLong() + ")"
    );
  }

  /** Discord's client. */
  protected DiscordClient _client;

  /** Discord's gateway. */
  protected GatewayDiscordClient _gateway;

  /** For registering command bits. */
  protected CommandRegistrar _cmdReg;

  /** Command listener. */
  private SlashCommandListener<DiscordManager> _slashCmdListener;

  /** Bot token. */
  protected String _token;

  /** ID of the main guild. */
  protected long _guildId;

  /** Discord gateway intents. */
  protected IntentSet _intents;

  /** Default guild icon image url. */
  protected String _defaultServerIcon;

  /** Default guild banner image url. */
  protected String _defaultServerBanner;
}