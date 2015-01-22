package uk.ac.lancs.socialcomp.prediction.features;

import uk.ac.lancs.socialcomp.datasets.DatasetRetriever;
import uk.ac.lancs.socialcomp.identity.statistics.ChurnerExtractor;
import uk.ac.lancs.socialcomp.prediction.models.PredictionTasks;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2014 / 17:18
 */
public class DatasetBuilder implements PredictionTasks {

    String platform;
    int k;
    int predictionMode;

    public DatasetBuilder(String platform, int k, int predictionMode) {
        this.platform = platform;
        this.k = k;
        this.predictionMode = predictionMode;
    }

    public Dataset buildTrainingData() {

        int[] dynamics = {1,2,3};
        int[] entropyMeasures = {1,2,3};

//        System.out.println("-Getting Features");
        FeatureBuilder builder =  new FeatureBuilder(platform, "train", k);

        // get the static features
        HashMap<String,ArrayList<Feature>> userToStaticFeatures = new HashMap<String, ArrayList<Feature>>();
        // get the rate features
        HashMap<String,ArrayList<Feature>> userToRateFeatures = new HashMap<String, ArrayList<Feature>>();


        for (int dynamic : dynamics) {
//            System.out.println("Dynamic=" + dynamic);
            for (int entropyMeasure : entropyMeasures) {
//                System.out.println("EntropyMeasure=" + entropyMeasure);

//                System.out.println("Static");
                HashMap<String,ArrayList<Feature>> innerStaticFeatures = builder.getStaticFeatures(dynamic,entropyMeasure);
//                System.out.println("Rate");
                HashMap<String,ArrayList<Feature>> innerRateFeatures = builder.getRateFeatures(dynamic,entropyMeasure);

                // update the outer map with the nwe inner features
                userToStaticFeatures = updateOuterFeatureMap(userToStaticFeatures,innerStaticFeatures);
                userToRateFeatures = updateOuterFeatureMap(userToRateFeatures,innerRateFeatures);
            }
        }

        // get the list of users within the training split
//        System.out.println("-Getting users");
        DatasetRetriever retriever = new DatasetRetriever();
        HashSet<String> datasetUsers = retriever.getTrainingUsers(platform);

        // get the response variables for the users in the training split and depending on the task
//        System.out.println("-Getting user response variables");
        HashMap<String,Double> userToResponse = getResponseVariables(datasetUsers);

        // build the instances from each user's features
//        System.out.println("-Building instances");
        Instance[] instances = buildInstances(userToStaticFeatures,userToRateFeatures,userToResponse,"train");

        // build the dataset based on these instances
//        System.out.println("-Constructing the dataset");
        Dataset dataset = new Dataset(platform,"train",k,instances);

        return dataset;
    }

    public Dataset buildTestingData() {

        int[] dynamics = {1,2,3};
        int[] entropyMeasures = {1,2,3};

//        System.out.println("-Getting Features");
        FeatureBuilder builder = new FeatureBuilder(platform, "test", k);

        // get the static features
        HashMap<String,ArrayList<Feature>> userToStaticFeatures = new HashMap<String, ArrayList<Feature>>();
        // get the rate features
        HashMap<String,ArrayList<Feature>> userToRateFeatures = new HashMap<String, ArrayList<Feature>>();


        for (int dynamic : dynamics) {
//            System.out.println("Dynamic=" + dynamic);
            for (int entropyMeasure : entropyMeasures) {
//                System.out.println("EntropyMeasure=" + entropyMeasure);

//                System.out.println("Static");
                HashMap<String,ArrayList<Feature>> innerStaticFeatures = builder.getStaticFeatures(dynamic,entropyMeasure);
//                System.out.println("Rate");
                HashMap<String,ArrayList<Feature>> innerRateFeatures = builder.getRateFeatures(dynamic,entropyMeasure);

                // update the outer map with the new inner features
                userToStaticFeatures = updateOuterFeatureMap(userToStaticFeatures,innerStaticFeatures);
                userToRateFeatures = updateOuterFeatureMap(userToRateFeatures,innerRateFeatures);
            }
        }

        // get the list of users within the training split
//        System.out.println("-Getting users");
        DatasetRetriever retriever = new DatasetRetriever();
        HashSet<String> datasetUsers = retriever.getTestingUsers(platform);

//        System.out.println("-Getting user response variables");
        // get the response variables for the users in the training split and depending on the task
        HashMap<String,Double> userToResponse = getResponseVariables(datasetUsers);

        // build the instances from each user's features
//        System.out.println("-Building instances");
        Instance[] instances = buildInstances(userToStaticFeatures,userToRateFeatures,userToResponse,"test");

        // build the dataset based on these instances
//        System.out.println("-Constructing the dataset");
        Dataset dataset = new Dataset(platform,"test",k,instances);

        return dataset;

    }

