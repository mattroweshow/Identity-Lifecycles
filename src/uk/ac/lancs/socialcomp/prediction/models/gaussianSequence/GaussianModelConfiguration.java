package uk.ac.lancs.socialcomp.prediction.models.gaussianSequence;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 28/07/2014 / 09:42
 */
public class GaussianModelConfiguration {

    String platform;
    int fidelity;

    int model;
    int learningProcedure;
    double lambda;
    double eta;
    double rho;

    public GaussianModelConfiguration(String platform, int fidelity, int model, int learningProcedure, double lambda, double eta, double rho) {
        this.model = model;
        this.learningProcedure = learningProcedure;
        this.lambda = lambda;
        this.eta = eta;
        this.rho = rho;
        this.platform = platform;
        this.fidelity = fidelity;
    }

    public GaussianModelConfiguration() {
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

    public int getModel() {
        return model;
    }

    public void setModel(int model) {
        this.model = model;
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

    public double getRho() {
        return rho;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    @Override
    public String toString() {
        return "GaussianModelConfiguration{" +
                "platform='" + platform + '\'' +
                ", fidelity=" + fidelity +
                ", model=" + model +
                ", learningProcedure=" + learningProcedure +
                ", lambda=" + lambda +
                ", eta=" + eta +
                ", rho=" + rho +
                '}';
    }
}
