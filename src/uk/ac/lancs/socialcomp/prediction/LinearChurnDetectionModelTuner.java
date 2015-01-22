package uk.ac.lancs.socialcomp.prediction;

import uk.ac.lancs.socialcomp.prediction.features.Dataset;
import uk.ac.lancs.socialcomp.prediction.features.DatasetBuilder;
import uk.ac.lancs.socialcomp.prediction.features.DatasetSplitter;
import uk.ac.lancs.socialcomp.prediction.features.Instance;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.linear.LinearAverageDescentLearningProcedure;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.linear.LinearLearningProcedure;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.linear.LinearLearningProcedureTypes;
import uk.ac.lancs.socialcomp.prediction.learningProcedures.linear.LinearStochasticDescentLearningProcedure;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 22/09/2014 / 10:46
 */
public class LinearChurnDetectionModelTuner implements LinearLearningProcedureTypes {

    public static void main(String[] args) {

//        String[] platforms = {"serverfault"};
        String[] platforms = {"boards"};
        int[] ks = {5,10,20};
//        int[] ks = {5,10};

        // set the experiment's mode
        int mode = 1;   // detection at present

        // set for the CV fidelity
        int foldCount = 5;

        // set the indices for the regularisation penalities
        double[] alphas = {0,0.5,1};
//        double[] alphas = {0};

        // set the learning procedure that is to be followed:
        // 1=Average Coordinate Descent, 2=Stochastic Coordinate Descent
        int[] learningProcedures = {1,2};

        // the hyperparameters that are to be tuned
        double[] lambdas = {0.5, 0.4, 0.3, 0.2, 0.1, 0.01,0.001};
//        double[] lambdas = {0.001};
        double[] etas = {0.5, 0.4, 0.3, 0.2, 0.1,0.01,0.001,0.0001,0.00001};
//        double[] etas = {0.1};

        // the model convergence parameter
        double epsilon = 0.001;

        // define the fidelity of the ROC calculation - greater fidelity = more FPR x TPR rates
        int sigmaFidelity = 20;

        LinearChurnDetectionModelTuner runner = new LinearChurnDetectionModelTuner();

        // tune the hyperparameters for the different platforms and lifecycle stages fidelity settings
        for (double alpha : alphas) {
            for (int learningProcedure : learningProcedures) {
                System.out.println("\nAlpha = " + alpha + " | Learning Procedure = " + learningProcedure);
                for (String platform : platforms) {
                    for (int k : ks) {
                        System.out.println("\nPlatform: " + platform + " | k = " + k);
                        runner.tuneHyperParameters(platform, k,
                                mode, foldCount,
                                alpha,
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
                                    int mode, int foldCount,
                                    double alpha,
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
//                System.out.println(training.getInstances().length);

                // split the data into folds
                DatasetSplitter splitter = new DatasetSplitter(training);
                TreeMap<Integer,Dataset> folds = splitter.splitToFolds(foldCount);

                // run the prediction over the training set using CV
                TreeMap<Integer,Double> foldToROCScores = this.runCV(folds,
                        lambda, eta, alpha, epsilon,
                        learningProcedure,
                        sigmaFidelity);
                // derive the average of the fold scores
                double avgROC = this.deriveAvgROC(foldToROCScores);
                DecimalFormat df = new DecimalFormat("#.#####");
                System.out.println("lambda = " + lambda + " | eta = " + eta + " | Avg ROC = " + df.format(avgROC));

                // append the results to the buffer
                buffer.append(lambda + "\t" + eta + "\t" + df.format(avgROC) + "\n");
            }
        }

        // write the resuls to the file
        String filePathString = "data/results/" + platform + "/linear/tuningresults_" + k + "_model_alpha_"
                + alpha + "_learning_" + learningProcedure + ".tsv";
        try {
            PrintWriter writer = new PrintWriter(filePathString);
            writer.write(buffer.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public TreeMap<Integer,Double> runCV(TreeMap<Integer,Dataset> folds,
                                         double lambda, double eta, double alpha, double epsilon,
                                         int learningProcedure,
                                         int sigmaFidelity) {
        TreeMap<Integer,Double> foldToROCScores = new TreeMap<Integer, Double>();

        // go through each fold
        for (Integer foldID : folds.keySet()) {
//            System.out.println("\n-Running fold: " + foldID);

            // compile the inner training and test datasets
            Dataset test = folds.get(foldID);

            // compile the training data from the other folds
            ArrayList<Instance> trainingFoldInstances = new ArrayList<Instance>();
            for (Integer foldID2 : folds.keySet()) {
                if(!foldID.equals(foldID2)) {
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
            LinearLearningProcedure procedure = null;
            switch(learningProcedure) {
                case AD:
                    procedure = new LinearAverageDescentLearningProcedure();
                    break;
                default:
                    procedure = new LinearStochasticDescentLearningProcedure();
                    break;
            }

            procedure.setPredictionModel(lambda,eta,alpha,epsilon,
                    sigmaFidelity,
                    test);

            // clean dataset - remove the elements from the training and testing sets that contain fewer than the maximum number of features
            training = cleanDataset(training,procedure.getM());
            test = cleanDataset(test,procedure.getM());

            // train it
            procedure.trainModel(training);

            // test it
            double roc = procedure.evaluateModel(test);
//            System.out.println(roc);

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
