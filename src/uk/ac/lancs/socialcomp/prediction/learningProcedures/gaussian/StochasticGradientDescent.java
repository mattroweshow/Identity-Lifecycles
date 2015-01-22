package uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian;

import uk.ac.lancs.socialcomp.prediction.features.Dataset;
import uk.ac.lancs.socialcomp.prediction.features.Feature;
import uk.ac.lancs.socialcomp.prediction.features.FeatureProperties;
import uk.ac.lancs.socialcomp.prediction.features.Instance;
import uk.ac.lancs.socialcomp.prediction.models.gaussianSequence.DualGSDetectionModel;
import uk.ac.lancs.socialcomp.prediction.models.PredictionModel;
import uk.ac.lancs.socialcomp.prediction.models.PredictionModelTypes;
import uk.ac.lancs.socialcomp.prediction.models.gaussianSequence.SingleGSDetectionModel;

import java.util.*;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 18/07/2014 / 16:24
 */
public class StochasticGradientDescent implements GaussianLearningProcedure, FeatureProperties, PredictionModelTypes {


    int maxEpochs;
    int sigmaFidelity;
    PredictionModel model;

    @Override
    public void setPredictionModel(double lambda, double eta, double rho, double epsilon,
                                   int sigmaFidelity,
                                   Dataset training,
                                   int predictionModel) {

        // derive the number of static features in the dataset
        int m = 0;
        int maxM = 0;
        for (Instance instance : training.getInstances()) {
            // count how many static features the first instance has
            for (Feature feature : instance.getFeatures()) {
                if(feature.getType() == STATIC) {
                    m++;
                }
            }

            if(m > maxM)
                maxM = m;
        }
        m = maxM;

        // initialise the model based on the ID of the model to be used
        switch (predictionModel) {
            case DUAL_GS_MODEL:
                model = new DualGSDetectionModel(lambda, eta, rho, epsilon, m, training);
                break;
            case SINGLE_GS_MODEL:
                model = new SingleGSDetectionModel(lambda, eta, rho, epsilon, m, training);
                break;
        }

        this.sigmaFidelity = sigmaFidelity;
    }

    @Override
    public void trainModel(Dataset training) {

        // store the ids of the training instances
        ArrayList<Integer> trainingInstanceIDs = new ArrayList<Integer>();
        for (int i = 0; i < training.getInstances().length; i++) {
            trainingInstanceIDs.add(i);
        }

        // store the ids of the features - only for dual stochastic gradient descent
//        int featureTally = training.getInstances()[0].getFeatures().size();
//        ArrayList<Integer> featureIDs =

        while(!model.converged() || (model.getEpochs() < maxEpochs)) {
            // shuffle the order of the training users
            Collections.shuffle(trainingInstanceIDs);

            // run through the users one at a time
            for (Integer instanceID : trainingInstanceIDs) {
                Instance instance = training.getInstances()[instanceID];

                // get the error
                double error = model.apply(instance);

                // update the model using the error
                model.update(error, -1, instance);
            }

//            System.out.println("-Epoch #: " + model.getEpochs());
        }
    }

    @Override
    public double evaluateModel(Dataset test) {
        // calculate the ROC using sigma fidelity increases
//        double[] sigmas = {1, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1};

        // compute the mapping between instances and churn probabilities
        HashMap<Instance,Double> instanceToChurnProbability = new HashMap<Instance, Double>();
        for (Instance instance : test.getInstances()) {
            double churnProb = model.test(instance);
            churnProb = Math.log(churnProb);
            instanceToChurnProbability.put(instance,churnProb);
        }

        // go through the test instances and derive the TPR and FPR for them
        TreeMap<Double,Double> tprToFPR  = getTPRToFPR(instanceToChurnProbability);

        // derive the AUROC from the values
        double auc = deriveROC(tprToFPR);

        return auc;
    }

    /*
     * Convenience function to derive the mapping between TPR and FPR values
     */
    private TreeMap<Double,Double> getTPRToFPR(HashMap<Instance,Double> instanceToChurnProbability) {
        TreeMap<Double,Double> tprToFPR = new TreeMap<Double, Double>();

        // dynamically derive the sigma values based on splitting the min-max churn probability interval
        double minChunProb = 10;
        double maxChurnProb = -10;
        for (Double churnProb : instanceToChurnProbability.values()) {
            if(minChunProb > churnProb)
                minChunProb = churnProb;
            if(maxChurnProb < churnProb)
                maxChurnProb = churnProb;
        }

        // bin splitting approach 1
        // split the sigma range into 10 bins
//        double binWidth = (maxChurnProb - minChunProb) / (double)sigmaFidelity; // derive the bin width
//        // generate the sigma points
//        Stack<Double> sigmaPoints = new Stack<Double>();
//        double currentBinWith = minChunProb;
//        sigmaPoints.push(minChunProb);
//        for (int i = 0; i < (sigmaFidelity-1); i++) {
//            currentBinWith += binWidth;
//            sigmaPoints.push(currentBinWith);
//        }

        // bin splitting approach 2
        // get the next smallest value over the min
        double nextMinChurnProb = 10;
        for (Double churnProb : instanceToChurnProbability.values()) {
            if((churnProb > minChunProb) && (churnProb < nextMinChurnProb)) {
                nextMinChurnProb = churnProb;
            }
        }
        // derive the bin width as being the gap between the min and next probability value
        double binWidth = nextMinChurnProb - minChunProb;
        Stack<Double> sigmaPoints = new Stack<Double>();
        double currentBinWith = minChunProb;
        while(currentBinWith <= maxChurnProb) {
            currentBinWith += binWidth;
            sigmaPoints.push(currentBinWith);
        }

        // derive the TPR and FPR coordinates
        while(!sigmaPoints.empty()) {
            double sigma = sigmaPoints.pop();

            double tpTally = 0;
            double fpTally = 0;
            double tnTally = 0;
            double fnTally = 0;

            double sanityCheck = 0;

            for (Instance instance : instanceToChurnProbability.keySet()) {
                double churnProb = instanceToChurnProbability.get(instance);
                double classLabel = instance.response;

                // predicted to churn
                if(churnProb > sigma) {
                    if(classLabel == 1) {
                        tpTally++;
                    } else {
                        fpTally++;
                    }
                // not predicted to churn
                } else {
                    if(classLabel == 0) {
                        tnTally++;
                    } else {
                        fnTally++;
                    }
                }
            }

            sanityCheck = tpTally + fpTally + tnTally + fnTally;

            // derive tpr
            double tpr = tpTally / (tpTally + fnTally);
            tpr *= 100;
            // derive fpr
            double fpr = fpTally / (fpTally + tnTally);
            fpr *= 100;

            // map them
            tprToFPR.put(tpr,fpr);
        }

        if(tprToFPR.size() > 0) {
            tprToFPR.put(0.0,0.0);
            tprToFPR.put(100.0,100.0);
        } else {
            tprToFPR.put(0.0,0.0);
            tprToFPR.put(100.0,0.0);
        }


        return tprToFPR;
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


}
