package uk.ac.lancs.socialcomp.prediction.models.gaussianSequence;

import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import uk.ac.lancs.socialcomp.prediction.features.Dataset;
import uk.ac.lancs.socialcomp.prediction.features.Feature;
import uk.ac.lancs.socialcomp.prediction.features.FeatureProperties;
import uk.ac.lancs.socialcomp.prediction.features.Instance;
import uk.ac.lancs.socialcomp.prediction.models.PredictionModel;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 17/07/2014 / 16:16
 */
public class DualGSDetectionModel implements PredictionModel, FeatureProperties {

    double lambda, eta, rho, epsilon;
    int epochs;
    int m;  // number of features
    Dataset trainingData;

    // parameter vector
    SparseDoubleMatrix1D b;
    SparseDoubleMatrix1D bOld;

    GaussianSequence gs;


    public DualGSDetectionModel(double lambda, double eta, double rho, double epsilon, int m, Dataset trainingData) {
        this.lambda = lambda;
        this.eta = eta;
        this.rho = rho;
        this.epsilon = epsilon;
        this.m = m;
        this.trainingData = trainingData;

        this.epochs = 0;

        // initialise the parameter vector
        b = new SparseDoubleMatrix1D(m);
        // prime these to random values
//        for (int i = 0; i < b.size(); i++) {
//             b.set(i,Math.random());
//        }

        // construct the gaussian sequence from the dataset
        GaussianSequenceConstructor constructor = new GaussianSequenceConstructor();
        gs = constructor.buildSequenceFromDataset(trainingData);
    }

    @Override
    public double apply(Instance instance) {

        double outerChurnProb = 1;

        // go through each of the instance's features
        for (Feature feature : instance.getFeatures()) {
            // construct the label
            if(feature.getType() == STATIC) {
                String label = feature.getS() + "_" + feature.getDynamic() + "_" + feature.getEntropy();

                // get the numeric id that this maps to
                int featureIndex = gs.getFeatureLabelToIndex().get(label);

                // get the probability of churn membership
                double churnProb = gs.getChurnGaussianSequence().get(featureIndex).calcProb(feature.getValue());

                // get the probability of non-churn membership
                double nonChurnProb = gs.getNonChurnGaussianSequence().get(featureIndex).calcProb(feature.getValue());

                // calculate the beta weighted probability
                double innerChurnProb = (b.get(featureIndex) * churnProb) - ((1 - b.get(featureIndex)) * nonChurnProb);

                // handle zero inner probabilities
                if(innerChurnProb <= 0) {
                    innerChurnProb = rho;
                }

                // multiply with the product of the outerchurb prob
                outerChurnProb *= innerChurnProb;
            }
        }

        // derive the as-is error
        double error = instance.response - outerChurnProb;
        return error;
    }

    @Override
    public double test(Instance instance) {
        double outerChurnProb = 1;

        // go through each of the instance's features
        for (Feature feature : instance.getFeatures()) {
            // construct the label
            if(feature.getType() == STATIC) {
                String label = feature.getS() + "_" + feature.getDynamic() + "_" + feature.getEntropy();

                // get the numeric id that this maps to
                int featureIndex = gs.getFeatureLabelToIndex().get(label);

                // get the probability of churn membership
                double churnProb = gs.getChurnGaussianSequence().get(featureIndex).calcProb(feature.getValue());

                // get the probability of non-churn membership
                double nonChurnProb = gs.getNonChurnGaussianSequence().get(featureIndex).calcProb(feature.getValue());

                // calculate the beta weighted probability
                double betaCoef = b.get(featureIndex);
                double innerChurnProb = (betaCoef * churnProb) - ((1 - betaCoef) * nonChurnProb);

                // handle zero inner probabilities
                if(innerChurnProb <= 0) {
                    innerChurnProb = rho;
                }

                // multiply with the product of the outerchurb prob
                outerChurnProb *= innerChurnProb;
            }
        }

        return outerChurnProb;
    }

    @Override
    public void update(double error, int featureIndex, Instance instance) {

        //// only update if the error is not 0!
        if(error != 0) {
            // update a single vector if the id is not -1
            if(featureIndex >= 0) {
//                System.out.println(featureIndex);
                // get the current parameter weight
                double beta = b.get(featureIndex);
                // update this based on the update rule for a single parameter
                beta += eta * (error - (lambda * beta));
                b.set(featureIndex,beta);

            // otherwise update everything - we use this in the singular stochastic gradient descent setting
            } else {
                // update each parameter's weight
                for (int i = 0; i < b.size(); i++) {
                    double beta = b.get(i);
                    // update this based on the update rule for a single parameter
                    beta += eta * (error - (lambda * beta));
                    b.set(i,beta);
                }
            }
        }
    }

    @Override
    public void updateBatch(int featureIndex) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getEpochs() {
        return this.epochs;
    }

    @Override
    public boolean converged() {
//        System.out.println("Checking convergence");
        if(epochs > 0) {
            double diff = 0;
            for (int i = 0; i < b.size(); i++) {
                diff += Math.abs(b.get(i) - bOld.get(i));
            }

            epochs++;
            bOld = b;

            if(diff < epsilon) {
//                System.out.println("Model has converged, exiting");
                return true;
            } else {
                return false;
            }
        } else {
            epochs++;
            bOld = b;
            return false;
        }
    }
}
