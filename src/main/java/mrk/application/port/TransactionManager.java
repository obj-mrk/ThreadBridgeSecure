package mrk.application.port;

import java.sql.Connection;

public interface TransactionManager {
    <T> T execute(TransactionCallback<T> callback);

    @FunctionalInterface
    interface TransactionCallback<T> {
        T doInTransaction(Connection connection) throws Exception;
    }
}