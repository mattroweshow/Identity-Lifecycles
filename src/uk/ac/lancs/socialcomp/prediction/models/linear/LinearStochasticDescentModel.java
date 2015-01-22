package uk.ac.lancs.socialcomp.prediction.models.linear;

import uk.ac.lancs.socialcomp.prediction.features.Instance;
import uk.ac.lancs.socialcomp.prediction.models.PredictionModel;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 19/09/2014 / 14:46
 */
public class LinearStochasticDescentModel implements PredictionModel {
    @Override
    public double apply(Instance instance) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(double error, int featureIndex, Instance instance) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateBatch(int featureIndex) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double test(Instance instance) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean converged() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getEpochs() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
