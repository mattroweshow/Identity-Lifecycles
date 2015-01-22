package uk.ac.lancs.socialcomp.prediction;

import uk.ac.lancs.socialcomp.prediction.models.gaussianSequence.GaussianModelConfiguration;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 28/07/2014 / 09:41
 */
public class GaussianBestHyperparameterSelection {

    String platform;
    int fidelity;

    int model;
    int learningProcedure;
    double rho;

    public GaussianBestHyperparameterSelection(String platform, int fidelity, int model, int learningProcedure, double rho) {
        this.platform = platform;
        this.fidelity = fidelity;
        this.model = model;
        this.learningProcedure = learningProcedure;
        this.rho = rho;
    }

    public GaussianModelConfiguration getBestTunedModelConfiguration() {
        GaussianModelConfiguration modelConfiguration = new GaussianModelConfiguration();

        // go through the tuned value and choose the model configuration with the largest roc value
        String filePathString = "data/results/" + platform + "/tuningresults_" + fidelity + "_model_"
                + model + "_learning_" + learningProcedure + ".tsv";

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

                    modelConfiguration = new GaussianModelConfiguration(platform,fidelity,
                            model, learningProcedure,
                            lambda, eta,
                            rho);
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

        int[] models = {1,2};
        int[] learningProcedures = {1,2};

        double rho = 0.1;

        for (String platform : platforms) {
            for (int learningProcedure : learningProcedures) {
                for (int model : models) {
                    for (int k : ks) {

                        GaussianBestHyperparameterSelection selection = new GaussianBestHyperparameterSelection(platform, k, model, learningProcedure, rho);
                        GaussianModelConfiguration configuration = selection.getBestTunedModelConfiguration();
                        System.out.println(configuration);

                    }
                }
            }
        }

    }
}
