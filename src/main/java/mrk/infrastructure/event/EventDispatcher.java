package mrk.infrastructure.event;

import mrk.domain.model.OutboxEvent;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventDispatcher {
    private final Map<String, List<EventListener>> listenersByEventType;

    public EventDispatcher(List<EventListener> listeners) {
        this.listenersByEventType = listeners.stream()
                .collect(Collectors.groupingBy(EventListener::eventType));
    }

    public void dispatch(Connection connection, OutboxEvent event) {
        List<EventListener> listeners =
                listenersByEventType.getOrDefault(event.getEventType(), List.of());

        if (listeners.isEmpty()) {
            throw new IllegalStateException("No listeners registered for event type: " + event.getEventType());
        }

        for (EventListener listener : listeners) {
            listener.handle(connection, event);
        }
    }
}