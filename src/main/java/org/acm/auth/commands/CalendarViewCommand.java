package org.acm.auth.commands;

import com.google.api.services.calendar.Calendar;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.acm.auth.services.GoogleCalendar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;

/**
 * Represents a command that displays the description of the calendar that corresponds to the guild
 * from which was invoked.
 */
public class CalendarViewCommand extends Command{
    private static final Logger LOGGER = LogManager.getLogger(CalendarViewCommand.class);
    private static final String CALENDAR_ID = "tln0hmfdrimk3e3j3ngaj7vvag@group.calendar.google.com";

    /**
     * Constructs a CalendarView command
     */
    public CalendarViewCommand() {
        super(
            "calview",
                "Displays the description of the calendar that corresponds to the guild from " +
                    "which was invoked.",
            false,
            new String[] {"cview"});
    }

    @Override
    public void invoke(MessageReceivedEvent event, String[] args) {
        if(!GoogleCalendar.utilityObjectsInitialized(event, args))
            return;

        Calendar service = GoogleCalendar.getCalendarService();

        try {
            // get the calendar that corresponds to the guild from which the command was invoked
            com.google.api.services.calendar.model.Calendar calendar = service
                .calendars()
                .get(CALENDAR_ID)
                .execute();

            MessageEmbed message = getMessageEmbed(calendar,event,args);

            // send the message in the channel the command was invoked
            event.getChannel()
                .sendMessage(message)
                .queue();

        } catch (IOException exception) {
            LOGGER.error(exception.getMessage());
            event.getChannel()
                .sendMessage("Could not retrieve the information about calendar. I can't" +
                        " execute the command. Please contact the developer team.")
                .queue();
        }
    }

    /**
     * Builds the embed message
     * @param calendar the calendar that corresponds to the guild from which the command was invoked
     * @return the embed message as {@link MessageEmbed}
     */
    private static MessageEmbed getMessageEmbed(com.google.api.services.calendar.model.Calendar calendar,
                                                MessageReceivedEvent event, String[] args) {

        StringBuilder stringBuilder = new StringBuilder(calendar.getSummary());
        stringBuilder.append("\n\n")
                .append(calendar.getDescription())
                .append("\n\n\n");

        return new EmbedBuilder()
                .setTitle(EmojiParser.parseToUnicode(":spiral_calendar_pad:") + " Calendar Information "
                        + EmojiParser.parseToUnicode(":spiral_calendar_pad:"))
                .setDescription(stringBuilder.toString())
                .setFooter(EmojiParser.parseToUnicode(":robot_face:") +
                        " Powered by ACM Auth Bot Development team " +
                        EmojiParser.parseToUnicode(":robot_face:"))
                .setColor(Color.decode("#309ECF"))
                .setThumbnail(event.getGuild().getIconUrl())
                .addField(new MessageEmbed.Field("Integrate our calendar with your Google Calendar:",
                        calendar.getId(),true,true))
                .build();
    }
}