    private Instance[] buildInstances(HashMap<String,ArrayList<Feature>> staticFeatures,
                                      HashMap<String,ArrayList<Feature>> rateFeatures,
                                      HashMap<String,Double> responseMap,
                                      String split) {

        // go through and ensure that users are in all three maps
        ArrayList<Instance> instances = new ArrayList<Instance>();
        for (String userid : staticFeatures.keySet()) {
//            System.out.println("In rate = " + rateFeatures.containsKey(userid) + " | In response = " + responseMap.containsKey(userid));
            if(rateFeatures.containsKey(userid) && responseMap.containsKey(userid)) {

                ArrayList<Feature> userStaticFeatures = staticFeatures.get(userid);
                ArrayList<Feature> userRateFeatures = rateFeatures.get(userid);
                ArrayList<Feature> userAllFeatures = userStaticFeatures;
                userAllFeatures.addAll(userRateFeatures);
                double response = responseMap.get(userid);

                Instance instance = new Instance(userid,platform,split,userAllFeatures,response);
                instances.add(instance);
            }
        }

        // convert to a numeric map
        Instance[] instancesArray = new Instance[instances.size()];
        for (int i = 0; i < instances.size(); i++) {
              instancesArray[i] = instances.get(i);
        }

        // write the list of users for the given split and lifecycle fidelity to a file
        StringBuffer buffer = new StringBuffer();
        for (Instance instance : instances) {
            buffer.append(instance.getUserid() + "\n");
        }
        try {
            // write this to a file
            PrintWriter writer = new PrintWriter("data/datasets/" + platform + "/" + split + "_" + k + "_split.tsv");
            writer.write(buffer.toString());
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return instancesArray;
    }

    /*
     * Get the response variables for the given split's users
     */
    public HashMap<String,Double> getResponseVariables(HashSet<String> users) {
        HashMap<String,Double> userToResponses = new HashMap<String, Double>();

        switch(predictionMode) {
            case DETECTION:
                // get the churner map
                ChurnerExtractor extractor = new ChurnerExtractor(platform);
                HashMap<String,Double> userToLabels = extractor.getUserToChurnOrNonChurnLabels();
                for (String user : users) {
                    if(userToLabels.containsKey(user))
                        userToResponses.put(user,userToLabels.get(user));
                }
                break;
            case FORECASTING:
                //TODO: include function to get the churn point (in days) for each churner
                break;
        }

        return userToResponses;
    }

    private HashMap<String,ArrayList<Feature>> updateOuterFeatureMap(HashMap<String,ArrayList<Feature>> outerMap,
                                                                     HashMap<String,ArrayList<Feature>> innerMap) {
        HashMap<String,ArrayList<Feature>> returnMap = new HashMap<String, ArrayList<Feature>>();

        // if nothing has been mapped before then just return the inner map
        if(outerMap.size() == 0) {
            returnMap = innerMap;

        // otherwise do the mapping
        } else {
            returnMap = outerMap;
            for (String userid : innerMap.keySet()) {
                ArrayList<Feature> innerUserFeatures = innerMap.get(userid);
                if(outerMap.containsKey(userid)) {
                    ArrayList<Feature> outerUserFeatures = outerMap.get(userid);
                    outerUserFeatures.addAll(innerUserFeatures);
                    outerMap.put(userid,outerUserFeatures);
                }
            }
        }

        return returnMap;
    }

    public static void main(String[] args) {

        String[] platforms = {"facebook", "sap", "serverfault","boards"};
        int[] ks = {5,10,20};

        int mode = 1;   // detection at present
//        int mode = 2;   // forecasting next


        for (String platform : platforms) {
            System.out.println("\n" + platform);
            for (int k : ks) {
                System.out.println("k=" + k);
                DatasetBuilder builder = new DatasetBuilder(platform, k, mode);
                Dataset training = builder.buildTrainingData();
                System.out.println("Training = " + training.getInstances().length);
                Dataset testing = builder.buildTestingData();
                System.out.println("Testing = " + testing.getInstances().length);

            }
        }

    }
}
