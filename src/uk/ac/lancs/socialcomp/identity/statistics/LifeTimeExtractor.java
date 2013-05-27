package uk.ac.lancs.socialcomp.identity.statistics;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import uk.ac.lancs.socialcomp.io.Database;
import uk.ac.lancs.socialcomp.io.QueryGrabber;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class LifeTimeExtractor {

    public static final String DB = "facebook";

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
            String postid = results.getString("messageuri");
            Date postDate = new Date(results.getTimestamp("created").getTime());
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
        List<Lifetime> lifetimes = new ArrayList<Lifetime>();
        for (String user : userToPosts.keySet()) {
            HashSet<String> posts = userToPosts.get(user);
            // get the earliest post
            Date earliest = new Date(); // this will set the date object as today's
            for (String post : posts) {
                Date postDate = postsToDate.get(post);
                if(postDate.before(earliest))
                    earliest = postDate;
            }
            // get the latest post
            Date latest = earliest;
            for (String post : posts) {
                Date postDate = postsToDate.get(post);
                if(postDate.after(latest))
                    latest = postDate;
            }
            // create the lifetime object and record it
            Lifetime lifetime = new Lifetime(user,earliest,latest);
            lifetimes.add(lifetime);
        }

        // close the connection
        connection.close();

        return lifetimes;
    }


    /*
    * Derive a lifetime map
    */
    public HashMap<String,Lifetime> deriveLifetimeMap(String DB) throws Exception {
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
            String postid = results.getString("messageuri");
            Date postDate = new Date(results.getTimestamp("created").getTime());
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
        HashMap<String,Lifetime> lifetimes = new HashMap<String, Lifetime>();
        for (String user : userToPosts.keySet()) {
            HashSet<String> posts = userToPosts.get(user);
            // get the earliest post
            Date earliest = new Date(); // this will set the date object as today's
            for (String post : posts) {
                Date postDate = postsToDate.get(post);
                if(postDate.before(earliest))
                    earliest = postDate;
            }
            // get the latest post
            Date latest = earliest;
            for (String post : posts) {
                Date postDate = postsToDate.get(post);
                if(postDate.after(latest))
                    latest = postDate;
            }
            // create the lifetime object and record it
            Lifetime lifetime = new Lifetime(user,earliest,latest, posts);
            lifetimes.put(user,lifetime);
        }

        // close the connection
        connection.close();

        return lifetimes;
    }


    /*
     * Outputs the lifetime statistics to a file
     */
    public void outputLifetimeStats(List<Lifetime> lifetimes) throws Exception {

        StringBuffer output = new StringBuffer();

        // work out the lifetime duration of each user
        for (Lifetime lifetime : lifetimes) {
            long duration = (lifetime.death.getTime() - lifetime.birth.getTime()) / 1000; // in seconds
            duration /= 60; // in minutes
            duration /= 60; // in hours
            duration /= 24; // in days
            Double durationVal = new Double(duration);
            if(!durationVal.isInfinite()) {
                duration++; // knock it on one as a user cannot be on the platform for 0 days!
                output.append(duration + "\n");
            }
        }

        // output the result to the log file
        File file = new File("data/logs/" + DB + "_lifetimes.tsv");
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(output.toString());
        bw.close();
    }


    public static void main(String[] args) {
        try {

            LifeTimeExtractor lifeTimeExtractor = new LifeTimeExtractor();
            List<Lifetime> lifetimes = lifeTimeExtractor.deriveLifetimes();
            System.out.println(lifetimes.size());
            lifeTimeExtractor.outputLifetimeStats(lifetimes);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
