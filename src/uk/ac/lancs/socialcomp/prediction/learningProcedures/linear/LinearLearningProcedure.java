package uk.ac.lancs.socialcomp.prediction.learningProcedures.linear;

import uk.ac.lancs.socialcomp.prediction.features.Dataset;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 19/09/2014 / 15:32
 */
public interface LinearLearningProcedure {

    // sets the prediction model to be used
    public void setPredictionModel(double lambda, double eta, double alpha, double epsilon,
                                   int sigmaFidelity,
                                   Dataset training);

    // trains the model using the given method
    public void trainModel(Dataset dataset);

    // evalautes the model over the given dataset
    public double evaluateModel(Dataset dataset);

    // returns the cardinality of the feature space
    public int getM();

}
