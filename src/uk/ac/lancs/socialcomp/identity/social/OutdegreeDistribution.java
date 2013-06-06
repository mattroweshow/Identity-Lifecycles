package uk.ac.lancs.socialcomp.identity.social;

import uk.ac.lancs.socialcomp.datasets.DatasetRetriever;
import uk.ac.lancs.socialcomp.distribution.DiscreteProbabilityDistribution;
import uk.ac.lancs.socialcomp.distribution.DistributionMeasurer;
import uk.ac.lancs.socialcomp.identity.statistics.Interval;
import uk.ac.lancs.socialcomp.identity.statistics.LifeTimeExtractor;
import uk.ac.lancs.socialcomp.identity.statistics.Lifetime;
import uk.ac.lancs.socialcomp.identity.statistics.LifetimeStageDeriver;
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
 * Date / Time : 17/05/2013 / 13:04
 */
public class OutdegreeDistribution {

    String DB;
    String split;

    HashMap<String,Date> postToDate;
    HashMap<String,HashSet<String>> userToPosts;
    HashMap<String,String> postToUser;

    HashMap<String,String> replyToOriginal;
    HashMap<String,HashSet<String>> originalToReplies;


    public OutdegreeDistribution(String DB, String split) {
        this.DB = DB;
        this.split = split;

        // load data into memory
        try {
            System.out.println("-Loading data into memory");

            // get the SQL query that is to be run in order to retrieve the user info
            String query = QueryGrabber.getQuery(DB, "getAllPosts");
            Connection connection = Database.getConnection(DB);

            userToPosts = new HashMap<String, HashSet<String>>();
            postToDate = new HashMap<String, Date>();
            postToUser = new HashMap<String, String>();

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
                postToUser.put(postid,userid);
            }
            statement.close();


            // load the reply graph into memory
            originalToReplies = new HashMap<String, HashSet<String>>();
            replyToOriginal = new HashMap<String, String>();
            System.out.println("-Loading reply graph into memory");
            String replyGraphQuery = QueryGrabber.getQuery(DB, "getReplyGraph");
            Statement statement1 = connection.createStatement();
            ResultSet results1 = statement1.executeQuery(replyGraphQuery);
            while(results1.next())  {
                String reply = results1.getString("reply");
                String original = results1.getString("original");

                // insert into maps
                if(originalToReplies.containsKey(original)) {
                    HashSet<String> replies = originalToReplies.get(original);
                    replies.add(reply);
                    originalToReplies.put(original,replies);
                } else {
                    HashSet<String> replies = new HashSet<String>();
                    replies.add(reply);
                    originalToReplies.put(original,replies);
                }
                replyToOriginal.put(reply,original);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Derives the period-specific entropy of the user's outdegree distribution
     */
    public void deriveEntropyPerStageDistributions() {
        System.out.println("-Computing per-stage entropies");

        try {
            // get the training set users for this dataset
            DatasetRetriever retriever = new DatasetRetriever();
            HashSet<String> users = new HashSet<String>();
            if(split.equals("train")) {
                users = retriever.getTrainingUsers(DB);
            } else {
                users = retriever.getTestingUsers(DB);
            }

            // get the lifeperods of each user
            LifeTimeExtractor extractor = new LifeTimeExtractor(DB);
            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(DB);

            // derive the activity proportion for each user across his stages
            StringBuffer buffer = new StringBuffer();
            for (String user : users) {
                // the user may not have initiated anything, hence only call the function if he has
                if(userToPosts.containsKey(user)) {
                    try {
                        TreeMap<Integer,Double> stageEntropies = this.derivePeriodIndependentEntropy(user,this.userToPosts.get(user), lifetimes.get(user));
                        if(stageEntropies.size() == 20) {
                            String vector = this.convertToStringVector(stageEntropies);
                            buffer.append(user + "\t" + vector + "\n");
                        }
                    }  catch(Exception e) {
                    }
                }
            }
            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "_" + split +"_outdegree_entropies_stages.tsv");
            writer.write(buffer.toString());
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TreeMap<Integer,Double> derivePeriodIndependentEntropy(String user, HashSet<String> posts, Lifetime lifetime) {
        TreeMap<Integer,Double> stageEntropies = new TreeMap<Integer, Double>();

        // split the lifetime up into 20 equal sized stages
        LifetimeStageDeriver lifetimeStageDeriver = new LifetimeStageDeriver(lifetime, postToDate);
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(20);

        // get the set of posts that the user replied to: reply -> orig
        HashMap<String,String> repliedToPosts = new HashMap<String,String>();
        for (String post : posts) {
            if(replyToOriginal.containsKey(post))
                repliedToPosts.put(post, replyToOriginal.get(post));
        }


        // derive the outdegree distribution over the time period
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Interval interval = intervals.get(startInterval);
            Date endInterval = interval.getEndInterval();

            // tally the replies to users within the time period
            HashMap<String,Integer> userToRepliedToFreq = new HashMap<String, Integer>();
            double replyCount = 0;
            for (String post : repliedToPosts.keySet()) {
                if(postToDate.containsKey(post)) {
                    Date postDate = postToDate.get(post);
                    if((postDate.equals(startInterval) || postDate.after(startInterval)) && (postDate.before(endInterval))) {
                        String repliedToID = postToUser.get(repliedToPosts.get(post));
                        if(userToRepliedToFreq.containsKey(repliedToID)) {
                            int tally = userToRepliedToFreq.get(repliedToID);
                            tally++;
                            userToRepliedToFreq.put(repliedToID,tally);
                        } else {
                            userToRepliedToFreq.put(repliedToID,1);
                        }
                        replyCount++;
                    }
                }
            }

            // work out the probability distribution for the time period
            HashMap<String,Double> userToRepliedToProb = new HashMap<String, Double>();
            for (String userA : userToRepliedToFreq.keySet()) {
//                double userReplyFreq = userToReplyFreq.get(userA);
                double userRepliedToFreq = userToRepliedToFreq.get(userA);
                double userRepliedToProb = userRepliedToFreq / replyCount++;
                userToRepliedToProb.put(userA,userRepliedToProb);

            }

            // create the prob dist object
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(user,intervalCount,userToRepliedToProb);
            // work out the entropy of the distribution
            DistributionMeasurer measurer = new DistributionMeasurer();
            double entropy = measurer.measureEntropy(pd);

            // map the interval to its probability
            stageEntropies.put(intervalCount,entropy);
            intervalCount++;
        }

        return stageEntropies;
    }


    /*
    * Derives the minimised cross entropy per period for each user
    */
    public void deriveCrossEntropyPerStageDistributions() {
        System.out.println("-Computing per-stage crosss entropies");

        try {
            // get the training set users for this dataset
            DatasetRetriever retriever = new DatasetRetriever();
            HashSet<String> users = new HashSet<String>();
            if(split.equals("train")) {
                users = retriever.getTrainingUsers(DB);
            } else {
                users = retriever.getTestingUsers(DB);
            }

            // get the lifeperods of each user
            LifeTimeExtractor extractor = new LifeTimeExtractor(DB);
            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(DB);

            // derive the activity proportion for each user across his stages
            StringBuffer buffer = new StringBuffer();
            for (String user : users) {
                // the user may not have initiated anything, hence only call the function if he has
                if(userToPosts.containsKey(user)) {
                    try {
                        TreeMap<Integer,Double> stageEntropies = this.deriveCrossPeriodEntropies(user, this.userToPosts.get(user), lifetimes.get(user));
                        if(stageEntropies.size() == 20) {
                            String vector = this.convertToStringVector(stageEntropies);
                            buffer.append(user + "\t" + vector + "\n");
                        }
                    } catch(Exception e) {
                    }
                }
            }
            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "_" + split + "_outdegree_user_crossentropies_stages.tsv");
            writer.write(buffer.toString());
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TreeMap<Integer,Double> deriveCrossPeriodEntropies(String user,HashSet<String> posts, Lifetime lifetime) {
        TreeMap<Integer,Double> stageEntropies = new TreeMap<Integer, Double>();

        // split the lifetime up into 20 equal sized stages
        LifetimeStageDeriver lifetimeStageDeriver = new LifetimeStageDeriver(lifetime, postToDate);
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(20);

        // get the set of posts that the user replied to: reply -> orig
        HashMap<String,String> repliedToPosts = new HashMap<String,String>();
        for (String post : posts) {
            if(replyToOriginal.containsKey(post))
                repliedToPosts.put(post, replyToOriginal.get(post));
        }

        // derive the indegree distribution over the time period and persist them
        TreeMap<Integer,DiscreteProbabilityDistribution> stageDistributions = new TreeMap<Integer, DiscreteProbabilityDistribution>();
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Interval interval = intervals.get(startInterval);
            Date endInterval = interval.getEndInterval();

            // tally the number of times that the user has replied to someone in the period
            HashMap<String,Integer> userToRepliedToFreq = new HashMap<String, Integer>();
            double replyCount = 0;
            for (String post : repliedToPosts.keySet()) {
                if(postToDate.containsKey(post)) {
                    Date postDate = postToDate.get(post);
                    if((postDate.equals(startInterval) || postDate.after(startInterval)) && (postDate.before(endInterval))) {
                        String recipientID = postToUser.get(repliedToPosts.get(post));
                        if(userToRepliedToFreq.containsKey(recipientID)) {
                            int tally = userToRepliedToFreq.get(recipientID);
                            tally++;
                            userToRepliedToFreq.put(recipientID,tally);
                        } else {
                            userToRepliedToFreq.put(recipientID,1);
                        }
                        replyCount++;
                    }
                }
            }

            // work out the probability distribution for the time period
//            HashMap<String,Double> userToReplyProb = new HashMap<String, Double>();
            HashMap<String,Double> userToRepliedToProb = new HashMap<String, Double>();
            for (String userA : userToRepliedToFreq.keySet()) {
                double userRepliedToFreq = userToRepliedToFreq.get(userA);
                double userRepliedToProb = userRepliedToFreq / replyCount;
                userToRepliedToProb.put(userA,userRepliedToProb);
            }

            // create the prob dist object
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(user,intervalCount,userToRepliedToProb);
            // persists the distribution for its stage
            stageDistributions.put(intervalCount,pd);

            intervalCount++;
        }

        // go through each stage and work out the cross entropy
        DistributionMeasurer measurer = new DistributionMeasurer();
        for (Integer interval : stageDistributions.keySet()) {
            DiscreteProbabilityDistribution pd1 = stageDistributions.get(interval);

            double crossEntropy = 0;
            boolean entropyCalculated = false;

            for (Integer intervalA : stageDistributions.keySet()) {
                if(intervalA < interval) {
                    DiscreteProbabilityDistribution pd2 = stageDistributions.get(intervalA);
                    // work out the cross entropy
                    double crossEntropyA = measurer.measureCrossEntropy(pd1,pd2);

                    if(!entropyCalculated) {
                        entropyCalculated = true;
                        crossEntropy = crossEntropyA;
                    } else {
                        if((crossEntropyA < crossEntropy) && (crossEntropyA != 0)) {
                            crossEntropy = crossEntropyA;
                        }
                    }
                }
            }
            // map the interval to its cross entropy
            stageEntropies.put(interval,crossEntropy);
        }
        return stageEntropies;
    }


    /*
     * Derives the cross entropy of the user's in-degree distribution with the global in-degree distribution
     * over the same time period
     */
    public void deriveCommunityDependentStageDistributions() {
        System.out.println("-Computing per-stage community crosss entropies");

        try {
            // get the training set users for this dataset
            DatasetRetriever retriever = new DatasetRetriever();
            HashSet<String> users = new HashSet<String>();
            if(split.equals("train")) {
                users = retriever.getTrainingUsers(DB);
            } else {
                users = retriever.getTestingUsers(DB);
            }

            // get the lifeperods of each user
            LifeTimeExtractor extractor = new LifeTimeExtractor(DB);
            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(DB);

            // derive the activity proportion for each user across his stages
            StringBuffer buffer = new StringBuffer();
            double totalUsers = users.size();
            double count = 1;
            for (String user : users) {
                // the user may not have initiated anything, hence only call the function if he has
                if(userToPosts.containsKey(user)) {
                    try {
                        TreeMap<Integer,Double> stageEntropies = this.deriveCommunityPeriodEntropies(user, this.userToPosts.get(user), lifetimes.get(user));
                        if(stageEntropies.size() == 20) {
                            String vector = this.convertToStringVector(stageEntropies);
                            buffer.append(user + "\t" + vector + "\n");
                        }
                    } catch(Exception e) {
                    }

                    double soFar = (count / totalUsers) * 100;
                    System.out.println("soFar = " + soFar);
                    count++;
                }
            }
            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "_" + split + "_outdegree_community_crossentropies_stages.tsv");
            writer.write(buffer.toString());
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TreeMap<Integer,Double> deriveCommunityPeriodEntropies(String user, HashSet<String> posts, Lifetime lifetime) {
        TreeMap<Integer,Double> stageCrossEntropies = new TreeMap<Integer, Double>();

        // split the lifetime up into 20 equal sized stages
        LifetimeStageDeriver lifetimeStageDeriver = new LifetimeStageDeriver(lifetime, postToDate);
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(20);

        // get the set of replied to posts by the user
        HashMap<String,String> repliedToPosts = new HashMap<String, String>();
        for (String post : posts) {
            if(replyToOriginal.containsKey(post))
                repliedToPosts.put(post, replyToOriginal.get(post));
        }

        // derive the outdegree distribution over the time period
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Interval interval = intervals.get(startInterval);
            Date endInterval = interval.getEndInterval();

            // tally the replies to users within the time period
//            HashMap<String,Integer> userToReplyFreq = new HashMap<String, Integer>();
            HashMap<String,Integer> userToRepliedToFreq = new HashMap<String, Integer>();
            double replyCount = 0;
            for (String post : repliedToPosts.keySet()) {
                if(postToDate.containsKey(post)) {
                    Date postDate = postToDate.get(post);
                    if((postDate.equals(startInterval) || postDate.after(startInterval)) && (postDate.before(endInterval))) {
                        String recipientID = postToUser.get(repliedToPosts.get(post));
                        if(userToRepliedToFreq.containsKey(recipientID)) {
                            int tally = userToRepliedToFreq.get(recipientID);
                            tally++;
                            userToRepliedToFreq.put(recipientID,tally);
                        } else {
                            userToRepliedToFreq.put(recipientID,1);
                        }
                        replyCount++;
                    }
                }
            }

            // work out the probability distribution for the time period
//            HashMap<String,Double> userToReplyProb = new HashMap<String, Double>();
            HashMap<String,Double> userToRepliedToProb = new HashMap<String, Double>();
            for (String userA : userToRepliedToFreq.keySet()) {
                double userRepliedToFreq = userToRepliedToFreq.get(userA);
                double userRepliedToProb = userRepliedToFreq / replyCount;
                userToRepliedToProb.put(userA,userRepliedToProb);
            }

            // create the prob dist object for the user
            DiscreteProbabilityDistribution pd1 = new DiscreteProbabilityDistribution(user,intervalCount,userToRepliedToProb);

            // work out the global probability distribution
            // get the replies that were posted within the window
            HashMap<String,Integer> windowRepliedToFreq = new HashMap<String, Integer>();
//            HashMap<String,Integer> windowRepliesFromFreq = new HashMap<String, Integer>();
            double globalReplyCount = 0;
            for (String post : postToDate.keySet()) {
                if(replyToOriginal.containsKey(post)) {
                    String original = replyToOriginal.get(post);
                    Date postDate = postToDate.get(post);

                    // check that the reply was in the window
                    if((postDate.after(startInterval) || postDate.equals(startInterval)) && (postDate.before(endInterval))) {
                        // tally how many times the user has been replied to within the window
                        if(postToUser.containsKey(original)) {
                            String recipientAuthor = postToUser.get(original);
                            if(windowRepliedToFreq.containsKey(recipientAuthor)) {
                                int tally = windowRepliedToFreq.get(recipientAuthor);
                                tally++;
                                windowRepliedToFreq.put(recipientAuthor,tally);
                            } else {
                                windowRepliedToFreq.put(recipientAuthor,1);
                            }
                            globalReplyCount++;
                        }
                    }
                }
            }
            // work out the probability of the user posting a reply
            HashMap<String,Double> windowRepliedToProb = new HashMap<String, Double>();

            for (String repliedToId : windowRepliedToFreq.keySet()) {
                double freq = windowRepliedToFreq.get(repliedToId);
                double prob = freq / globalReplyCount;
                windowRepliedToProb.put(repliedToId,prob);
            }
            // set the discrete probability distribution object
            DiscreteProbabilityDistribution pd2 = new DiscreteProbabilityDistribution("global",intervalCount,windowRepliedToProb);

            // work out the cross entropy between the user's in-degree probability distribution and the global distribution
            DistributionMeasurer measurer = new DistributionMeasurer();
            double crossEntropy = measurer.measureCrossEntropy(pd1,pd2);

            // map the interval to its probability
            stageCrossEntropies.put(intervalCount,crossEntropy);
            intervalCount++;
        }

        return stageCrossEntropies;
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
        String db = "boards";
        String split = "test";

        OutdegreeDistribution outdegreeDistribution = new OutdegreeDistribution(db,split);

        outdegreeDistribution.deriveEntropyPerStageDistributions();
        outdegreeDistribution.deriveCrossEntropyPerStageDistributions();
        outdegreeDistribution.deriveCommunityDependentStageDistributions();

    }

}
