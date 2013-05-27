package uk.ac.lancs.socialcomp.identity.activity;

import uk.ac.lancs.socialcomp.datasets.DatasetRetriever;
import uk.ac.lancs.socialcomp.identity.statistics.LifeTimeExtractor;
import uk.ac.lancs.socialcomp.identity.statistics.Lifetime;
import uk.ac.lancs.socialcomp.io.Database;
import uk.ac.lancs.socialcomp.io.QueryGrabber;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2013 / 15:54
 */
public class ReplyDistribution {

    public static final String DB = "sap";

    HashMap<String,Date> postToDate;
    HashMap<String,HashSet<String>> userToPosts;

    public ReplyDistribution() {
        // load data into memory
        try {
            System.out.println("-Loading data into memory");
            // get the SQL query that is to be run in order to retrieve the user info
            String query = QueryGrabber.getQuery(DB, "getReplies");
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


    public void deriveActivityPerStageDistributions() {

        try {
            // get the training set users for this dataset
            DatasetRetriever retriever = new DatasetRetriever();
            HashSet<String> trainingUsers = retriever.getTrainingUsers(DB);

            // get the lifeperods of each user
            LifeTimeExtractor extractor = new LifeTimeExtractor();
            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(DB);

            // derive the activity proportion for each user across his stages
            StringBuffer buffer = new StringBuffer();
            for (String user : trainingUsers) {
                // the user may not have initiated anything, hence only call the function if he has
                if(userToPosts.containsKey(user)) {
                    TreeMap<Integer,Double> stageProportions = this.deriveStageProportions(this.userToPosts.get(user), lifetimes.get(user));
                    String vector = this.convertToStringVector(stageProportions);
                    buffer.append(vector + "\n");
                }
            }

            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "_replies_stages.tsv");
            writer.write(buffer.toString());
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public TreeMap<Integer,Double> deriveStageProportions(HashSet<String> posts, Lifetime lifetime) {
        TreeMap<Integer,Double> stageProportions = new TreeMap<Integer, Double>();

        // split the lifetime up into 20 equal sized stages
        int stagesTotal = 20;
        int stagesCount = 1;
        TreeMap<Date,Date> intervals = new TreeMap<Date, Date>();
        // work out the width of each window
        long windowWidth = (lifetime.getDeath().getTime() - lifetime.getBirth().getTime()) / stagesTotal;
        Date startWindow = lifetime.getBirth();

        while(stagesCount <= stagesTotal) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(startWindow.getTime() + windowWidth);
            Date endWindow = cal.getTime();
//            System.out.println("Window = [" + startWindow + "] ---> [" + endWindow + "]");
            intervals.put(startWindow,endWindow);
            startWindow = endWindow;
            stagesCount++;
        }

        double postsTotal = posts.size();
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Date endInterval = intervals.get(startInterval);

            // work out how many posts were made within the interval
            double postIntervalCount = 0;
            for (String post : posts) {
                Date postDate = postToDate.get(post);
                if((postDate.equals(startInterval) || postDate.after(startInterval)) && (postDate.before(endInterval)))
                    postIntervalCount++;
            }

            // work out the probability of the user posting within the interval
            double postIntervalProb = 0;
            if(postIntervalCount > 0)
                postIntervalProb = postIntervalCount / postsTotal;

            // map the interval to its probability
            stageProportions.put(intervalCount,postIntervalProb);
            intervalCount++;
        }

        return stageProportions;
    }

    private String convertToStringVector(TreeMap<Integer,Double> stageProportions) {
        String vector = "";
        boolean afterFirst = false;
        for (Integer stage : stageProportions.keySet()) {
            double proportion = stageProportions.get(stage);
            if(afterFirst) {
                vector += "\t" + proportion;
            } else {
                vector = "" + proportion;
                afterFirst = true;
            }
        }
        return vector;
    }


    public static void main(String[] args) {

        ReplyDistribution replyDistribution = new ReplyDistribution();
        replyDistribution.deriveActivityPerStageDistributions();


    }

}
