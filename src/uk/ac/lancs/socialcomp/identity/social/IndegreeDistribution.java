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
public class IndegreeDistribution {

//    public static final String DB = "boards";
    String DB;

    HashMap<String,Date> postToDate;
    HashMap<String,HashSet<String>> userToPosts;
    HashMap<String,String> postToUser;

    HashMap<String,String> replyToOriginal;
    HashMap<String,HashSet<String>> originalToReplies;


    public IndegreeDistribution(String DB) {
        this.DB = DB;
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
     * Derives the period-specific entropy of the user's indegree distribution
     */
    public void deriveEntropyPerStageDistributions() {
        System.out.println("-Computing per-stage entropies");

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
                    TreeMap<Integer,Double> stageEntropies = this.derivePeriodIndependentEntropy(user,this.userToPosts.get(user), lifetimes.get(user));
                    if(stageEntropies.size() == 20) {
                        String vector = this.convertToStringVector(stageEntropies);
                        buffer.append(user + "\t" + vector + "\n");
                    }
                }
            }
            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "_indegree_entropies_stages.tsv");
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

        // get the set of replies to the user's posts
        HashSet<String> repliesToUserPosts = new HashSet<String>();
        for (String post : posts) {
            if(originalToReplies.containsKey(post))
                repliesToUserPosts.addAll(originalToReplies.get(post));
        }

        // derive the indegree distribution over the time period
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Interval interval = intervals.get(startInterval);
            Date endInterval = interval.getEndInterval();

            // tally the replies from users within the time period
            HashMap<String,Integer> userToReplyFreq = new HashMap<String, Integer>();
            double replyCount = 0;
            for (String post : repliesToUserPosts) {
                if(postToDate.containsKey(post)) {
                    Date postDate = postToDate.get(post);
                    if((postDate.equals(startInterval) || postDate.after(startInterval)) && (postDate.before(endInterval))) {
                        String replierID = postToUser.get(post);
                        if(userToReplyFreq.containsKey(replierID)) {
                            int tally = userToReplyFreq.get(replierID);
                            tally++;
                            userToReplyFreq.put(replierID,tally);
                        } else {
                            userToReplyFreq.put(replierID,1);
                        }
                        replyCount++;
                    }
                }
            }

            // work out the probability distribution for the time period
            HashMap<String,Double> userToReplyProb = new HashMap<String, Double>();
            for (String userA : userToReplyFreq.keySet()) {
                double userReplyFreq = userToReplyFreq.get(userA);
                double userReplyProb = userReplyFreq / replyCount;
                userToReplyProb.put(userA,userReplyProb);
            }

            // create the prob dist object
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(user,intervalCount,userToReplyProb);
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
            HashSet<String> trainingUsers = retriever.getTrainingUsers(DB);

            // get the lifeperods of each user
            LifeTimeExtractor extractor = new LifeTimeExtractor();
            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(DB);

            // derive the activity proportion for each user across his stages
            StringBuffer buffer = new StringBuffer();
            for (String user : trainingUsers) {
                // the user may not have initiated anything, hence only call the function if he has
                if(userToPosts.containsKey(user)) {
                    TreeMap<Integer,Double> stageEntropies = this.deriveCrossPeriodEntropies(user, this.userToPosts.get(user), lifetimes.get(user));
                    if(stageEntropies.size() == 20) {
                        String vector = this.convertToStringVector(stageEntropies);
                        buffer.append(user + "\t" + vector + "\n");
                    }
                }
            }
            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "_indegree_user_crossentropies_stages.tsv");
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

        // get the set of replies to the user's posts
        HashSet<String> repliesToUserPosts = new HashSet<String>();
        for (String post : posts) {
            if(originalToReplies.containsKey(post))
                repliesToUserPosts.addAll(originalToReplies.get(post));
        }

        // derive the indegree distribution over the time period and persist them
        TreeMap<Integer,DiscreteProbabilityDistribution> stageDistributions = new TreeMap<Integer, DiscreteProbabilityDistribution>();
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Interval interval = intervals.get(startInterval);
            Date endInterval = interval.getEndInterval();

            // tally the replies from users within the time period
            HashMap<String,Integer> userToReplyFreq = new HashMap<String, Integer>();
            double replyCount = 0;
            for (String post : repliesToUserPosts) {
                if(postToDate.containsKey(post)) {
                    Date postDate = postToDate.get(post);
                    if((postDate.equals(startInterval) || postDate.after(startInterval)) && (postDate.before(endInterval))) {
                        String replierID = postToUser.get(post);
                        if(userToReplyFreq.containsKey(replierID)) {
                            int tally = userToReplyFreq.get(replierID);
                            tally++;
                            userToReplyFreq.put(replierID,tally);
                        } else {
                            userToReplyFreq.put(replierID,1);
                        }
                        replyCount++;
                    }
                }
            }

            // work out the probability distribution for the time period
            HashMap<String,Double> userToReplyProb = new HashMap<String, Double>();
            for (String userA : userToReplyFreq.keySet()) {
                double userReplyFreq = userToReplyFreq.get(userA);
                double userReplyProb = userReplyFreq / replyCount;
                userToReplyProb.put(userA,userReplyProb);
            }

            // create the prob dist object
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(user,intervalCount,userToReplyProb);
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
            HashSet<String> trainingUsers = retriever.getTrainingUsers(DB);

            // get the lifeperods of each user
            LifeTimeExtractor extractor = new LifeTimeExtractor();
            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(DB);

            // derive the activity proportion for each user across his stages
            StringBuffer buffer = new StringBuffer();
            double totalUsers = trainingUsers.size();
            double count = 1;
            for (String user : trainingUsers) {
                // the user may not have initiated anything, hence only call the function if he has
                if(userToPosts.containsKey(user)) {
                    TreeMap<Integer,Double> stageEntropies = this.deriveCommunityPeriodEntropies(user,this.userToPosts.get(user), lifetimes.get(user));
                    if(stageEntropies.size() == 20) {
                        String vector = this.convertToStringVector(stageEntropies);
                        buffer.append(user + "\t" + vector + "\n");
                    }

                    double soFar = (count / totalUsers) * 100;
                    System.out.println("soFar = " + soFar);
                    count++;
                }
            }
            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "_indegree_community_crossentropies_stages.tsv");
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

        // get the set of replies to the user's posts
        HashSet<String> repliesToUserPosts = new HashSet<String>();
        for (String post : posts) {
            if(originalToReplies.containsKey(post))
                repliesToUserPosts.addAll(originalToReplies.get(post));
        }

        // derive the indegree distribution over the time period
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Interval interval = intervals.get(startInterval);
            Date endInterval = interval.getEndInterval();

            // tally the replies from users within the time period
            HashMap<String,Integer> userToReplyFreq = new HashMap<String, Integer>();
            double replyCount = 0;
            for (String post : repliesToUserPosts) {
                if(postToDate.containsKey(post)) {
                    Date postDate = postToDate.get(post);
                    if((postDate.equals(startInterval) || postDate.after(startInterval)) && (postDate.before(endInterval))) {
                        String replierID = postToUser.get(post);
                        if(userToReplyFreq.containsKey(replierID)) {
                            int tally = userToReplyFreq.get(replierID);
                            tally++;
                            userToReplyFreq.put(replierID,tally);
                        } else {
                            userToReplyFreq.put(replierID,1);
                        }
                        replyCount++;
                    }
                }
            }

            // work out the probability distribution for the time period
            HashMap<String,Double> userToReplyProb = new HashMap<String, Double>();
            for (String userA : userToReplyFreq.keySet()) {
                double userReplyFreq = userToReplyFreq.get(userA);
                double userReplyProb = userReplyFreq / replyCount;
                userToReplyProb.put(userA,userReplyProb);
            }

            // create the prob dist object for the user
            DiscreteProbabilityDistribution pd1 = new DiscreteProbabilityDistribution(user,intervalCount,userToReplyProb);

            // work out the global probability distribution
            // get the replies that were posted within the window
            HashMap<String,Integer> windowRepliedFromFreq = new HashMap<String, Integer>();
            double globalReplyCount = 0;
            for (String post : postToDate.keySet()) {
                if(replyToOriginal.containsKey(post)) {
                    if(postToDate.containsKey(post)) {
                        Date postDate = postToDate.get(post);
                        // check that the reply was in the window
                        if((postDate.after(startInterval) || postDate.equals(startInterval)) && (postDate.before(endInterval))) {
                            // tally how many times the user has been replied to within the window
                            String repliedFromId = postToUser.get(post);
                            if(windowRepliedFromFreq.containsKey(repliedFromId)) {
                                int tally = windowRepliedFromFreq.get(repliedFromId);
                                tally++;
                                windowRepliedFromFreq.put(repliedFromId,tally);
                            } else {
                                windowRepliedFromFreq.put(repliedFromId,1);
                            }
                            globalReplyCount++;
                        }
                    }
                }
            }
            // work out the probability of the window users receiving a reply
            HashMap<String,Double> windowRepliedFromProb = new HashMap<String, Double>();
            for (String repledFromId : windowRepliedFromFreq.keySet()) {
                double freq = windowRepliedFromFreq.get(repledFromId);
                double prob = freq / globalReplyCount;
                windowRepliedFromProb.put(repledFromId,prob);
            }
            // set the discrete probability distribution object
            DiscreteProbabilityDistribution pd2 = new DiscreteProbabilityDistribution("global",intervalCount,windowRepliedFromProb);

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

//        IndegreeDistribution indegreeDistribution = new IndegreeDistribution();
//
//        indegreeDistribution.deriveEntropyPerStageDistributions();
//        indegreeDistribution.deriveCrossEntropyPerStageDistributions();
//        indegreeDistribution.deriveCommunityDependentStageDistributions();



    }

}
