import com.brein.time.timeintervals.intervals.TimestampInterval;
import com.google.api.client.util.Lists;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;

import java.util.List;

public class CalendarInterval extends TimestampInterval {

    private static final Long ONE_DAY = 86_400L;

    private final Event.Organizer organizer;
    private final List<EventAttendee> attendees;
    private final String summary;
    private final boolean allDay;
    private final String eventType; // "default", "outOfOffice", "focusTime"
    private final boolean outOfOffice;

    public CalendarInterval(Event.Organizer organizer, EventDateTime start, EventDateTime end, List<EventAttendee> attendees, String summary, String eventType) {
        super(getTimestamp(start) / 1000, getTimestamp(end) / 1000);
        this.allDay = (getTimestamp(end) / 1000) - (getTimestamp(start) / 1000) == ONE_DAY;
        this.organizer = organizer;
        this.attendees = attendees;
        this.summary = summary;
        this.eventType = eventType;
        this.outOfOffice = "outOfOffice".equals(eventType);
    }

    private static Long getTimestamp(EventDateTime start) {
        return start.getDate() == null ? start.getDateTime().getValue() : start.getDate().getValue();
    }

    public CalendarInterval() {
        organizer = new Event.Organizer();
        attendees = Lists.newArrayList();
        summary = "";
        allDay = false;
        eventType = "default";
        outOfOffice = false;
    }

    @Override
    public String toString() {
        return super.toString() + "//CalendarInterval{" +
                "summary=" + summary +
                ", organizer=" + organizer.getEmail() +
                ", attendees=" + attendees +
                '}';
    }
}
