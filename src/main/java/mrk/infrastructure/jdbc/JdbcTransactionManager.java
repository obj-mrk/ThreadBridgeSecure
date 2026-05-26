package mrk.infrastructure.jdbc;

import mrk.application.port.TransactionManager;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcTransactionManager implements TransactionManager {
    private final ConnectionFactory connectionFactory;

    public JdbcTransactionManager(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public <T> T execute(TransactionCallback<T> callback) {
        try (Connection connection = connectionFactory.createConnection()) {
            connection.setAutoCommit(false);

            try {
                T result = callback.doInTransaction(connection);
                connection.commit();
                return result;
            } catch (RuntimeException exception) {
                tryRollback(connection, exception);
                throw exception;
            } catch (Exception e) {
                tryRollback(connection, e);
                throw new RuntimeException("Transaction failed", e);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not create database transaction", exception);
        }
    }

    private void tryRollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}