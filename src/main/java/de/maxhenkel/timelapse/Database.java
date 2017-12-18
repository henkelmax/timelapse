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
    }

    public boolean isWhitelisted(int userID) throws SQLException {
        ResultSet rs=getConnection().createStatement().executeQuery("SELECT id FROM whitelist WHERE id= " +userID +";");

        return rs.next();
    }

    public void addToWhitelist(int id, String comment) throws SQLException {
        getConnection().createStatement()
                .execute("INSERT INTO whitelist (id, comment) VALUES (" +id +", '" +comment +"');");
    }

    public WhitelistEntry getEntry(int id) throws SQLException {
        ResultSet rs=getConnection().createStatement().executeQuery("SELECT * FROM whitelist WHERE id= " +id +";");

        if(!rs.next()){
            return null;
        }

        int i=rs.getInt(0);
        String comment=rs.getString(1);
        WhitelistEntry entry=new WhitelistEntry();
        entry.id=i;
        entry.comment=comment;
        return entry;
    }

    public static class WhitelistEntry{
        private int id;
        private String comment;

        private WhitelistEntry(){

        }

        public String getComment() {
            return comment;
        }

        public int getId() {
            return id;
        }
    }
}
