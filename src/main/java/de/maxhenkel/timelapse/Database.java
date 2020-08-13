package de.maxhenkel.timelapse;

import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.database.SQLiteBase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteBase {

    public Database(Configuration config) throws SQLException {
        super(config.getString("database_path", "database.db"));
    }

    @Override
    protected void init(Connection connection) throws SQLException {
        connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS whitelist (id INTEGER PRIMARY KEY NOT NULL, comment TEXT);");
        connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS blacklist (id INTEGER PRIMARY KEY NOT NULL, comment TEXT);");
    }

    public boolean isWhitelisted(int userID) throws SQLException {
        ResultSet rs = getConnection().createStatement().executeQuery("SELECT id FROM whitelist WHERE id= " + userID + ";");

        return rs.next();
    }

    public boolean isBlacklisted(int userID) throws SQLException {
        ResultSet rs = getConnection().createStatement().executeQuery("SELECT id FROM blacklist WHERE id= " + userID + ";");

        return rs.next();
    }

    public void addToWhitelist(int id, String comment) throws SQLException {
        getConnection().createStatement()
                .execute("INSERT INTO whitelist (id, comment) VALUES (" + id + ", '" + comment + "');");
    }

    public void addToBlacklist(int id, String comment) throws SQLException {
        getConnection().createStatement()
                .execute("INSERT INTO blacklist (id, comment) VALUES (" + id + ", '" + comment + "');");
    }

    public void removeFromWhitelist(int id) throws SQLException {
        getConnection().createStatement()
                .execute("DELETE FROM whitelist WHERE id=" + id + ";");
    }

    public void removeFromBlacklist(int id) throws SQLException {
        getConnection().createStatement()
                .execute("DELETE FROM blacklist WHERE id=" + id + ";");
    }

    public Entry getWhitelistEntry(int id) throws SQLException {
        ResultSet rs = getConnection().createStatement().executeQuery("SELECT * FROM whitelist WHERE id= " + id + ";");

        return get(rs, true);
    }

    public Entry getBlacklistEntry(int id) throws SQLException {
        ResultSet rs = getConnection().createStatement().executeQuery("SELECT * FROM blacklist WHERE id= " + id + ";");

        return get(rs, true);
    }

    public List<Entry> getWhitelistEntries() throws SQLException {
        ResultSet rs = getConnection().createStatement().executeQuery("SELECT * FROM whitelist;");
        List<Entry> entries = new ArrayList<>();

        while (rs.next()) {
            Entry e = get(rs, false);
            if (e != null) {
                entries.add(e);
            }
        }

        return entries;
    }

    public List<Entry> getBlacklistEntries() throws SQLException {
        ResultSet rs = getConnection().createStatement().executeQuery("SELECT * FROM blacklist;");
        List<Entry> entries = new ArrayList<>();

        while (rs.next()) {
            Entry e = get(rs, false);
            if (e != null) {
                entries.add(e);
            }
        }

        return entries;
    }

    private Entry get(ResultSet rs, boolean next) throws SQLException {
        if (next && !rs.next()) {
            return null;
        }

        int i = rs.getInt(1);
        String comment = rs.getString(2);
        Entry entry = new Entry();
        entry.id = i;
        entry.comment = comment;
        return entry;
    }

    public static class Entry {
        private int id;
        private String comment;

        private Entry() {

        }

        public String getComment() {
            return comment;
        }

        public int getId() {
            return id;
        }
    }
}
