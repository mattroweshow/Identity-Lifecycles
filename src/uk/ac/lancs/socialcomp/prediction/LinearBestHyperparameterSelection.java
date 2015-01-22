package uk.ac.lancs.socialcomp.prediction;
import uk.ac.lancs.socialcomp.prediction.models.linear.LinearModelConfiguration;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 28/07/2014 / 09:41
 */
public class LinearBestHyperparameterSelection {

    String platform;
    int fidelity;

    int learningProcedure;
    double alpha;

    public LinearBestHyperparameterSelection(String platform, int fidelity, int learningProcedure, double alpha) {
        this.platform = platform;
        this.fidelity = fidelity;
        this.learningProcedure = learningProcedure;
        this.alpha = alpha;
    }

    public LinearModelConfiguration getBestTunedModelConfiguration() {
        LinearModelConfiguration modelConfiguration = new LinearModelConfiguration();

        // go through the tuned value and choose the model configuration with the largest roc value
        String filePathString = "data/results/" + platform + "/linear/tuningresults_" + fidelity
                + "_model_alpha_" + alpha
                + "_learning_" + learningProcedure + ".tsv";

        double bestROC = 0;

        // read in the file line by line
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePathString));
            String line;
            while ((line = br.readLine()) != null) {
                // tokenise the line
                String[] toks = line.split("\t");

                double roc = Double.parseDouble(toks[2]);
                if(roc > bestROC) {
                    bestROC = roc;

                    // get the rest of the parameters and construct the model config object
                    double lambda = Double.parseDouble(toks[0]);
                    double eta = Double.parseDouble(toks[1]);

                    modelConfiguration = new LinearModelConfiguration(platform,fidelity,
                            learningProcedure,
                            lambda, eta,
                            alpha);
                }
            }
        } catch (Exception  e) {
            e.printStackTrace();
        }
        return modelConfiguration;
    }

    public static void main(String[] args) {

        String[] platforms = {"facebook", "sap", "serverfault", "boards"};
        int[] ks = {5,10,20};

        int[] learningProcedures = {1,2};
//        int[] learningProcedures = {1};

        double[] alphas = {0,0.5,1};

        for (String platform : platforms) {
            for (int learningProcedure : learningProcedures) {
                for (double alpha : alphas) {
                    for (int k : ks) {
                        LinearBestHyperparameterSelection selection = new LinearBestHyperparameterSelection(platform, k, learningProcedure, alpha);
                        LinearModelConfiguration configuration = selection.getBestTunedModelConfiguration();
                        System.out.println(configuration);

                    }
                }
            }
        }

    }
}
