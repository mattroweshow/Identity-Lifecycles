package uk.ac.lancs.socialcomp.identity.lexical;

import uk.ac.lancs.socialcomp.datasets.DatasetRetriever;
import uk.ac.lancs.socialcomp.distribution.DiscreteProbabilityDistribution;
import uk.ac.lancs.socialcomp.distribution.DistributionMeasurer;
import uk.ac.lancs.socialcomp.identity.statistics.Interval;
import uk.ac.lancs.socialcomp.identity.statistics.LifeTimeExtractor;
import uk.ac.lancs.socialcomp.identity.statistics.Lifetime;
import uk.ac.lancs.socialcomp.identity.statistics.LifetimeStageDeriver;
import uk.ac.lancs.socialcomp.io.Database;
import uk.ac.lancs.socialcomp.io.QueryGrabber;
import uk.ac.lancs.socialcomp.tools.text.StringTokeniser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 21/05/2013 / 13:41
 */
public class TermDistribution {

    String DB;
    String split;
    int k;

    HashMap<String,Date> postToDate;
    HashMap<String,HashSet<String>> userToPosts;
    HashMap<String,String> postToUser;
    HashMap<String,String> postToContent;

    public TermDistribution(String db, String split, int k) {
        this.DB = db;
        this.split = split;
        this.k = k;

        try {
            System.out.println("-Loading data into memory");

            // get the SQL query that is to be run in order to retrieve the user info
            String query = QueryGrabber.getQuery(DB, "getAllPosts");
            Connection connection = Database.getConnection(DB);

            userToPosts = new HashMap<String, HashSet<String>>();
            postToDate = new HashMap<String, Date>();
            postToUser = new HashMap<String, String>();
            postToContent = new HashMap<String, String>();

            // query the db
            Statement statement = connection.createStatement();
            statement.setFetchSize(100);    // restrict the fetch size in order to scale the querying

            // get the churn point cutoff - new code to ensure that we are predicting churners from analysis point
            Properties properties = new Properties();
            properties.load(new FileInputStream("data/properties/" + DB + "-stats.properties"));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date cutoff = sdf.parse(properties.getProperty("churn_cutoff"));


            ResultSet results = statement.executeQuery(query);
            while(results.next())  {
                String postid = results.getString("messageuri");
                Date postDate = new Date(results.getTimestamp("created").getTime());
                String userid = results.getString("contributor");
                String content = results.getString("content");

                // insert into the maps if the postdate appears before the cutoff
                if(postDate.equals(cutoff) || postDate.before(cutoff)) {
                    if(userToPosts.containsKey(userid)) {
                        HashSet<String> posts = userToPosts.get(userid);
                        posts.add(postid);
                        userToPosts.put(userid,posts);
                    } else {
                        HashSet<String> posts = new HashSet<String>();
                        posts.add(postid);
                        userToPosts.put(userid,posts);
                    }
                    postToDate.put(postid, postDate);
                    postToUser.put(postid,userid);
                    postToContent.put(postid,content);
                }
            }
            statement.close();
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
//            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(DB);
            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(this.postToDate,this.userToPosts);

            // derive the term distribution across each stage
            StringBuffer buffer = new StringBuffer();
            for (String user : users) {
                // the user may not have initiated anything, hence only call the function if he has
                if(userToPosts.containsKey(user)) {
                    try {
                        if(userToPosts.get(user).size() >= (2*k)) {
                            TreeMap<Integer,Double> stageEntropies = this.derivePeriodIndependentEntropy(user,this.userToPosts.get(user), lifetimes.get(user));
                            String vector = this.convertToStringVector(stageEntropies);
                            buffer.append(user + "\t" + vector + "\n");
                        }
                    } catch(Exception e) {
                    }
                }
            }
            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "/" + DB + "_" + split + "_lexical_entropies_stages_" + k + ".tsv");
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
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(k);

        // Derive the distribution of terms in each interval
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Interval interval = intervals.get(startInterval);
            Date endInterval = interval.getEndInterval();

            HashSet<String> intervalPosts = new HashSet<String>();
            for (String post : posts) {
                Date postDate = postToDate.get(post);
                if((postDate.after(startInterval) || postDate.equals(startInterval)) && postDate.before(endInterval)) {
                    intervalPosts.add(post);
                }
            }

            // work out the probability distribution for the time period
            HashMap<String,Integer> termToTally = new HashMap<String, Integer>();
            double totalTally = 0;
            for (String post : intervalPosts) {
                // get the token bag for the post
                String[] tokens = StringTokeniser.tokenise(postToContent.get(post));

                for (String token : tokens) {
                    if(termToTally.containsKey(token)) {
                        int tally = termToTally.get(token);
                        tally++;
                        termToTally.put(token,tally);
                    } else {
                        termToTally.put(token,1);
                    }
                    totalTally++;
                }
            }
            // work out the probability distribution of the terms
            HashMap<String,Double> termToProb = new HashMap<String, Double>();
            for (String term : termToTally.keySet()) {
                double tally = termToTally.get(term);
                double prob = tally / totalTally;
                termToProb.put(term,prob);
            }

            // create the prob dist object
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(user,intervalCount,termToProb);
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
//            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(DB);
            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(this.postToDate,this.userToPosts);

            // derive the activity proportion for each user across his stages
            StringBuffer buffer = new StringBuffer();
            for (String user : users) {
                // the user may not have initiated anything, hence only call the function if he has
                if(userToPosts.containsKey(user)) {
                    try {
                        if(userToPosts.get(user).size() >= (2*k)) {
                            TreeMap<Integer,Double> stageEntropies = this.deriveCrossPeriodEntropies(user, this.userToPosts.get(user), lifetimes.get(user));
                            String vector = this.convertToStringVector(stageEntropies);
                            buffer.append(user + "\t" + vector + "\n");
                        }
                    } catch(Exception e) {
                    }
                }
            }
            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "/" + DB + "_" + split + "_lexical_user_crossentropies_stages_" + k + ".tsv");
            writer.write(buffer.toString());
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Derives the cross period entropie (i.e. comparing the previous lifecycle stage with the current one
     */
    private TreeMap<Integer,Double> deriveCrossPeriodEntropies(String user,HashSet<String> posts, Lifetime lifetime) {
        TreeMap<Integer,Double> stageEntropies = new TreeMap<Integer, Double>();

        // split the lifetime up into 20 equal sized stages
        LifetimeStageDeriver lifetimeStageDeriver = new LifetimeStageDeriver(lifetime, postToDate);
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(k);

        // derive the term distribution over the time period and persist them
        TreeMap<Integer,DiscreteProbabilityDistribution> stageDistributions = new TreeMap<Integer, DiscreteProbabilityDistribution>();
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Interval interval = intervals.get(startInterval);
            Date endInterval = interval.getEndInterval();

            HashSet<String> intervalPosts = new HashSet<String>();
            for (String post : posts) {
                Date postDate = postToDate.get(post);
                if((postDate.after(startInterval) || postDate.equals(startInterval)) && postDate.before(endInterval)) {
                    intervalPosts.add(post);
                }
            }

            // work out the probability distribution for the time period
            HashMap<String,Integer> termToTally = new HashMap<String, Integer>();
            double totalTally = 0;
            for (String post : intervalPosts) {
                // get the token bag for the post
                String[] tokens = StringTokeniser.tokenise(postToContent.get(post));

                for (String token : tokens) {
                    if(termToTally.containsKey(token)) {
                        int tally = termToTally.get(token);
                        tally++;
                        termToTally.put(token,tally);
                    } else {
                        termToTally.put(token,1);
                    }
                    totalTally++;
                }
            }
            // work out the probability distribution of the terms
            HashMap<String,Double> termToProb = new HashMap<String, Double>();
            for (String term : termToTally.keySet()) {
                double tally = termToTally.get(term);
                double prob = tally / totalTally;
                termToProb.put(term,prob);
            }

            // create the prob dist object
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(user,intervalCount,termToProb);
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
//            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(DB);
            HashMap<String,Lifetime> lifetimes = extractor.deriveLifetimeMap(this.postToDate,this.userToPosts);

            // derive the activity proportion for each user across his stages
            StringBuffer buffer = new StringBuffer();
            double totalUsers = users.size();
            double count = 1;
            for (String user : users) {
                // the user may not have initiated anything, hence only call the function if he has
                if(userToPosts.containsKey(user)) {
                    try {
                        if(userToPosts.get(user).size() >= (2*k)) {
                            TreeMap<Integer,Double> stageEntropies = this.deriveCommunityPeriodEntropies(user, this.userToPosts.get(user), lifetimes.get(user));
                            String vector = this.convertToStringVector(stageEntropies);
                            buffer.append(user + "\t" + vector + "\n");
                        }
                    } catch (Exception e) {
                    }
                }
                double soFar = (count / totalUsers) * 100;
                System.out.println("soFar = " + soFar);
                count++;
            }
            // write the whole thing to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "/" + DB + "_" + split + "_lexical_community_crossentropies_stages_" + k + ".tsv");
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
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(k);

        // derive the term distribution of the user at each time step and compare this to the community's distribution at the same time interval
        int intervalCount = 0;
        for (Date startInterval : intervals.keySet()) {
            Interval interval = intervals.get(startInterval);
            Date endInterval = interval.getEndInterval();

            HashSet<String> intervalPosts = new HashSet<String>();
            for (String post : posts) {
                Date postDate = postToDate.get(post);
                if((postDate.after(startInterval) || postDate.equals(startInterval)) && postDate.before(endInterval)) {
                    intervalPosts.add(post);
                }
            }

            // work out the probability distribution for the time period
            HashMap<String,Integer> termToTally = new HashMap<String, Integer>();
            double totalTally = 0;
            for (String post : intervalPosts) {
                // get the token bag for the post
                String[] tokens = StringTokeniser.tokenise(postToContent.get(post));

                for (String token : tokens) {
                    if(termToTally.containsKey(token)) {
                        int tally = termToTally.get(token);
                        tally++;
                        termToTally.put(token,tally);
                    } else {
                        termToTally.put(token,1);
                    }
                    totalTally++;
                }
            }
            // work out the probability distribution of the terms
            HashMap<String,Double> termToProb = new HashMap<String, Double>();
            for (String term : termToTally.keySet()) {
                double tally = termToTally.get(term);
                double prob = tally / totalTally;
                termToProb.put(term,prob);
            }

            // create the prob dist object
            DiscreteProbabilityDistribution pd1 = new DiscreteProbabilityDistribution(user,intervalCount,termToProb);

            // work out the term probability distribution globally
            // get the posts that were published globally during the time window
            HashSet<String> globalIntervalPosts = new HashSet<String>();
            for (String post : postToDate.keySet()) {
                Date postDate = postToDate.get(post);
                if((postDate.after(startInterval) || postDate.equals(startInterval)) && postDate.before(endInterval)) {
                    globalIntervalPosts.add(post);
                }
            }
            // work out the probability distribution of the global term frequencies
            double globalTotalTally = 0;
            HashMap<String,Integer> globalTermToTally = new HashMap<String, Integer>();
            for (String post : globalIntervalPosts) {
                String[] tokens = StringTokeniser.tokenise(postToContent.get(post));

                for (String token : tokens) {
                    if(globalTermToTally.containsKey(token)) {
                        int tally = globalTermToTally.get(token);
                        tally++;
                        globalTermToTally.put(token,tally);
                    } else {
                        globalTermToTally.put(token,1);
                    }
                    globalTotalTally++;
                }
            }
            // work out the probability distribution of the terms
            HashMap<String,Double> globalTermToProb = new HashMap<String, Double>();
            for (String term : globalTermToTally.keySet()) {
                double tally = globalTermToTally.get(term);
                double prob = tally / globalTotalTally;
                globalTermToProb.put(term,prob);
            }
            // create the global probability distribution object
            DiscreteProbabilityDistribution pd2 = new DiscreteProbabilityDistribution("global", intervalCount, globalTermToProb);

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

//        String[] platforms = {"facebook", "serverfault", "sap"};
        String[] platforms = {"sap"};
        String[] splits = {"train","test"};
//        String split = "train";
//        String split = "test";

        // test different numbers of lifecycle stages
        int[] ks = {5,10,20};

        for (String platform : platforms) {
            System.out.println("\nProceessing: " + platform);
            for (String split : splits) {
                System.out.println("Split: " + split);
                for (int k : ks) {
                    System.out.println(k);
                    TermDistribution termDistribution = new TermDistribution(platform, split, k);
                    termDistribution.deriveEntropyPerStageDistributions();
                    termDistribution.deriveCrossEntropyPerStageDistributions();
                    termDistribution.deriveCommunityDependentStageDistributions();
                }
            }
        }


    }
}
