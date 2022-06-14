package technobot.commands.suggestions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import technobot.TechnoBot;
import technobot.commands.Category;
import technobot.commands.Command;
import technobot.data.GuildData;
import technobot.handlers.SuggestionHandler;
import technobot.listeners.ButtonListener;
import technobot.util.embeds.EmbedUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Admin command to setup and modify the suggestion board.
 *
 * @author TechnoVision
 */
public class SuggestionsCommand extends Command {

    public SuggestionsCommand(TechnoBot bot) {
        super(bot);
        this.name = "suggestions";
        this.description = "Setup and modify the suggestions config.";
        this.category = Category.SUGGESTIONS;
        this.permission = Permission.MANAGE_SERVER;
        this.subCommands.add(new SubcommandData("create", "Sets a channel to become the suggestion board.")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "The channel to set as the suggestion board")
                        .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)));
        this.subCommands.add(new SubcommandData("dm", "Toggle private messages on suggestion response."));
        this.subCommands.add(new SubcommandData("anonymous", "Toggle anonymous mode for suggestions."));
        this.subCommands.add(new SubcommandData("config", "Display the current suggestions config."));
        this.subCommands.add(new SubcommandData("reset", "Reset all suggestion board data and settings."));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Guild guild = event.getGuild();
        SuggestionHandler suggestionHandler = GuildData.get(guild).suggestionHandler;

        String text = null;
        switch(event.getSubcommandName()) {
            case "create" -> {
                // Setup suggestion board
                OptionMapping channelOption = event.getOption("channel");
                if (channelOption == null) {
                    // Create new suggestion channel
                    guild.createTextChannel("suggestions").queue(channel -> {
                        ArrayList<Permission> denyPerms = new ArrayList<>();
                        denyPerms.add(Permission.MESSAGE_ADD_REACTION);
                        denyPerms.add(Permission.MESSAGE_SEND);

                        ArrayList<Permission> allowPerms = new ArrayList<>();
                        allowPerms.add(Permission.VIEW_CHANNEL);
                        allowPerms.add(Permission.MESSAGE_HISTORY);

                        channel.upsertPermissionOverride(guild.getPublicRole()).deny(denyPerms).setAllowed(allowPerms).queue();
                        suggestionHandler.setChannel(channel.getIdLong());
                    });
                    text = EmbedUtils.BLUE_TICK + " Created a new suggestion channel!";
                } else {
                    // Set suggestion board to mentioned channel
                    try {
                        long channel = channelOption.getAsGuildChannel().getIdLong();
                        String channelMention = "<#" + channel + ">";
                        suggestionHandler.setChannel(channel);
                        text = EmbedUtils.BLUE_TICK + " Set the suggestion channel to " + channelMention;
                    } catch (NullPointerException e) {
                        text = "You can only set a text channel as the suggestion board!";
                        event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
                        return;
                    }
                }
            }
            case "dm" -> {
                boolean isEnabled = suggestionHandler.toggleResponseDM();
                if (isEnabled) {
                    text = EmbedUtils.BLUE_TICK + " Response DMs have been **enabled** for suggestions!";
                } else {
                    text = EmbedUtils.BLUE_X + " Response DMs have been **disabled** for suggestions!";
                }
            }
            case "anonymous" -> {
                boolean isEnabled = suggestionHandler.toggleAnonymous();
                if (isEnabled) {
                    text = EmbedUtils.BLUE_TICK + " Anonymous mode has been **enabled** for suggestions!";
                } else {
                    text = EmbedUtils.BLUE_X + " Anonymous mode has been **disabled** for suggestions!";
                }
            }
            case "config" -> {
                text = "";
                if (suggestionHandler.getChannel() != null) {
                    text += "\n**Channel:** <#" + suggestionHandler.getChannel() + ">";
                } else {
                    text += "\n**Channel:** none";
                }
                text += "\n**DM on Response:** " + suggestionHandler.hasResponseDM();
                text += "\n**Anonymous Mode:** " + suggestionHandler.isAnonymous();
                event.getHook().sendMessage(text).queue();
                return;
            }
            case "reset" -> {
                long userID = event.getUser().getIdLong();
                String uuid = userID + ":" + UUID.randomUUID();
                List<Button> components = ButtonListener.getResetButtons(uuid, "Suggestion");
                ButtonListener.buttons.put(uuid, components);
                text = "Would you like to reset the suggestions system?\nThis will delete **ALL** data!";
                event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).addActionRow(components).queue(interactionHook -> {
                    // Timer task to disable buttons and clear cache after 3 minutes
                    Runnable task = () -> {
                        List<Button> actionRow = ButtonListener.buttons.get(uuid);
                        for (int i = 0; i < actionRow.size(); i++) {
                            actionRow.set(i, actionRow.get(i).asDisabled());
                        }
                        interactionHook.editMessageComponents(ActionRow.of(actionRow)).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                        ButtonListener.buttons.remove(uuid);
                    };
                    ButtonListener.executor.schedule(task, 3, TimeUnit.MINUTES);
                });
                return;
            }
        }
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
