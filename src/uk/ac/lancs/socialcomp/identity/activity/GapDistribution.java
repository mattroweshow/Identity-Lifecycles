package uk.ac.lancs.socialcomp.identity.activity;

import uk.ac.lancs.socialcomp.io.Database;
import uk.ac.lancs.socialcomp.io.QueryGrabber;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 16/04/2014 / 13:09
 */
public class GapDistribution {

    HashMap<String,Date> postToDate;
    HashMap<String,HashSet<String>> userToPosts;

    String DB;

    public GapDistribution(String DB) {
        this.DB = DB;

        // load data into memory
        try {
            System.out.println("-Loading data into memory");
            // get the SQL query that is to be run in order to retrieve the user info
            String query = QueryGrabber.getQuery(DB, "getPostDetails");
            Connection connection = Database.getConnection(DB);

            userToPosts = new HashMap<String, HashSet<String>>();
            postToDate = new HashMap<String, Date>();

            // query the db
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
                postToDate.put(postid,postDate);
            }
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void deriveGapDistribution() {

        // for each user work out the maximal gap in days between their posts
        ArrayList<Integer> gaps = new ArrayList<Integer>();

        for (String user : userToPosts.keySet()) {
            HashSet<String> userPosts = userToPosts.get(user);
            int maxGap = 0;
            for (String post1 : userPosts) {
                Date post1Date = postToDate.get(post1);
                long post1DateLong = post1Date.getTime() / 1000 / 60 / 60 / 24;

                for (String post2 : userPosts) {
                    Date post2Date = postToDate.get(post2);
                    long post2DateLong = post2Date.getTime() / 1000 / 60 / 60 / 24;

                    // work out the difference in days
                    int diff = (int)Math.abs(post1DateLong - post2DateLong);

                    // update the max gap if the diff exceeds the current max gap
                    if(diff >  maxGap)
                        maxGap = diff;
                }
            }

            gaps.add(maxGap);
        }


        // write the gap distribution to a file
        StringBuffer buffer = new StringBuffer();
        for (Integer gap : gaps) {
            buffer.append(gap + "\n");
        }

        // write the whole thing to a file
        try {
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "_gap_distribution.tsv");
            writer.write(buffer.toString());
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

//        String DB = "facebook";
//        String DB = "sap";
//        String DB = "serverfault";
        String DB = "boards";

        GapDistribution distribution = new GapDistribution(DB);
        distribution.deriveGapDistribution();


    }
}
