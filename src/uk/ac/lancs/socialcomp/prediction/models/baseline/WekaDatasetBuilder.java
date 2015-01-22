package uk.ac.lancs.socialcomp.prediction.models.baseline;

import uk.ac.lancs.socialcomp.prediction.features.Dataset;
import uk.ac.lancs.socialcomp.prediction.features.DatasetBuilder;
import uk.ac.lancs.socialcomp.prediction.features.Feature;
import uk.ac.lancs.socialcomp.prediction.features.FeatureProperties;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 01/08/2014 / 09:52
 */
public class WekaDatasetBuilder implements FeatureProperties {

    String platform;

    public WekaDatasetBuilder(String platform) {
        this.platform = platform;

    }

    public Instances buildWekaDataset(String split, int k, boolean allFeatures)  {

        // get the socialcomp dataset object for the given platform, split and setting of k
        DatasetBuilder builder = new DatasetBuilder(platform, k, 1);
        Dataset dataset = null;
        if(split.equals("train")) {
//            System.out.println("-Loading training data");
            dataset = builder.buildTrainingData();
        } else {
//            System.out.println("-Loading testing data");
            dataset = builder.buildTestingData();
        }

        // go through the first instance and generate the attribute framework
        // first specify the class attribute
        FastVector fvClassVal = new FastVector(2);
        fvClassVal.addElement("pos");
        fvClassVal.addElement("neg");
        Attribute ClassAttribute = new Attribute("class", fvClassVal);

        // then generate the remaining attributes
        int attributeCardinality = 0;
        for (uk.ac.lancs.socialcomp.prediction.features.Instance instance : dataset.getInstances()) {
            int innerAttributeCardinality = 0;
            for (Feature feature : instance.getFeatures()) {
                if(feature.getType() == STATIC)
                    innerAttributeCardinality++;

                // add the rate feature too if the allFeatures boolean is set to true
                if(allFeatures)
                    if(feature.getType() == RATE)
                        innerAttributeCardinality++;

            }
            if(innerAttributeCardinality > attributeCardinality)
                attributeCardinality = innerAttributeCardinality;
        }
        attributeCardinality++; // knock this on one for the class attribute
        // set the size of the weka attributes vector
        FastVector fvWekaAttributes = new FastVector(attributeCardinality);
        HashMap<String,Attribute> labelToAttributeMap = new HashMap<String, Attribute>();

        // now fill the vector with its elements
        for (uk.ac.lancs.socialcomp.prediction.features.Instance instance : dataset.getInstances()) {
            int innerAttributeCardinality = 0;
            for (Feature feature : instance.getFeatures()) {
                if(feature.getType() == STATIC)
                    innerAttributeCardinality++;

                // add the rate feature too if the allFeatures boolean is set to true
                if(allFeatures)
                    if(feature.getType() == RATE)
                        innerAttributeCardinality++;
            }


            if(innerAttributeCardinality == (attributeCardinality-1)) {
                // for each of the instance's features, generate a weka attribute
                for (Feature feature : instance.getFeatures()) {
                    if(feature.getType() == STATIC) {
                        String tempAttributeLabel = feature.getDynamic()
                                + "_" + feature.getEntropy()
                                + "_" + feature.getS()
                                + "_" + feature.getType();

                        Attribute tempAttribute = new Attribute(tempAttributeLabel);
                        fvWekaAttributes.addElement(tempAttribute);
                        labelToAttributeMap.put(tempAttributeLabel,tempAttribute);
                    } else if((feature.getType() == RATE) && (allFeatures)) {
                        // add the rate feature if allFeatures are enabled
                        String tempAttributeLabel = feature.getDynamic()
                                + "_" + feature.getEntropy()
                                + "_" + feature.getS()
                                + "_" + feature.getType();

                        Attribute tempAttribute = new Attribute(tempAttributeLabel);
                        fvWekaAttributes.addElement(tempAttribute);
                        labelToAttributeMap.put(tempAttributeLabel,tempAttribute);
                    }
                }
                break;
            }
        }

        // add the class attribute
        fvWekaAttributes.addElement(ClassAttribute);

        Instances wekaDataset = new Instances(this.platform + "_" + split + "_" + k,
                fvWekaAttributes,
                dataset.getInstances().length);
        wekaDataset.setClass(ClassAttribute);

        // populate the weka dataset using the native instances
        for (uk.ac.lancs.socialcomp.prediction.features.Instance instance : dataset.getInstances()) {
            Instance wekaInstance = new Instance(attributeCardinality);

            // get the features first
            for (Feature feature : instance.getFeatures()) {
                // only using static features if all features is negative
                if(feature.getType() == STATIC) {
                    // get the attribute object from the map
                    String tempAttributeLabel = feature.getDynamic()
                            + "_" + feature.getEntropy()
                            + "_" + feature.getS()
                            + "_" + feature.getType();
                    Attribute tempAttribute = labelToAttributeMap.get(tempAttributeLabel);
                    try {
                        wekaInstance.setValue(tempAttribute,feature.getValue());
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                } else if((feature.getType() == RATE) && (allFeatures)) {
                    // get the attribute object from the map
                    String tempAttributeLabel = feature.getDynamic()
                            + "_" + feature.getEntropy()
                            + "_" + feature.getS()
                            + "_" + feature.getType();
                    Attribute tempAttribute = labelToAttributeMap.get(tempAttributeLabel);
                    try {
                        wekaInstance.setValue(tempAttribute,feature.getValue());
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
            }

            // generate the class label
            double classVal = instance.response;
            if(classVal == 0) {
                wekaInstance.setValue(ClassAttribute,"neg");
            } else {
                wekaInstance.setValue(ClassAttribute,"pos");
            }

            // add the instance to the dataset
            wekaDataset.add(wekaInstance);
        }

        return wekaDataset;
    }

    public static void main(String[] args) {
        String[] platforms = {"facebook", "sap", "serverfault"};
        int[] ks = {5,10,20};
        String[] splits = {"train", "test"};

        boolean allFeatures = true;


        for (String platform : platforms) {
            WekaDatasetBuilder builder = new WekaDatasetBuilder(platform);
            for (String split : splits) {
                for (int k : ks) {

                    Instances dataset = builder.buildWekaDataset(split, k, allFeatures);
                    System.out.println(platform + " with " + split + " and " + k);
                    System.out.println(dataset.numInstances());

                }
            }
        }
    }

}
