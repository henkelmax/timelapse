package de.maxhenkel.timelapse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class SQLiteBase {

    private Connection connection;
    private String path;

    public SQLiteBase(String path, boolean directConnect) throws SQLException {
        this.path = path;
        if (directConnect) {
            getConnection();
        }
    }

    public SQLiteBase(String path) throws SQLException {
        this(path, true);
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            init(connection);
        }
        return this.connection;
    }

    protected abstract void init(Connection connection) throws SQLException;

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
