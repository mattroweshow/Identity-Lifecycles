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
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 05/06/2013 / 10:35
 */
public class PostsPerDayDistribution {

//    public static final String DB = "facebook";
    public String DB;
    Logger logger = LogManager.getLogger(PostsPerDayDistribution.class.getName());

    public PostsPerDayDistribution(String DB) {
        this.DB = DB;
    }

    /*
    * Derives the number of posts per day
    */
    public TreeMap<Date,HashSet<String>> derivePostCounts() throws Exception {
        // get the SQL query that is to be run in order to retrieve the user info
        String query = QueryGrabber.getQuery(DB, "getPostDetails");
        TreeMap<Date,HashSet<String>> dateToPosts = new TreeMap<Date, HashSet<String>>();

        // set up the db connection
        Connection connection = Database.getConnection(DB);
        // query the db
        logger.trace("Gathering user and post details");
        Statement statement = connection.createStatement();
        ResultSet results = statement.executeQuery(query);
        while(results.next())  {
            String postid = results.getString("messageuri");
            Date date = new Date(results.getTimestamp("created").getTime());

            // format the date by removing the time
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date postDate = sdf.parse(sdf.format(date));

            if(dateToPosts.containsKey(postDate)) {
                HashSet<String> datePosts = dateToPosts.get(postDate);
                datePosts.add(postid);
                dateToPosts.put(postDate,datePosts);
            } else {
                HashSet<String> datePosts = new HashSet<String>();
                datePosts.add(postid);
                dateToPosts.put(postDate,datePosts);
            }
        }
        statement.close();

        // close the connection
        connection.close();

        return dateToPosts;
    }


    /*
     * Outputs the post frequencies to a file to allow the distribution to be examined
     */
    public void outputLifetimeStats(TreeMap<Date,HashSet<String>> dateToPosts) throws Exception {

        // get the first and last date
        Date firstDate = null;
        boolean firstDateSet = false;
        Date lastDate = null;
        boolean lastDateSet = false;

        for (Date date : dateToPosts.keySet()) {
            // set the first date
            if(!firstDateSet) {
                firstDateSet = true;
                firstDate = date;
            } else {
                if(date.before(firstDate))
                    firstDate = date;
            }

            // set the last date
            if(!lastDateSet) {
                lastDateSet = true;
                lastDate = date;
            } else {
                if(date.after(lastDate))
                    lastDate = date;
            }
        }

        StringBuffer output = new StringBuffer();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // get the distribution of posts per day
        Date iterativeDate = firstDate;
        while(iterativeDate.before(lastDate) || iterativeDate.equals(lastDate)) {
            String dateString = sdf.format(iterativeDate);
            if(dateToPosts.containsKey(iterativeDate)) {
                int datePostCount = dateToPosts.get(iterativeDate).size();
                output.append(dateString + "\t" + datePostCount + "\n");
            } else {
                output.append(dateString + "\t0\n");
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(iterativeDate);
            calendar.add(Calendar.DAY_OF_YEAR,+1);
            iterativeDate = calendar.getTime();
        }

        // output the result to the log file
        File file = new File("data/logs/posts/" + DB + "_posts_per_day_frequencies.tsv");
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(output.toString());
        bw.close();
    }


    public static void main(String[] args) {
        try {

//            String[] dbNames = {"facebook","sap","serverfault"};
            String[] dbNames = {"boards"};

            for (String dbName : dbNames) {
                System.out.println("Deriving posts per day distribution for: " + dbName);
                PostsPerDayDistribution distribution = new PostsPerDayDistribution(dbName);
                TreeMap<Date,HashSet<String>> dateToPosts = distribution.derivePostCounts();
                distribution.outputLifetimeStats(dateToPosts);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
