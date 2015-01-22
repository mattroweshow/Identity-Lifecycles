package uk.ac.lancs.socialcomp.prediction;

import uk.ac.lancs.socialcomp.prediction.learningProcedures.gaussian.GaussianLearningProcedureTypes;
import uk.ac.lancs.socialcomp.prediction.models.gaussianSequence.GaussianModelConfiguration;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 04/08/2014 / 16:48
 */
public class ChurnDetectionModelLaterHyperparameters implements GaussianLearningProcedureTypes {

    public static void main(String[] args) {
        String[] platforms = {"facebook", "sap", "serverfault"};
//        String[] platforms = {"facebook"};

        // lifecycle fidelity setting
        int[] ks = {5,10,20};

        // set the experiment's mode
        int mode = 1;   // detection at present

        // set the smoothing parameter
        double rho = 0.1;


        // Run 1: using specific settings
        ChurnDetectionModelLaterHyperparameters runner = new ChurnDetectionModelLaterHyperparameters();

        // Run 2: using the best tuned hyperparameters
        int[] models = {1,2};
        int[] learningProcedures = {1,2};
        for (String platform : platforms) {
            System.out.println(platform + " ");
            for (int k : ks) {
                String line = " & " + k;
                for (int procedure : learningProcedures) {
                    for (int modelA : models) {
                        line += " & " + runner.getBestConfigRendered(platform,k,mode,modelA,procedure,rho);
                    }
                }
                line += " \\\\";
                System.out.println(line);
            }

        }

    }

    public String getBestConfigRendered(String platform, int k,
                             int mode,
                             int model, int learningProcedure,
                             double rho) {

        // get the best model configuration
        GaussianBestHyperparameterSelection selection = new GaussianBestHyperparameterSelection(platform,  k, model, learningProcedure, rho);
        GaussianModelConfiguration configuration = selection.getBestTunedModelConfiguration();

        // format the string to be a latex rendered version
        // lambda eta
        return new String("$ \\lambda=" + configuration.getLambda() + ", \\eta=" + configuration.getEta() + " $");





    }
}
