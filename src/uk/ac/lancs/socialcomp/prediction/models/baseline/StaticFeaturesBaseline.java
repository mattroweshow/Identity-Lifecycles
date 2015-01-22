package uk.ac.lancs.socialcomp.prediction.models.baseline;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import java.text.DecimalFormat;
import java.util.Stack;
import java.util.TreeMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 01/08/2014 / 09:52
 */
public class StaticFeaturesBaseline {
    String platform;

    public void classify(String platform) {

        try {
//            System.out.println("\nP: Loading weka datasets");
            WekaDatasetBuilder datasetBuilder = new WekaDatasetBuilder(platform);

            // run the different fidelities of lifecycle stages
            int[] ks = {5,10,20};
            for (int k : ks) {
                System.out.println("k = " + k);

                // load the training dataset for the platform
                Instances train = datasetBuilder.buildWekaDataset("train",k, false);

                // load the test dataset for the platform
                Instances test = datasetBuilder.buildWekaDataset("test", k, false);

                // induce the j48 model
//                System.out.println("\nC: Computing the prediction model");
                J48 model = new J48();
//                SMO model = new SMO();
//                NaiveBayes model = new NaiveBayes();
//                Logistic model = new Logistic();
                model.buildClassifier(train);

                // apply it to the test dataset
//                System.out.println("\nC: Applying the prediction model");
                Evaluation evaluation = new Evaluation(train);

                double tpTally = 0;
                double fpTally = 0;
                double tnTally = 0;
                double fnTally = 0;

                // go through and classify each instance
                for (int i = 0; i < test.numInstances(); i++) {
                    Instance instance = test.instance(i);
                    double classLabel = model.classifyInstance(instance);

                    // predicted to churn
                    // true
                    if(classLabel == instance.classValue()) {
                        // 0 is pos
                        if(classLabel == 0) {
                            tpTally++;
                            // 1 is neg
                        } else {
                            tnTally++;
                        }
                        // false
                    } else {
                        if(classLabel == 0) {
                            fpTally++;
                            // 1 is neg
                        } else {
                            fnTally++;
                        }
                    }
                }

                // derive tpr
                double tpr = tpTally / (tpTally + fnTally);
                tpr *= 100;
                // derive fnr
                double fpr = fpTally / (fpTally + tnTally);
                fpr *= 100;

                TreeMap<Double,Double> tprToFPR = new TreeMap<Double, Double>();
                tprToFPR.put(tpr,fpr);
                if(tpr !=0 & fpr !=0) {
                    tprToFPR.put(0.0,0.0);
                    tprToFPR.put(100.0,100.0);
                } else {
                    tprToFPR.put(0.0,0.0);
                    tprToFPR.put(100.0,0.0);
                }

                double roc = deriveROC(tprToFPR);
                DecimalFormat df = new DecimalFormat("#.#####");
                System.out.println("ROC = " + df.format(roc));
//                System.out.println(roc);




            }

            // output the evaluation results
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    * Convience function to derive the ROC
    */
    private double deriveROC(TreeMap<Double,Double> tprToFPR) {
        double auc = 0;

        // convert the keyset into indexed elements
        Stack<Double> tprIndices = new Stack<Double>();
        for (Double tpr : tprToFPR.keySet()) {
            tprIndices.push(tpr);
        }

        // iterate through the values and derive the area under the trapezoid
        for (int i = 0; i < (tprIndices.size()-1); i++) {

            double tprI = tprIndices.get(i);
            double tprJ = tprIndices.get(i+1);

            double fprI = tprToFPR.get(tprI);
            double fprJ = tprToFPR.get(tprJ);

            // derive the run
            double run = fprJ - fprI;

            // derive the average height of the rotated trapezoid
            double avgRise = (tprI + tprJ) / 2;

            // derive the area
            double trapArea = run * avgRise;

            // accumulate the area
            auc += trapArea;
        }

        // divide by the total possible area (i.e. 100 x 100)
        auc /= (100 * 100);

        return auc;
    }

    public static void main(String[] args) {
        String[] platforms = {"facebook", "sap", "serverfault", "boards"};
//        String[] platforms = {"boards"};
        StaticFeaturesBaseline j48 = new StaticFeaturesBaseline();
        for (String platform : platforms) {
            System.out.println("\nPlatform = " + platform);
            j48.classify(platform);
        }

    }
}
