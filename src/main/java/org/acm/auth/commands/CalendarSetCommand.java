package org.acm.auth.commands;

import com.vdurmont.emoji.EmojiParser;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.acm.auth.db.Tables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarSetCommand extends Command {
    private static final Logger LOGGER = LogManager.getLogger(CalendarViewCommand.class);
    private static final Dotenv env = Dotenv.load();
    private static final String url = "jdbc:mariadb://localhost:3306";
    private static final String username = env.get("MYSQL_USER");
    private static final String password = env.get("MYSQL_PASSWORD");

    /**
     * Constructs a CalendarSet command
     */
    public CalendarSetCommand() {
        super(
        "calset", // name
        "Sets the calendar id that corresponds to the guild the command was executed.", // description
        true, // guildOnly
        false, // devOnly
        new String[] {"cset"}, // alias
        1, // minArgs
        1, // maxArgs
        "", // usage
        new Permission[] { Permission.MESSAGE_MANAGE }, // botPerms
        new Permission[] { Permission.ADMINISTRATOR, Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER }); // usrPerms
    }

    @Override
    public void invoke(MessageReceivedEvent event, String[] args) {
        if(event.getAuthor().isBot())
            return;

        String calendarId = args[0];
        calendarId = calendarId.replaceAll("\"","");

        if(!validateCalendarId(calendarId)) {
            event.getChannel().sendMessage("The id you provided is not a valid google calendar id. Please change " +
                    "the calendar id and try executing again this command. "
                    + EmojiParser.parseToUnicode(":pray:"))
                    .queue();
            return;
        }

        // the provided calendar id is valid so execute the insertion in the db
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            DSLContext create = DSL.using(conn, SQLDialect.MARIADB);

            // if the guild id already exists update the corresponding calendar id
            create.insertInto(Tables.CALENDARS, Tables.CALENDARS.GUILD_ID, Tables.CALENDARS.CALENDAR_ID)
                    .values(event.getGuild().getId(), calendarId)
                    .onDuplicateKeyUpdate()
                    .set(Tables.CALENDARS.CALENDAR_ID, calendarId)
                    .execute();

            // notify the user that the insertion was successful
            event.getChannel().sendMessage("Server's calendar was successfully updated to **" + calendarId + "** " +
                    EmojiParser.parseToUnicode(":tada:")).queue();

            // delete original message
            event.getMessage().delete().queue();

        } catch (SQLException e) { // a database access error occurred or the url is null
            LOGGER.error(e.getMessage());
            event.getChannel().sendMessage("There is an error with our database. Please try again later. " +
                    EmojiParser.parseToUnicode(":innocent:"))
                    .queue();
        }
    }

    /**
     * Validates that a string is a valid google calendar id.
     * @param calendarId the string that will be validated as {@code String}
     * @return true if the provided string is a valid google calendar id, false otherwise as {@code boolean}
     */
    private boolean validateCalendarId(String calendarId) {
        Pattern pattern = Pattern.compile("[A-Za-z0-9+_.-]+\\b@group.calendar.google.com\\b");
        Matcher matcher = pattern.matcher(calendarId);

        return matcher.matches();
    }
}
