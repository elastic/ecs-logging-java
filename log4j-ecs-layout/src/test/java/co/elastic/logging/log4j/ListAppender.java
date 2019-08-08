package co.elastic.logging.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

class ListAppender extends AppenderSkeleton {
    private List<LoggingEvent> logEvents = new ArrayList<>();

    @Override
    protected void append(LoggingEvent event) {
        logEvents.add(event);
    }

    @Override
    public void close() {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    public List<LoggingEvent> getLogEvents() {
        return logEvents;
    }
}
