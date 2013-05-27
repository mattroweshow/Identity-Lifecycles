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

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2013 / 12:58
 */
public class PostActivityAssessor {

    public static final String DB = "facebook";

    Logger logger = LogManager.getLogger(PostActivityAssessor.class.getName());

    /*
     * Derives the post count for each user
     */
    public List<Integer> derivePostCounts() throws Exception {
        // get the SQL query that is to be run in order to retrieve the user info
        String query = QueryGrabber.getQuery(DB, "getPostDetails");
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
        }
        statement.close();

        // work out how many times each user has posted
        List<Integer> postFrequencies = new ArrayList<Integer>();
        for (String user : userToPosts.keySet()) {
            postFrequencies.add(userToPosts.get(user).size());
        }

        // close the connection
        connection.close();

        return postFrequencies;
    }


    /*
     * Outputs the post frequencies to a file to allow the distribution to be examined
     */
    public void outputLifetimeStats(List<Integer> postFrequencies) throws Exception {

        StringBuffer output = new StringBuffer();

        // work out the lifetime duration of each user
        for (Integer frequency : postFrequencies) {
            output.append(frequency + "\n");
        }

        // output the result to the log file
        File file = new File("data/logs/" + DB + "_post_frequencies.tsv");
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(output.toString());
        bw.close();
    }


    public static void main(String[] args) {
        try {

            PostActivityAssessor activityAssessor = new PostActivityAssessor();
            List<Integer> postFrequencies = activityAssessor.derivePostCounts();
            activityAssessor.outputLifetimeStats(postFrequencies);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
