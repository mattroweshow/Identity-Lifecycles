package uk.ac.lancs.socialcomp.prediction.models;

import uk.ac.lancs.socialcomp.prediction.features.Instance;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 17/07/2014 / 15:56
 */
public interface PredictionModel {

    // Apply the model to the instance and calculate an error (actual class vs predicted)
    // Returns the error (absolute, so positive and negative)
    public double apply(Instance instance);

    // Updates the model's parameters using the given error and instance
    public void update(double error,
                       int featureIndex,
                       Instance instance);

    public void updateBatch(int featureIndex);

    // Runs the given instance with the current model and returns the probability calculation
    public double test(Instance instance);

    // Check that the model's parameters have converged
    public boolean converged();

    // returns how many times the dataset has been iterated through
    public int getEpochs();

    // output diagnostics of the model
    public String toString();


}
