package uk.ac.lancs.socialcomp.io;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Database {
    public static Connection getConnection(String name) throws Exception {
        Properties properties = new Properties();
        properties.load(Database.class.getResourceAsStream("/" + name + "-db.properties"));
        Class.forName ("com.mysql.jdbc.Driver").newInstance ();
        return DriverManager.getConnection(
                properties.getProperty("dbURL") + properties.getProperty("dbNAME"),
                properties.getProperty("dbUSER"),
                properties.getProperty("dbPASS"));

    }

    public static void close(Connection connection) throws Exception {
        connection.close();
    }
}
