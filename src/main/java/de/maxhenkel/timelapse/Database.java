package de.maxhenkel.timelapse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteBase {

    public Database(String path) throws SQLException {
        super(path);
    }

    @Override
    protected void init(Connection connection) throws SQLException {
        connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS whitelist (id INTEGER PRIMARY KEY NOT NULL, comment TEXT);");
        connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS blacklist (id INTEGER PRIMARY KEY NOT NULL, comment TEXT);");
    }

    public boolean isWhitelisted(long userID) throws SQLException {
        ResultSet rs = getConnection().createStatement().executeQuery("SELECT id FROM whitelist WHERE id= " + userID + ";");

        return rs.next();
    }

    public boolean isBlacklisted(long userID) throws SQLException {
        ResultSet rs = getConnection().createStatement().executeQuery("SELECT id FROM blacklist WHERE id= " + userID + ";");

        return rs.next();
    }

    public void addToWhitelist(long id, String comment) throws SQLException {
        getConnection().createStatement()
                .execute("INSERT INTO whitelist (id, comment) VALUES (" + id + ", '" + comment + "');");
    }

    public void addToBlacklist(long id, String comment) throws SQLException {
        getConnection().createStatement()
                .execute("INSERT INTO blacklist (id, comment) VALUES (" + id + ", '" + comment + "');");
    }

    public void removeFromWhitelist(long id) throws SQLException {
        getConnection().createStatement()
                .execute("DELETE FROM whitelist WHERE id=" + id + ";");
    }

    public void removeFromBlacklist(long id) throws SQLException {
        getConnection().createStatement()
                .execute("DELETE FROM blacklist WHERE id=" + id + ";");
    }

    public Entry getWhitelistEntry(long id) throws SQLException {
        ResultSet rs = getConnection().createStatement().executeQuery("SELECT * FROM whitelist WHERE id= " + id + ";");

        return get(rs, true);
    }

    public Entry getBlacklistEntry(long id) throws SQLException {
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
        private long id;
        private String comment;

        private Entry() {

        }

        public String getComment() {
            return comment;
        }

        public long getId() {
            return id;
        }
    }

}
