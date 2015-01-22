package uk.ac.lancs.socialcomp.prediction.models.linear;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 28/07/2014 / 09:42
 */
public class LinearModelConfiguration {

    String platform;
    int fidelity;

    int learningProcedure;
    double lambda;
    double eta;
    double alpha;

    public LinearModelConfiguration(String platform, int fidelity, int learningProcedure,
                                    double lambda, double eta, double alpha) {
        this.learningProcedure = learningProcedure;
        this.lambda = lambda;
        this.eta = eta;
        this.alpha = alpha;

        this.platform = platform;
        this.fidelity = fidelity;
    }

    public LinearModelConfiguration() {
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public int getFidelity() {
        return fidelity;
    }

    public void setFidelity(int fidelity) {
        this.fidelity = fidelity;
    }

    public int getLearningProcedure() {
        return learningProcedure;
    }

    public void setLearningProcedure(int learningProcedure) {
        this.learningProcedure = learningProcedure;
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public double getEta() {
        return eta;
    }

    public void setEta(double eta) {
        this.eta = eta;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public String toString() {
        return "LinearModelConfiguration{" +
                "platform='" + platform + '\'' +
                ", fidelity=" + fidelity +
                ", learningProcedure=" + learningProcedure +
                ", lambda=" + lambda +
                ", eta=" + eta +
                ", alpha=" + alpha   +
                '}';
    }
}
