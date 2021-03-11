package org.acm.auth.commands;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.oauth2.GoogleCredentials;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.auth.http.HttpCredentialsAdapter;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Represents a command that displays the description of the calendar that corresponds to the guild
 * from which was invoked.
 */
public class CalendarViewCommand extends Command{
    private static final Logger LOGGER = LogManager.getLogger(CalendarViewCommand.class);
    private static final String CALENDAR_ID = "tln0hmfdrimk3e3j3ngaj7vvag@group.calendar.google.com";
    private static NetHttpTransport HTTP_TRANSPORT = null;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static HttpRequestInitializer requestInitializer = null;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch(GeneralSecurityException| IOException exception) {
            LOGGER.error(exception.getMessage());
        }

        GoogleCredentials credentials = null;
        try {
            credentials = getCredentials();
        }catch (IOException exception) {
            LOGGER.error(exception.getMessage());
        }

        if(credentials!=null)
            requestInitializer = new HttpCredentialsAdapter(credentials);
    }

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
        if(!utilityObjectsInitialized(event, args))
            return;

        // build the Calendar service
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                .setApplicationName("ACM Auth Bot")
                .build();

        try {
            // get the calendar that corresponds to the guild from which the command was invoked
            com.google.api.services.calendar.model.Calendar calendar = service
                .calendars()
                .get(CALENDAR_ID)
                .execute();

            // build the embed message
            StringBuilder stringBuilder = new StringBuilder(calendar.getSummary());
            stringBuilder.append("\n\n")
                .append(calendar.getDescription())
                .append("\n\n\n");

            MessageEmbed message = new EmbedBuilder()
                .setTitle("Calendar Information")
                .setDescription(stringBuilder.toString())
                .setFooter("Powered by ACM Auth")
                .setColor(Color.decode("#309ECF"))
                .setThumbnail("https://auth.acm.org/wp-content/uploads/2020/03/Logo.png")
                .setAuthor("Bot Development Team")
                .addField(new MessageEmbed.Field("Integrate our calendar with your Google Calendar:",
                        calendar.getId(),true,true))
                .build();

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
     * Creates and returns a credentials object for making authorized API calls to Google Calendar API.
     * @return the credentials object as {@link com.google.auth.oauth2.GoogleCredentials}
     * @throws IOException if the credentials file that contains the private key of the service account was not found
     */
    private static GoogleCredentials getCredentials() throws IOException{
       return GoogleCredentials
               .fromStream(new FileInputStream("service_account_credentials.json"))
               .createScoped(CalendarScopes.CALENDAR);
    }

    /**
     * Verifies that the utility objects used in creating API calls were properly initialized
     * @param event the event created when a message was sent in a {@link net.dv8tion.jda.api.entities.MessageChannel MessageChannel}.
     * @param args command's potential arguments
     * @return whether or not the utility objects used in API calls were properly initialized as {@code boolean}
     */
    private boolean utilityObjectsInitialized(MessageReceivedEvent event, String[] args) {
        if(HTTP_TRANSPORT==null) {
            // HTTP_TRANSPORT was not initialized
            event.getChannel()
                .sendMessage("There was an error while initializing my Net HTTP Transport. I can't execute" +
                            " the command. Please contact the developer team.")
                .queue();
            return false;
        }

        if(requestInitializer==null) {
            // credentials file was not found
            event.getChannel()
                .sendMessage("There was an error while trying to initialize my Http Request Initializer. " +
                        "I can't execute the command. Please contact the developer team.")
                .queue();
            return false;
        }

        return true;
    }
}
