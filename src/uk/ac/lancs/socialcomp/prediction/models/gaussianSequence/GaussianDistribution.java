package uk.ac.lancs.socialcomp.prediction.models.gaussianSequence;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 17/07/2014 / 16:40
 */
public class GaussianDistribution {

    double mean;
    double sd;
    int type;   // the type of the sequence - i.e. 1 = churn, 0 = non-churn

    public GaussianDistribution(double mean, double sd, int type) {
        this.mean = mean;
        this.sd = sd;
        this.type = type;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getSd() {
        return sd;
    }

    public void setSd(double sd) {
        this.sd = sd;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "GaussianDistribution{" +
                "mean=" + mean +
                ", sd=" + sd +
                ", type=" + type +
                '}';
    }

    public double calcProb(double value) {
        double prob = 0;
        if((mean != 0) && (sd !=0)) {
            NormalDistribution distribution = new NormalDistribution(mean, sd);
            prob = distribution.density(value);
        }
        return prob;
    }
}
