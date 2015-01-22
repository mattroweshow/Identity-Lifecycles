package uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian;

import uk.ac.lancs.socialcomp.prediction.features.Dataset;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 17/07/2014 / 15:58
 */
public interface GaussianLearningProcedure {

    // sets the prediction model to be used
    public void setPredictionModel(double lambda, double eta, double rho, double epsilon,
                                   int sigmaFidelity,
                                   Dataset training,
                                   int predictionModel);

    // trains the model using the given method
    public void trainModel(Dataset dataset);

    // evalautes the model over the given dataset
    public double evaluateModel(Dataset dataset);

}
