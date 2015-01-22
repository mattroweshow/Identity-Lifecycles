package uk.ac.lancs.socialcomp.identity.parallelised;

import uk.ac.lancs.socialcomp.distribution.DiscreteProbabilityDistribution;
import uk.ac.lancs.socialcomp.distribution.DistributionMeasurer;
import uk.ac.lancs.socialcomp.identity.statistics.Interval;
import uk.ac.lancs.socialcomp.identity.statistics.Lifetime;
import uk.ac.lancs.socialcomp.identity.statistics.LifetimeStageDeriver;
import uk.ac.lancs.socialcomp.identity.statistics.UtilFunctions;
import uk.ac.lancs.socialcomp.prediction.features.FeatureProperties;
import uk.ac.lancs.socialcomp.tools.text.StringTokeniser;

import java.util.*;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 21/05/2013 / 13:41
 */
public class ParallelTermDistribution implements ParallelDistribution, FeatureProperties {

    String platform;
    String split;
    int k;

    HashMap<String,Date> postToDate;
    HashMap<String,HashSet<String>> userToPosts;
    HashMap<String,String> postToContent;

    public ParallelTermDistribution(String platform, String split, int k, HashMap<String, Date> postToDate, HashMap<String, HashSet<String>> userToPosts, HashMap<String, String> postToContent) {
        this.platform = platform;
        this.split = split;
        this.k = k;
        this.postToDate = postToDate;
        this.userToPosts = userToPosts;
        this.postToContent = postToContent;
    }

    @Override
    public JobResult derivePerStageEntropies(String userid, Lifetime lifetime) {
        TreeMap<Integer,Double> stageEntropies = new TreeMap<Integer, Double>();

        // split the lifetime up into 20 equal sized stages
        LifetimeStageDeriver lifetimeStageDeriver = new LifetimeStageDeriver(lifetime, postToDate);
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(k);

        // get the posts of the user
        HashSet<String> posts = userToPosts.get(userid);

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
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(userid,intervalCount,termToProb);
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
                LEXICAL,
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
            DiscreteProbabilityDistribution pd = new DiscreteProbabilityDistribution(userid,intervalCount,termToProb);
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
                LEXICAL,
                HISTORICALENTROPY,
                outputValue,
                true);
        return jobResult;
    }

    @Override
    public JobResult deriveCommunityEntropies(String userid, Lifetime lifetime) {
        TreeMap<Integer,Double> stageCrossEntropies = new TreeMap<Integer, Double>();

        // split the lifetime up into k equal sized stages
        LifetimeStageDeriver lifetimeStageDeriver = new LifetimeStageDeriver(lifetime, postToDate);
        TreeMap<Date,Interval> intervals = lifetimeStageDeriver.deriveStageIntervals(k);

        // get the posts of the user
        HashSet<String> posts = userToPosts.get(userid);

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
            DiscreteProbabilityDistribution pd1 = new DiscreteProbabilityDistribution(userid,intervalCount,termToProb);

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

        // create a JobResult from the entropies
        String outputValue =  UtilFunctions.convertToStringVector(stageCrossEntropies);
        JobResult jobResult = new JobResult(platform,
                split,
                k,
                userid,
                LEXICAL,
                COMMUNITYENTROPY,
                outputValue,
                true);
        return jobResult;
    }


}
