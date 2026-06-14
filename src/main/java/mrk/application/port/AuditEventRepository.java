package mrk.application.port;

import mrk.domain.model.AuditEvent;

import java.sql.Connection;

public interface AuditEventRepository {
    long save(Connection connection, AuditEvent event);
}