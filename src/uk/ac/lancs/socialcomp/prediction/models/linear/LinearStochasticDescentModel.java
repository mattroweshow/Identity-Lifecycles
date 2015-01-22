package uk.ac.lancs.socialcomp.prediction.models.linear;

import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import uk.ac.lancs.socialcomp.prediction.features.Dataset;
import uk.ac.lancs.socialcomp.prediction.features.Instance;
import uk.ac.lancs.socialcomp.prediction.models.PredictionModel;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 19/09/2014 / 14:46
 */
public class LinearStochasticDescentModel implements PredictionModel {

    double lambda, eta, alpha, epsilon;
    int epochs;
    int m;  // number of features
    Dataset trainingData;

    // parameter vectors
    SparseDoubleMatrix1D b;
    SparseDoubleMatrix1D bOld;


    public LinearStochasticDescentModel(double lambda, double eta, double alpha, double epsilon, int m, Dataset trainingData) {
        this.lambda = lambda;
        this.eta = eta;
        this.alpha = alpha;
        this.epsilon = epsilon;
        this.m = m;
        this.trainingData = trainingData;

        // initialise the parameter vector
        b = new SparseDoubleMatrix1D(m);

        this.epochs = 0;
    }

    @Override
    public double apply(Instance instance) {
        // work out the error in the prediction when applying the model to the instance.
        // N.b. this must be the actual value, not the absolute value

        // derive the predicted probability
        double prob = 0;
        for (int i = 0; i < m; i++) {
            prob += b.get(i) * instance.getFeatures().get(i).getValue();
        }

        // work out the error in prediction
        double error = instance.response - prob;
        return error;
    }

    @Override
    public void update(double error, int featureIndex, Instance instance) {
        // go through all the instances and work out the average loss at parameter j
        double localError = error;
        localError *= instance.getFeatures().get(featureIndex).getValue();

        if(localError != 0) {
            localError /= -1;
        }
        // add the regularisation term to the end
        double regulariser = lambda * (1 - alpha) * b.get(featureIndex) + lambda * alpha;
        double deltaJ = localError + regulariser;

        // update parameter at the feature index based on deltaJ
        double betaJ = b.get(featureIndex);
        double newBetaJ = betaJ - eta * deltaJ;
        b.set(featureIndex,newBetaJ);
    }

    @Override
    public void updateBatch(int featureIndex) {
        // do nothing here as we are using a stochastic learning routine
    }

    @Override
    public double test(Instance instance) {
        double prob = 0;
        for (int i = 0; i < m; i++) {
            prob += b.get(i) * instance.getFeatures().get(i).getValue();
        }
        // work out the error in prediction
        double error = instance.response - prob;
//        return error;
        return prob;
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

    @Override
    public int getEpochs() {
        return this.epochs;
    }
}
