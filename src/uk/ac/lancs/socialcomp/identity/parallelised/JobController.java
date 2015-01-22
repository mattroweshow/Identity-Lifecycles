package uk.ac.lancs.socialcomp.identity.parallelised;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import uk.ac.lancs.socialcomp.identity.statistics.LifeTimeExtractor;
import uk.ac.lancs.socialcomp.identity.statistics.Lifetime;
import uk.ac.lancs.socialcomp.prediction.features.FeatureProperties;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 31/07/2014 / 11:07
 */
public class JobController implements FeatureProperties {

    public boolean dataLoaded;

    String platform;

    HashMap<String,Date> postToDate;
    HashMap<String,HashSet<String>> userToPosts;
    HashMap<String,String> postToUser;
    HashMap<String,String> postToContent;

    HashMap<String,String> replyToOriginal;
    HashMap<String,HashSet<String>> originalToReplies;

    HashMap<String,Lifetime> lifetimes;

    public JobController(String platform) {
        this.platform = platform;

        // log the load duration
        long start = System.currentTimeMillis();

        // load the data into memory from hdfs
        System.out.println("-Loading data into memory for: " + platform);

        String HDFSHostName = "hdfs://148.88.19.38:9000";
        String filesPath = "/users/hduser/identity/";

        try {
            // initialise the maps
            userToPosts = new HashMap<String, HashSet<String>>();
            postToDate = new HashMap<String, Date>();
            postToUser = new HashMap<String, String>();
            postToContent = new HashMap<String, String>();

            originalToReplies = new HashMap<String, HashSet<String>>();
            replyToOriginal = new HashMap<String, String>();

            // get the churn point cutoff - new code to ensure that we are predicting churners from analysis point
//            Properties properties = new Properties();
            // load this from HDFS
//            properties.load(new FileInputStream("data/properties/" + DB + "-stats.properties"));
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//            Date cutoff = sdf.parse(properties.getProperty("churn_cutoff"));

            // get the churner cutoff point from HDFS
            System.out.println("-Getting churner cutoff point");
            Configuration configuration = new Configuration();
            String churnCutoffPointHDFSFile = HDFSHostName + filesPath + platform + "-stats.properties";
            Path pt = new Path(churnCutoffPointHDFSFile);
            FileSystem fs = FileSystem.get(pt.toUri(), configuration);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(pt)));
            Properties properties = new Properties();
            properties.load(reader);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date cutoff = new Date();
            try {
                cutoff = sdf.parse(properties.getProperty("churn_cutoff").toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("--Churner cutoff for " + platform + " set to: " + sdf.format(cutoff));


            // Read the data from the posts data from the file
            System.out.println("-Loading posts into memory");
            configuration = new Configuration();
            String postsFilePath = HDFSHostName + filesPath + platform + "_posts.tsv";
            pt = new Path(postsFilePath);
            fs = FileSystem.get(pt.toUri(), configuration);
            reader = new BufferedReader(new InputStreamReader(fs.open(pt)));
            String line = null;

            // new date parser for boards data
            // 2008-01-06 10:07:51
            SimpleDateFormat sdfA = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            boolean afterFirst = false;

            while ((line = reader.readLine()) != null) {
                if(afterFirst) {
                    try {
                        // tokenise the line by the tab delimiter
                        String[] toks = line.split("\t");
                        String postid = toks[0];
                        String postDateStr = toks[4];
                        postDateStr = postDateStr.replaceAll("\"","").trim();
                        Date postDate = new Date(sdfA.parse(postDateStr).getTime());
                        String userid = toks[2];
                        String content = toks[5].replaceAll("\"","");

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
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                } else {
                    afterFirst = true;
                }
            }
            reader.close();
            fs.close();
            System.out.println("--Collected #posts: " + postToDate.size());


            // load the reply graph into memory
            System.out.println("-Loading replies into memory");
            configuration = new Configuration();
            String repliesFilePath = HDFSHostName + filesPath + platform + "_replies.tsv";
            pt = new Path(repliesFilePath);
            fs = FileSystem.get(pt.toUri(), configuration);
            reader = new BufferedReader(new InputStreamReader(fs.open(pt)));
            line = null;
            afterFirst = false;
            while ((line = reader.readLine()) != null) {
                if(afterFirst) {
                    try {
                        String[] toks = line.split("\t");
                        String reply = toks[0];
                        String original = toks[1];

                        if(postToDate.containsKey(reply) && postToDate.containsKey(original)) {
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
                    } catch(Exception e) {
                        System.err.println(e.getMessage());
                    }
                } else {
                    afterFirst = true;
                }
            }
            System.out.println("--Collected #replies: " + replyToOriginal.size());


            // derive the lifetimes of all users and map these
            System.out.println("-Generating the lifetimes of all users");
            LifeTimeExtractor extractor = new LifeTimeExtractor(platform);
            lifetimes = extractor.deriveLifetimeMap(this.postToDate,this.userToPosts);
            System.out.println("--Computed #lifetimes: " + lifetimes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // set the dataloaded attribute to true
        dataLoaded = true;

        // output the load duration
        long duration = (System.currentTimeMillis() - start) /1000;
        System.out.println("-Loading took (secs) = " + duration);
    }

    public ArrayList<JobResult> runJob(String userid, String split) {
        // set the preamble for the various feature and measure and fidelity settings
        int[] ks = {5,10,20};
        int[] featureTypes = {1,2,3};
        int[] measureTypes = {1,2,3};

        ArrayList<JobResult> jobResults = new ArrayList<JobResult>();

        // compute features for each lifecycle fidelity setting
        for (int k : ks) {
            System.out.println("\nComputing for fidelity = " + k);
            // check that the user has sufficient posts to compute the features
            // the user may not have initiated anything, hence only call the function if he has
            if(userToPosts.containsKey(userid)) {
                if(userToPosts.get(userid).size() >= (2*k)) {
                    for (int featureType : featureTypes) {
                        System.out.println("Computing feature type: " + featureType);

                        // switch based on the feature type
                        ParallelDistribution distribution = null;
                        switch(featureType) {
                            case INDEGREE:
                                distribution = new ParallelIndegreeDistribution(platform,split,k,
                                        postToDate, userToPosts, postToUser, replyToOriginal, originalToReplies);
                                break;
                            case OUTDEGREE:
                                distribution = new ParallelOutdegreeDistribution(platform,split,k,
                                        postToDate, userToPosts, postToUser, replyToOriginal);
                                break;
                            case LEXICAL:
                                distribution = new ParallelTermDistribution(platform,split,k,
                                        postToDate, userToPosts, postToContent);
                                break;
                        }

                        // now call the method based on the computation that is to be performed
                        for (int measureType : measureTypes) {
                            System.out.println("Computing measure: " + measureType);
                            // switch based on the measure type
                            switch (measureType) {
                                case PERIODENTROPY:
                                    JobResult jobResult = distribution.derivePerStageEntropies(userid, lifetimes.get(userid));
                                    jobResults.add(jobResult);
                                    break;
                                case HISTORICALENTROPY:
                                    JobResult jobResult1 = distribution.deriveHistoricalEntropies(userid,lifetimes.get(userid));
                                    jobResults.add(jobResult1);
                                    break;
                                case COMMUNITYENTROPY:
                                    JobResult jobResult2 = distribution.deriveCommunityEntropies(userid,lifetimes.get(userid));
                                    jobResults.add(jobResult2);
                                    break;
                            }
                        }
                    }

                } else {
                    System.out.println("Not a valid user so skipping the computation: " + userid + " has #posts = " + userToPosts.get(userid));
                }
            } else {
                System.out.println("Not a valid user as he has no posts before the churn cutoff point");
            }
        }

        return jobResults;
    }

}
