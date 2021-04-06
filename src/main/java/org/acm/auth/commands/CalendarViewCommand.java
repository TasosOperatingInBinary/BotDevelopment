package org.acm.auth.commands;

import com.google.api.services.calendar.Calendar;
import com.vdurmont.emoji.EmojiParser;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.acm.auth.db.Tables;
import org.acm.auth.services.GoogleCalendar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.NoDataFoundException;
import org.jooq.impl.DSL;
import java.sql.Connection;
import java.sql.DriverManager;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Represents a command that displays the description of the calendar that corresponds to the guild
 * from which was invoked.
 */
public class CalendarViewCommand extends Command {
    private static final Logger LOGGER = LogManager.getLogger(CalendarViewCommand.class);
    private static final Dotenv env = Dotenv.load();
    private static final String url = "jdbc:mariadb://localhost:3306";
    private static final String username = env.get("MYSQL_USER");
    private static final String password = env.get("MYSQL_PASSWORD");

    /**
     * Constructs a CalendarView command
     */
    public CalendarViewCommand() {
        super(
        "calview", // name
        "Displays the description of the calendar that corresponds to the guild the command was executed.", // description
        true, // guildOnly
        false, // devOnly
        new String[] {"cview"}, // alias
        0, // minArgs
        0, // maxArgs
        "", // usage
        new Permission[] { Permission.MESSAGE_EMBED_LINKS }, // botPerms
        new Permission[] {}); // usrPerms
    }

    @Override
    public void invoke(MessageReceivedEvent event, String[] args) {
        if(event.getAuthor().isBot())
            return;

        if(!GoogleCalendar.utilityObjectsInitialized(event, args))
            return;

        Record calendarRecord;
        try {
            calendarRecord = fetchCalendarId(event,args);

        } catch (NoDataFoundException e) { // the query returned no rows
            event.getChannel().sendMessage("There is no calendar associated with the current server! Please set the " +
                    "server's calendar with the **calset** command and then try again executing this command. "
                    + EmojiParser.parseToUnicode(":blush:"))
                    .queue();
            return;

        } catch (SQLException|DataAccessException e) {  // something went wrong while executing a SQL statement from jOOQ ( DataAccessException )
                                                        // a database access error occurred or the url is null ( SQLException )
            LOGGER.error(e.getMessage());
            event.getChannel().sendMessage("There is an error with our database. Please try again later. " +
                    EmojiParser.parseToUnicode(":innocent:"))
                    .queue();
            return;
        }

        // the query returned successfully the calendar id
        String CALENDAR_ID = calendarRecord.get(Tables.CALENDARS.CALENDAR_ID);
        Calendar service = GoogleCalendar.getCalendarService();

        try {
            // get the calendar that corresponds to the guild from which the command was executed
            com.google.api.services.calendar.model.Calendar calendar = service
                .calendars()
                .get(CALENDAR_ID)
                .execute();

            MessageEmbed message = buildMessageEmbed(calendar,event,args);

            // send the message in the channel the command was invoked
            event.getChannel()
                .sendMessage(message)
                .queue();

        } catch (IOException exception) {
            LOGGER.warn(exception.getMessage());
            event.getChannel()
                .sendMessage("Could not retrieve the information about calendar. I can't" +
                        " execute the command. Maybe the server's calendar id is not valid. Also don't forget sharing " +
                        "the calendar with me and giving me access to managing events! " + EmojiParser.parseToUnicode(":spiral_calendar_pad:"))
                .queue();
        }
    }

    /**
     * Builds the embed message
     * @param calendar the calendar that corresponds to the guild from which the command was invoked
     * @return the embed message as {@link MessageEmbed}
     */
    private static MessageEmbed buildMessageEmbed(com.google.api.services.calendar.model.Calendar calendar,
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

    /**
     * Returns the calendar id of the guild the command was executed.
     * @return the record of the table CALENDARS as {@link org.jooq.Record}
     * @throws SQLException if a database access error occurred or the url is null
     * @throws DataAccessException if something went wrong while executing a SQL statement from jOOQ
     */
    private Record fetchCalendarId(MessageReceivedEvent event, String[] args) throws SQLException, DataAccessException{
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            DSLContext create = DSL.using(conn, SQLDialect.MARIADB);

            // fetch guild's corresponding calendar
            return create.fetchSingle(Tables.CALENDARS, Tables.CALENDARS.GUILD_ID.eq(event.getGuild().getId()));
        }
    }
}
