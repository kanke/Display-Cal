import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Sets;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.*;

public class CalendarQuickstart {
    private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private static final DateTime yearAgo = new DateTime(System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000));
    private static final DateTime now = new DateTime(System.currentTimeMillis());


    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = CalendarQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static String escapeNewlines(String input) {
        return input.replaceAll("\n", "\\\\\\n").replaceAll("\r", "");
    }

    private static String quoteIfHasComma(@Nullable String input) {
        if (input == null) {
            return "";
        }
        if (input.contains(",")) {
            return "\"" + escapeNewlines(input.replaceAll("\"", "\\\\\"")) + "\"";
        }
        return escapeNewlines(input);
    }

    private static void formatRow(Event event) {
        System.out.printf(
                "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                quoteIfHasComma(event.getId()),
                quoteIfHasComma(event.getOrganizer().getDisplayName()),
                quoteIfHasComma(event.getOrganizer().getEmail()),
                quoteIfHasComma(event.getSummary()),
                quoteIfHasComma(event.getDescription()),
                quoteIfHasComma(event.getStatus()),
                event.getStart().getDateTime(),
                event.getEnd().getDateTime(),
                quoteIfHasComma(event.getAttendees() == null || event.getAttendees().isEmpty() ? "" : event.getAttendees().toString()),
                event.getAttendeesOmitted() != null && event.getAttendeesOmitted() ? "yes" : "no",
                quoteIfHasComma(event.getConferenceData() != null ? event.getConferenceData().toString() : ""),
                quoteIfHasComma(event.getHangoutLink()),
                quoteIfHasComma(event.getICalUID()),
                quoteIfHasComma(event.getLocation()),
                quoteIfHasComma(event.getRecurringEventId())
        );
    }

    private static void fetchEvents(Calendar service, String id, @Nullable String pageToken) throws IOException {
        final Calendar.Events.List request = service.events()
                .list(id)
                .setTimeMin(yearAgo)
                .setTimeMax(now)
                .setOrderBy("startTime")
                .setSingleEvents(true);
        if (pageToken != null) {
            request.setPageToken(pageToken);
        }

        Events events = request.execute();
        List<Event> items = events.getItems();
        if (!items.isEmpty()) {
            for (Event event : items) {
                formatRow(event);
            }
        }

        if (events.getNextPageToken() != null) {
            fetchEvents(service, id, events.getNextPageToken());
        }
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        System.out.println("ID," +
                "Organiser Name," +
                "Organiser Email," +
                "Summary," +
                "Description," +
                "Status," +
                "Start DateTime," +
                "End DateTime," +
                "Attendees," +
                "Attendees Omitted," +
                "Conference Data," +
                "Hangout Link," +
                "ICalUid," +
                "Location," +
                "Recurring EventId");

        Set<String> emails = new HashSet<>(Arrays.asList(
                "hathwal@adaptavist.com",
                "swicks@adaptavist.com",
                "rmartin@adaptavist.com",
                "gstevenson@adaptavist.com",
                "swilliams@adaptavist.com",
                "azaid@adaptavist.com"
        ));
        emails.forEach(email -> {
            try {
                fetchEvents(service, email, null);
            } catch (IOException e) {
                System.err.println("Failed to fetch events: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });

        final CalendarList calendarList = service.calendarList()
                .list()
                .setShowHidden(true)
                .execute();

        calendarList.getItems().forEach(cal -> {
            String calendarItem = String.format(
                    "%s %s %s %s %s %s",
                    cal.getId(),
                    cal.getSummary(),
                    cal.getDescription(),
                    cal.getAccessRole(),
                    cal.getSelected(),
                    cal.getTimeZone()
            );
            System.out.println(calendarItem);
        });

        final com.google.api.services.calendar.model.Calendar calendar = service.calendars()
                .get("cto@jonbevan.me.uk")
                .execute();

        Events events = service.events()
                .list("cto@jonbevan.me.uk")
                .setTimeMin(yearAgo)
                .setTimeMax(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setShowHiddenInvitations(true)
                .execute();
        List<Event> items = events.getItems();
        if (items.isEmpty()) {
            System.out.println("No upcoming events found.");
        } else {
            System.out.println("UPCOMING EVENTS");
            System.out.println("\n");
            for (Event event : items) {
                formatRow(event);
            }
        }
    }
}
