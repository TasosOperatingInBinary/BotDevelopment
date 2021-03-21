package org.acm.auth.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class GoogleCalendar {
    private static NetHttpTransport HTTP_TRANSPORT = null;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static HttpRequestInitializer requestInitializer = null;
    private static final Logger LOGGER = LogManager.getLogger(GoogleCalendar.class);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch(GeneralSecurityException | IOException exception) {
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
     * Returns a Calendar service.
     * @return the calendar service as {@link com.google.api.services.calendar.Calendar}
     */
    public static Calendar getCalendarService() {
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                .setApplicationName("ACM Auth Bot")
                .build();
    }

    /**
     * Verifies that the utility objects used in creating API calls were properly initialized
     * @param event the event created when a message was sent in a {@link net.dv8tion.jda.api.entities.MessageChannel MessageChannel}.
     * @param args command's potential arguments
     * @return whether or not the utility objects used in API calls were properly initialized as {@code boolean}
     */
    public static boolean utilityObjectsInitialized(MessageReceivedEvent event, String[] args) {
        if(HTTP_TRANSPORT==null) {
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
