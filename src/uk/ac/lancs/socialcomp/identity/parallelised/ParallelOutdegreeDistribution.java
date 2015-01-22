package uk.ac.lancs.socialcomp.identity.parallelised;

import uk.ac.lancs.socialcomp.distribution.DiscreteProbabilityDistribution;
import uk.ac.lancs.socialcomp.distribution.DistributionMeasurer;
import uk.ac.lancs.socialcomp.identity.statistics.Interval;
import uk.ac.lancs.socialcomp.identity.statistics.Lifetime;
import uk.ac.lancs.socialcomp.identity.statistics.LifetimeStageDeriver;
import uk.ac.lancs.socialcomp.identity.statistics.UtilFunctions;
import uk.ac.lancs.socialcomp.prediction.features.FeatureProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 31/07/2014 / 13:57
 */
public class ParallelOutdegreeDistribution implements ParallelDistribution, FeatureProperties {

    String platform;
    String split;
    int k;

    HashMap<String,Date> postToDate;
    HashMap<String,HashSet<String>> userToPosts;
    HashMap<String,String> postToUser;
    HashMap<String,String> replyToOriginal;

    public ParallelOutdegreeDistribution(String platform, String split, int k,
                                         HashMap<String, Date> postToDate,
                                         HashMap<String, HashSet<String>> userToPosts,
                                         HashMap<String, String> postToUser,
                                         HashMap<String, String> replyToOriginal) {
        this.platform = platform;
        this.split = split;
        this.k = k;
        this.postToDate = postToDate;
        this.userToPosts = userToPosts;
        this.postToUser = postToUser;
        this.replyToOriginal = replyToOriginal;
    }

    @Override
    public JobResult derivePerStageEntropies(String userid, Lifetime lifetime) {
        TreeMap<Integer,Double> stageEntropies = new TreeMap<Integer, Double>();

        // split the lifetime up into 20 equal sized stages
        LifetimeStageDeriver lifetimeStageDeriver = new LifetimeStageDeriver(lifetime, postToDate);
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(k);

        // get the posts of the user
        HashSet<String> posts = userToPosts.get(userid);

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
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(userid,intervalCount,userToRepliedToProb);
            // work out the entropy of the distribution
            DistributionMeasurer measurer = new DistributionMeasurer();
            double entropy = measurer.measureEntropy(pd);

            // map the interval to its probability
            stageEntropies.put(intervalCount,entropy);
            intervalCount++;
        }

        // create a JobResult from the entropies
        String outputValue =  UtilFunctions.convertToStringVector(stageEntropies);
        JobResult jobResult = new JobResult(platform,
                split,
                k,
                userid,
                OUTDEGREE,
                PERIODENTROPY,
                outputValue,
                true);
        return jobResult;

    }

    @Override
    public JobResult deriveHistoricalEntropies(String userid, Lifetime lifetime) {
        TreeMap<Integer,Double> stageEntropies = new TreeMap<Integer, Double>();

        // split the lifetime up into 20 equal sized stages
        LifetimeStageDeriver lifetimeStageDeriver = new LifetimeStageDeriver(lifetime, postToDate);
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(k);

        // get the user's posts
        HashSet<String> posts = userToPosts.get(userid);

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
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(userid,intervalCount,userToRepliedToProb);
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

        // create a JobResult from the entropies
        String outputValue =  UtilFunctions.convertToStringVector(stageEntropies);
        JobResult jobResult = new JobResult(platform,
                split,
                k,
                userid,
                OUTDEGREE,
                HISTORICALENTROPY,
                outputValue,
                true);
        return jobResult;
    }

    @Override
    public JobResult deriveCommunityEntropies(String userid, Lifetime lifetime) {
        TreeMap<Integer,Double> stageCrossEntropies = new TreeMap<Integer, Double>();

        // split the lifetime up into 20 equal sized stages
        LifetimeStageDeriver lifetimeStageDeriver = new LifetimeStageDeriver(lifetime, postToDate);
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(k);

        // get the user's posts
        HashSet<String> posts = userToPosts.get(userid);

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
            DiscreteProbabilityDistribution pd1 = new DiscreteProbabilityDistribution(userid,intervalCount,userToRepliedToProb);

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

        // create a JobResult from the entropies
        String outputValue =  UtilFunctions.convertToStringVector(stageCrossEntropies);
        JobResult jobResult = new JobResult(platform,
                split,
                k,
                userid,
                OUTDEGREE,
                COMMUNITYENTROPY,
                outputValue,
                true);
        return jobResult;


    }
}
