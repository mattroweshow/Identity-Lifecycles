package uk.ac.lancs.socialcomp.identity.statistics;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import uk.ac.lancs.socialcomp.io.Database;
import uk.ac.lancs.socialcomp.io.QueryGrabber;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class LifeTimeExtractor {

    public static final String DB = "boards";

    Logger logger = LogManager.getLogger(LifeTimeExtractor.class.getName());


    /*
     * Derive a list of lifetime objects of community users
     */
    public List<Lifetime> deriveLifetimes() throws Exception {
        // get the SQL query that is to be run in order to retrieve the user info
        String query = QueryGrabber.getQuery(DB,"getPostDetails");

        // store the users and their posts
        HashMap<String,Date> postsToDate = new HashMap<String, Date>();
        HashMap<String,HashSet<String>> userToPosts = new HashMap<String, HashSet<String>>();

        // set up the db connection
        Connection connection = Database.getConnection(DB);
        // query the db
        logger.trace("Gathering user and post details");
        Statement statement = connection.createStatement();
        ResultSet results = statement.executeQuery(query);
        while(results.next())  {
            String postid = results.getString("postid");
            Date postDate = results.getDate("created");
            String userid = results.getString("contributor");

            // insert into the maps
            if(userToPosts.containsKey(userid)) {
                HashSet<String> posts = userToPosts.get(userid);
                posts.add(postid);
                userToPosts.put(userid,posts);
            } else {
                HashSet<String> posts = new HashSet<String>();
                posts.add(postid);
                userToPosts.put(userid,posts);
            }
            postsToDate.put(postid,postDate);
        }
        statement.close();

        // derive the life times for the user
        logger.trace("Working out the life times for the users");
        for (String user : userToPosts.keySet()) {
            HashSet<String> posts = userToPosts.get(user);

        }



        // close the connection
        connection.close();
    }


    public static void main(String[] args) {
        try {


        } catch (Exception e) {

        }
    }


}
