package uk.ac.lancs.socialcomp.prediction.models.gaussianSequence;

import uk.ac.lancs.socialcomp.prediction.features.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 18/07/2014 / 14:14
 */
public class GaussianSequenceConstructor implements FeatureProperties {

    /*
     * Builds the Gaussian Sequence from the given dataset
     */
    public GaussianSequence buildSequenceFromDataset(Dataset dataset) {
        GaussianSequence gs = new GaussianSequence();

        // initialise the indexers for the feature ids and labels
        HashMap<Integer,String> featureIDToLabel = new HashMap<Integer, String>();
        HashMap<String,Integer> featureLabelToID = new HashMap<String, Integer>();
        int featureTally = -1;

        // record the churn and non-churner feature values
        HashMap<Integer,ArrayList<Double>> featureIDToChurnFeatureValues = new HashMap<Integer, ArrayList<Double>>();
        HashMap<Integer,ArrayList<Double>> featureIDToNonChurnFeatureValues = new HashMap<Integer, ArrayList<Double>>();


        // go through the instances and engineer the gaussian sequence
        for (Instance instance : dataset.getInstances()) {
            for (Feature feature : instance.getFeatures()) {

                // check that the features are static ones - we are not bothered about rate features here
                if(feature.getType() == STATIC) {

                    // derive the label of the feature
                    String label = feature.getS() + "_" + feature.getDynamic() + "_" + feature.getEntropy();
                    int featureIndex = 0;

                    // get the index if it exists
                    if(featureLabelToID.containsKey(label)) {
                        featureIndex = featureLabelToID.get(label);

                    // if not, then create it and do the mapping
                    } else {
                        featureTally++;
                        featureIndex = featureTally;
                        featureIDToLabel.put(featureIndex,label);
                        featureLabelToID.put(label,featureIndex);
                    }

                    // add the feature value to the appropriate value array
                    if(instance.response == 0) {
                        if(featureIDToNonChurnFeatureValues.containsKey(featureIndex)) {
                            ArrayList<Double> featureValues = featureIDToNonChurnFeatureValues.get(featureIndex);
                            featureValues.add(feature.getValue());
                            featureIDToNonChurnFeatureValues.put(featureIndex,featureValues);
                        } else {
                            ArrayList<Double> featureValues = new ArrayList<Double>();
                            featureValues.add(feature.getValue());
                            featureIDToNonChurnFeatureValues.put(featureIndex,featureValues);
                        }
                    } else {
                        if(featureIDToChurnFeatureValues.containsKey(featureIndex)) {
                            ArrayList<Double> featureValues = featureIDToChurnFeatureValues.get(featureIndex);
                            featureValues.add(feature.getValue());
                            featureIDToChurnFeatureValues.put(featureIndex,featureValues);
                        } else {
                            ArrayList<Double> featureValues = new ArrayList<Double>();
                            featureValues.add(feature.getValue());
                            featureIDToChurnFeatureValues.put(featureIndex,featureValues);
                        }
                    }
                }
            }
        }

        // determine the Gaussian sequence for churners and non-churners
        TreeMap<Integer,GaussianDistribution> churnGaussianSequence = new TreeMap<Integer, GaussianDistribution>();
        for (Integer featureIndex : featureIDToChurnFeatureValues.keySet()) {
            // get the values
            ArrayList<Double> featureValues = featureIDToChurnFeatureValues.get(featureIndex);
            // get the mean
            double mean = 0;
            for (Double featureValue : featureValues) {
                mean += featureValue;
            }
            mean /= (double)featureValues.size();

            // determine the standard deviation
            double sd = 0;
            for (Double featureValue : featureValues) {
                sd += Math.pow((featureValue - mean),2);
            }
            sd /= (double) featureValues.size() - 1;
            sd = Math.sqrt(sd);

            GaussianDistribution gaussian = new GaussianDistribution(mean, sd, 1);
            churnGaussianSequence.put(featureIndex,gaussian);
        }

        TreeMap<Integer,GaussianDistribution> nonChurnGaussianSequence = new TreeMap<Integer, GaussianDistribution>();
        for (Integer featureIndex : featureIDToNonChurnFeatureValues.keySet()) {
            // get the values
            ArrayList<Double> featureValues = featureIDToNonChurnFeatureValues.get(featureIndex);
            // get the mean
            double mean = 0;
            for (Double featureValue : featureValues) {
                mean += featureValue;
            }
            mean /= (double)featureValues.size();

            // determine the standard deviation
            double sd = 0;
            for (Double featureValue : featureValues) {
                sd += Math.pow((featureValue - mean),2);
            }
            sd /= (double) featureValues.size() - 1;
            sd = Math.sqrt(sd);

            GaussianDistribution gaussian = new GaussianDistribution(mean, sd, 1);
            nonChurnGaussianSequence.put(featureIndex,gaussian);
        }

        // build the Gaussian Sequence object
        gs.setChurnGaussianSequence(churnGaussianSequence);
        gs.setNonChurnGaussianSequence(nonChurnGaussianSequence);
        gs.setFeatureIndexToLabel(featureIDToLabel);
        gs.setFeatureLabelToIndex(featureLabelToID);
        gs.setFidelity(dataset.getK());

        return gs;
    }

    /*
     * Testing that the Gaussian sequences can be loaded from the given dataset
     */
    public static void main(String[] args) {

        String[] platforms = {"facebook", "sap", "serverfault"};
        int[] ks = {5};

        int mode = 1;   // detection at present
//        int mode = 2;   // forecasting next

        for (String platform : platforms) {
            System.out.println("\nFacebook");
            for (int k : ks) {
                System.out.println("k=" + k);
                DatasetBuilder builder = new DatasetBuilder(platform, k, mode);

                Dataset training = builder.buildTrainingData();
                System.out.println(training.toString());

                // build Gaussian Sequences from that
                GaussianSequenceConstructor constructor = new GaussianSequenceConstructor();
                GaussianSequence gs = constructor.buildSequenceFromDataset(training);
                System.out.println(gs.toString());


            }
        }

    }
}
