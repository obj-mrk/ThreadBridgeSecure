package mrk.domain.model;

import mrk.domain.value.AuditEventType;

import java.time.LocalDateTime;

public class AuditEvent {
    private Long id;
    private Long actorUserId;
    private AuditEventType eventType;
    private String aggregateType;
    private Long aggregateId;
    private String details;
    private LocalDateTime createdAt;

    public static AuditEvent of(
            Long actorUserId,
            AuditEventType eventType,
            String aggregateType,
            Long aggregateId,
            String details
    ) {
        AuditEvent event = new AuditEvent();
        event.setActorUserId(actorUserId);
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setDetails(details);
        return event;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}