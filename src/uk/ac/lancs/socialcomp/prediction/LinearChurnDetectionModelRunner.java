package uk.ac.lancs.socialcomp.prediction;

import uk.ac.lancs.socialcomp.prediction.features.Dataset;
import uk.ac.lancs.socialcomp.prediction.features.DatasetBuilder;
import uk.ac.lancs.socialcomp.prediction.features.Instance;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.linear.LinearAverageDescentLearningProcedure;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.linear.LinearLearningProcedure;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.linear.LinearLearningProcedureTypes;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.linear.LinearStochasticDescentLearningProcedure;
import uk.ac.lancs.socialcomp.prediction.models.linear.LinearModelConfiguration;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 23/07/2014 / 10:40
 */
public class LinearChurnDetectionModelRunner implements LinearLearningProcedureTypes {

    public static void main(String[] args) {

        // platform to run
//        String[] platforms = {"facebook", "sap", "serverfault", "boards"};
        String[] platforms = {"facebook"};

        // lifecycle fidelity setting
        int[] ks = {5,10,20};

        // lambda and eta setting for the above platform and fidelity
//        double lambda = 0.01;
//        double eta = 0.01;

        double[] alphas = {0,0.5,1};

        // set the experiment's mode
        int mode = 1;   // detection at present

        // set the learning procedure that is to be followed: 1=SGD, 2=DSGD
//        int learningProcedure = 1;

        // the model convergence parameter
        double epsilon = 0.001;

        // define the fidelity of the ROC calculation - greater fidelity = more FPR x TPR rates
        int sigmaFidelity = 10;


        // Run 1: using specific settings
//        GaussianChurnDetectionModelRunner runner = new GaussianChurnDetectionModelRunner();
        LinearChurnDetectionModelRunner runner = new LinearChurnDetectionModelRunner();
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
        DecimalFormat df = new DecimalFormat("#.###");

        // cap the limit of the number of times that the runs are called
        int runLimit = 20;

//        // run the model for lp=2, platform = facebook, fidelities = 5, 10, alpha = 0.5
//        int[] ks2 = {10,20};
//        for (int k : ks2) {
//            double roc = runner.applyModel("facebook", k,
//                    mode,
//                    0.5,
//                    2,
//                    epsilon,
//                    sigmaFidelity);
//            System.out.println(roc);
//        }



        // formats results for rendering in latex
        int[] learningProcedures = {1, 2};
        for (String platform : platforms) {
            System.out.print(platform);
            for (int k : ks) {
                System.out.print(" & " + k);
                for (double alpha : alphas) {
                    System.out.print(" & ");
                    for (int procedure : learningProcedures) {
                        ArrayList<Double> runResults = new ArrayList<Double>();
                        int runCount = 0;
                        while(runCount < runLimit) {
                            double roc = runner.applyModel(platform, k,
                                    mode,
                                    alpha,
                                    procedure,
                                    epsilon,
                                    sigmaFidelity);
                            runCount++;

                            // append the run's ROC to the arrayList
                            if(roc != 0.5 && roc !=0)
                                runResults.add(roc);
                        }

                        // work out the mean
                        double mean = 0;
                        for (Double runResult : runResults) {
                            mean += runResult;
                        }
                        mean /= (double)runResults.size();

                        // work out the sd
                        double sd = 0;
                        for (Double runResult : runResults) {
                            double innerProduct = runResult - mean;
                            sd += Math.pow(innerProduct,2);
                        }
                        if(sd > 0)
                            sd /= (double) runResults.size() - 1;
                        sd = Math.sqrt(sd);

                        System.out.print(" / " + df.format(mean));
                        System.out.print(" (" + df.format(sd) + ")");
                    }
                }
                System.out.println("\\\\");
            }
        }

    }

    /*
     * Method that returns the best tuned hyperparameters and then uses them to test the model on the held out split
     */
    public double applyModel(String platform, int k,
                             int mode,
                             double alpha,
                             int learningProcedure,
                             double epsilon,
                             int sigmaFidelity) {

        // build the training data
        DatasetBuilder builder = new DatasetBuilder(platform, k, mode);
        Dataset training = builder.buildTrainingData();

        // build the test data
        Dataset test = builder.buildTestingData();


        // get the best model configuration
        LinearBestHyperparameterSelection selection = new LinearBestHyperparameterSelection(platform,  k, learningProcedure, alpha);
        LinearModelConfiguration configuration = selection.getBestTunedModelConfiguration();

        // apply the model to the test set
        LinearLearningProcedure procedure = null;
        switch(learningProcedure) {
            case AD:
                procedure = new LinearAverageDescentLearningProcedure();
                break;
            default:
                procedure = new LinearStochasticDescentLearningProcedure();
                break;
        }

        procedure.setPredictionModel(configuration.getLambda(),
                configuration.getEta(),
                alpha,
                epsilon,
                sigmaFidelity,
                training);

        // clean the datasets
        training = cleanDataset(training, procedure.getM());
        test = cleanDataset(test,procedure.getM());


        // test it 1 - using the entire test set
        procedure.trainModel(training);
        double roc = procedure.evaluateModel(test);
        return roc;
    }

    public Dataset cleanDataset(Dataset dataset, int m) {
        ArrayList<Instance> instances = new ArrayList<Instance>();
        for (Instance instance : dataset.getInstances()) {
            if(instance.getFeatures().size() == m)
                instances.add(instance);
        }
        // convert to an array
        Instance[] instanceArray = new Instance[instances.size()];
        for (int i = 0; i < instanceArray.length; i++) {
            instanceArray[i] = instances.get(i);
        }
        // reset the dataset's instances
        dataset.setInstances(instanceArray);
        return dataset;
    }

}

