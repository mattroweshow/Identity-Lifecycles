package uk.ac.lancs.socialcomp.prediction;

import uk.ac.lancs.socialcomp.prediction.features.Dataset;
import uk.ac.lancs.socialcomp.prediction.features.DatasetBuilder;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian.DualStochasticGradientDescent;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian.GaussianLearningProcedure;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian.GaussianLearningProcedureTypes;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian.StochasticGradientDescent;
import uk.ac.lancs.socialcomp.prediction.models.gaussianSequence.GaussianModelConfiguration;

import java.text.DecimalFormat;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 23/07/2014 / 10:40
 */
public class GaussianChurnDetectionModelRunner implements GaussianLearningProcedureTypes {

    public static void main(String[] args) {

        // platform to run
//        String[] platforms = {"facebook", "sap", "serverfault","boards"};
        String[] platforms = {"boards"};

        // lifecycle fidelity setting
//        int[] ks = {5,10,20};
        int[] ks = {20};

        // lambda and eta setting for the above platform and fidelity
        double lambda = 0.01;
        double eta = 0.01;

        // set the experiment's mode
        int mode = 1;   // detection at present

        // set the prediction model to be used: 1 = Single Gaussian Sequence, 2 = Dual Gaussian Sequence
        int model = 2;

        // set the smoothing parameter
        double rho = 0.1;

        // set the learning procedure that is to be followed: 1=SGD, 2=DSGD
//        int learningProcedure = 1;

        // the model convergence parameter
        double epsilon = 0.001;

        // define the fidelity of the ROC calculation - greater fidelity = more FPR x TPR rates
        int sigmaFidelity = 10;


        // Run 1: using specific settings
        GaussianChurnDetectionModelRunner runner = new GaussianChurnDetectionModelRunner();
//        // run the model using the given tuned hyperparameters
//        for (String platform : platforms) {
//            for (int k : ks) {
//                System.out.println("\nPlatform: " + platform + " | k = " + k);
//                System.out.println("Lambda = " + lambda + " | Eta = " + eta);
//                double roc = runner.applyModel(platform, k,
//                        mode, model,
//                        rho,
//                        learningProcedure,
//                        lambda, eta,
//                        epsilon,
//                        sigmaFidelity);
//                DecimalFormat df = new DecimalFormat("#.###");
//                System.out.println("ROC = " + df.format(roc));
//            }
//        }

        // Run 2: using the best tuned hyperparameters
        int[] models = {1,2};
//        int[] models = {2};
        int[] learningProcedures = {1,2};
//        int[] learningProcedures = {2};

        for (String platform : platforms) {
            for (int k : ks) {
                for (int procedure : learningProcedures) {
                    for (int modelA : models) {

                        System.out.println("\nPlatform: " + platform + " | k = " + k
                                + " | model = " + modelA + " | procedure = " + procedure);
                        double roc = runner.applyModel(platform, k,
                                mode,
                                modelA, procedure,
                                rho,
                                epsilon,
                                sigmaFidelity);
                        DecimalFormat df = new DecimalFormat("#.#####");
                        System.out.println("ROC = " + df.format(roc));
                    }
                }
            }
        }

    }

    /*
     * Runs the model over the test set with the tuned lambda and eta settings
     */
    public double applyModel(String platform, int k,
                                    int mode, int model,
                                    double rho,
                                    int learningProcedure,
                                    double lambda, double eta,
                                    double epsilon,
                                    int sigmaFidelity) {



        // build the training data
        DatasetBuilder builder = new DatasetBuilder(platform, k, mode);
        Dataset training = builder.buildTrainingData();

        // build the test data
        Dataset test = builder.buildTestingData();

        // apply the model to the test set
        GaussianLearningProcedure procedure = null;
        switch(learningProcedure) {
            case DSGD:
                procedure = new DualStochasticGradientDescent();
                break;
            default: procedure = new StochasticGradientDescent();
                break;
        }

        procedure.setPredictionModel(lambda,eta,rho,epsilon,
                sigmaFidelity,
                training,
                model);

        // test it 1 - using the entire test set
        // run this 20 times and take the average
        int count = 0;
        double avgRoc = 0;
        while (count < 20) {
            // train it
            procedure.trainModel(training);
            // test it
            double roc = procedure.evaluateModel(test);
            // accumulate the roc
            avgRoc += roc;
            count++;
        }
        avgRoc /= 20;
        return avgRoc;
    }

    /*
     * Method that returns the best tuned hyperparameters and then uses them to test the model on the held out split
     */
    public double applyModel(String platform, int k,
                             int mode,
                             int model, int learningProcedure,
                             double rho, double epsilon,
                             int sigmaFidelity) {

        DatasetBuilder builder = new DatasetBuilder(platform, k, mode);
        Dataset training = builder.buildTrainingData();

        // build the test data
        Dataset test = builder.buildTestingData();

        // get the best model configuration
        GaussianBestHyperparameterSelection selection = new GaussianBestHyperparameterSelection(platform,  k, model, learningProcedure, rho);
        GaussianModelConfiguration configuration = selection.getBestTunedModelConfiguration();

        // apply the model to the test set
        GaussianLearningProcedure procedure = null;
        switch(learningProcedure) {
            case DSGD:
                procedure = new DualStochasticGradientDescent();
                break;
            default: procedure = new StochasticGradientDescent();
                break;
        }

        procedure.setPredictionModel(configuration.getLambda(),
                configuration.getEta(),
                rho,
                epsilon,
                sigmaFidelity,
                training,
                model);

        // test it 1 - using the entire test set
        // run this 20 times and take the average
        int count = 0;
        double avgRoc = 0;
        while (count < 20) {
            // train it
            procedure.trainModel(training);
            // test it
            double roc = procedure.evaluateModel(test);
            // accumulate the roc
            avgRoc += roc;
            count++;
        }
        avgRoc /= 20;
        return avgRoc;
    }

}
