package de.maxhenkel.timelapse;

import de.maxhenkel.henkellib.config.Configuration;
import de.maxhenkel.henkellib.database.SQLiteBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database extends SQLiteBase{

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
        ResultSet rs=getConnection().createStatement().executeQuery("SELECT id FROM whitelist WHERE id= " +userID +";");

        return rs.next();
    }

    public boolean isBlacklisted(int userID) throws SQLException {
        ResultSet rs=getConnection().createStatement().executeQuery("SELECT id FROM blacklist WHERE id= " +userID +";");

        return rs.next();
    }

    public void addToWhitelist(int id, String comment) throws SQLException {
        getConnection().createStatement()
                .execute("INSERT INTO whitelist (id, comment) VALUES (" +id +", '" +comment +"');");
    }

    public void addToBlacklist(int id, String comment) throws SQLException {
        getConnection().createStatement()
                .execute("INSERT INTO blacklist (id, comment) VALUES (" +id +", '" +comment +"');");
    }

    public Entry getWhitelistEntry(int id) throws SQLException {
        ResultSet rs=getConnection().createStatement().executeQuery("SELECT * FROM whitelist WHERE id= " +id +";");

        return get(rs);
    }

    public Entry getBlacklistEntry(int id) throws SQLException {
        ResultSet rs=getConnection().createStatement().executeQuery("SELECT * FROM blacklist WHERE id= " +id +";");

        return get(rs);
    }

    private Entry get(ResultSet rs) throws SQLException {
        if(!rs.next()){
            return null;
        }

        int i=rs.getInt(0);
        String comment=rs.getString(1);
        Entry entry=new Entry();
        entry.id=i;
        entry.comment=comment;
        return entry;
    }

    public static class Entry{
        private int id;
        private String comment;

        private Entry(){

        }

        public String getComment() {
            return comment;
        }

        public int getId() {
            return id;
        }
    }
}
