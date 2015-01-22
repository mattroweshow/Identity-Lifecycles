package uk.ac.lancs.socialcomp.prediction;

import uk.ac.lancs.socialcomp.prediction.features.Dataset;
import uk.ac.lancs.socialcomp.prediction.features.DatasetBuilder;
import uk.ac.lancs.socialcomp.prediction.features.DatasetSplitter;
import uk.ac.lancs.socialcomp.prediction.features.Instance;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian.DualStochasticGradientDescent;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian.GaussianLearningProcedure;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian.GaussianLearningProcedureTypes;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian.StochasticGradientDescent;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 21/07/2014 / 12:46
 */
public class GaussianChurnDetectionModelTuner implements GaussianLearningProcedureTypes {

    public static void main(String[] args) {

//        String[] platforms = {"facebook", "sap", "serverfault"};
        String[] platforms = {"boards"};
        int[] ks = {5, 10, 20};
//        int[] ks = {10};

        // set the experiment's mode
        int mode = 1;   // detection at present

        // set the prediction model to be used: 1 = Single Gaussian Sequence, 2 = Dual Gaussian Sequence
        int[] models = {1,2};

        // set for the CV fidelity
        int foldCount = 5;

        // set the smoothing parameter
        double rho = 0.1;

        // set the learning procedure that is to be followed: 1=SGD, 2=DSGD
        int[] learningProcedures = {1,2};

        // the hyperparameters that are to be tuned
        double[] lambdas = {0.1,0.01,0.001,0.0001,0.00001};
        double[] etas = {0.1,0.01,0.001,0.0001,0.00001};

        // the model convergence parameter
        double epsilon = 0.001;

        // define the fidelity of the ROC calculation - greater fidelity = more FPR x TPR rates
        int sigmaFidelity = 20;

        GaussianChurnDetectionModelTuner runner = new GaussianChurnDetectionModelTuner();

        // tune the hyperparameters for the different platforms and lifecycle stages fidelity settings
        for (int model : models) {
            for (int learningProcedure : learningProcedures) {
                System.out.println("\nModel = " + model + " | Learning Procedure = " + learningProcedure);
                for (String platform : platforms) {
                    for (int k : ks) {
                        System.out.println("\nPlatform: " + platform + " | k = " + k);
                        runner.tuneHyperParameters(platform, k,
                                mode, model, foldCount,
                                rho,
                                learningProcedure,
                                lambdas, etas,
                                epsilon,
                                sigmaFidelity);
                    }
                }
            }
        }
    }

    /*
     * Tunes the hyperparameters for the given model and learning procedure
     */
    public void tuneHyperParameters(String platform, int k,
                                    int mode, int model, int foldCount,
                                    double rho,
                                    int learningProcedure,
                                    double[] lambdas, double[] etas,
                                    double epsilon,
                                    int sigmaFidelity) {

        StringBuffer buffer = new StringBuffer();
        for (double lambda : lambdas) {
            for (double eta : etas) {

                // build the training data
                DatasetBuilder builder = new DatasetBuilder(platform, k, mode);
                Dataset training = builder.buildTrainingData();

                // split the data into folds
                DatasetSplitter splitter = new DatasetSplitter(training);
                TreeMap<Integer,Dataset> folds = splitter.splitToFolds(foldCount);

                // run the prediction over the training set using CV
                TreeMap<Integer,Double> foldToROCScores = this.runCV(folds,
                        lambda, eta, rho, epsilon,
                        learningProcedure,
                        sigmaFidelity,
                        model);
                // derive the average of the fold scores
                double avgROC = this.deriveAvgROC(foldToROCScores);
                DecimalFormat df = new DecimalFormat("#.#####");
                System.out.println("lambda = " + lambda + " | eta = " + eta + " | Avg ROC = " + df.format(avgROC));

                // append the results to the buffer
                buffer.append(lambda + "\t" + eta + "\t" + df.format(avgROC) + "\n");
            }
        }

        // write the resuls to the file
        String filePathString = "data/results/" + platform + "/tuningresults_" + k + "_model_"
                + model + "_learning_" + learningProcedure + ".tsv";
        try {
            PrintWriter writer = new PrintWriter(filePathString);
            writer.write(buffer.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public TreeMap<Integer,Double> runCV(TreeMap<Integer,Dataset> folds,
                                         double lambda, double eta, double rho, double epsilon,
                                         int learningProcedure,
                                         int sigmaFidelity,
                                         int predictionModel) {
        TreeMap<Integer,Double> foldToROCScores = new TreeMap<Integer, Double>();

        // go through each fold
        for (Integer foldID : folds.keySet()) {
//            System.out.println("\n-Running fold: " + foldID);

            // compile the inner training and test datasets
            Dataset test = folds.get(foldID);

            // compile the training data from the other folds
            ArrayList<Instance> trainingFoldInstances = new ArrayList<Instance>();
            for (Integer foldID2 : folds.keySet()) {
                if(foldID != foldID2) {
                    Dataset trainFold = folds.get(foldID2);
                    for (Instance instance : trainFold.getInstances()) {
                        trainingFoldInstances.add(instance);
                    }
                }
            }
            Instance[] trainFoldInstancesArray = new Instance[trainingFoldInstances.size()];
            int foldIndex = 0;
            for (Instance instance : trainingFoldInstances) {
                trainFoldInstancesArray[foldIndex] = instance;
                foldIndex++;
            }
            Dataset training = new Dataset(test.getPlatform(),test.getSplit(),test.getK(), trainFoldInstancesArray);

            // run the learning routine and evaluate the model on the test fold
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
                    test,
                    predictionModel);

            // train it
            procedure.trainModel(training);

            // test it
            double roc = procedure.evaluateModel(test);

            // map the error to the fold id
            foldToROCScores.put(foldID, roc);
        }

        return foldToROCScores;
    }

    public double deriveAvgROC(TreeMap<Integer,Double> rocScores) {
        double avgROC = 0;
        for (Double roc : rocScores.values()) {
            avgROC += roc;
        }
        avgROC /= (double)rocScores.size();
        return avgROC;
    }
}
