package uk.ac.lancs.socialcomp.datasets;

import uk.ac.lancs.socialcomp.prediction.features.FeatureProperties;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 11/09/2014 / 10:50
 * Comments: This class is used to merge together disparate feature computation files that have been derived from
 * parallelised computation.
 */
public class ParallelisedFeatureCombiner implements FeatureProperties {

    String platform;
    String split;

    public ParallelisedFeatureCombiner(String platform, String split) {
        this.platform = platform;
        this.split = split;
    }

    public void combineFeatures() {
        int[] featureIDs = {1,2,3};
        int[] entropyIDs = {1,2,3};
        int[] fidelities = {5,10,20};

        for (int featureID : featureIDs) {
            for (int entropyID : entropyIDs) {
                for (int fidelity : fidelities) {
                    combineFeatureFiles(featureID, entropyID, fidelity);
                }
            }
        }
    }


    public void combineFeatureFiles(int featureID, int entropyID, int fidelity) {
        // get the file name
        String fileName = platform + "_" + split + "_";

        // set the feature name
        switch(featureID) {
            case INDEGREE:
                fileName += "indegree_";
                break;
            case OUTDEGREE:
                fileName += "outdegree_";
                break;
            case LEXICAL:
                fileName += "lexical_";
                break;
        }
        String outputFileName = fileName;

        // set the entropy name
        switch (entropyID) {
            case PERIODENTROPY:
                fileName += "entropies_stages_";
                outputFileName += "entropies_stages_";
                break;
            case HISTORICALENTROPY:
                fileName += "crossentropies_stages_";
                outputFileName += "user_crossentropies_stages_";
                break;
            case COMMUNITYENTROPY:
                fileName += "community_crossentropies_stages_";
                outputFileName += "community_crossentropies_stages_";
                break;
        }

        // add the fidelity
        fileName += fidelity + ".tsv";
        outputFileName += fidelity + ".tsv";

        // go through each of the active slave folders to get the computation of this feature
        String pathToSlaveFolders = "/Users/mrowe/Documents/Experiments/lifecycles/feature-computation/";
        String fileContents = retrieveFileContents(fileName, pathToSlaveFolders, "master");
        for (int i = 1; i <= 6; i++) {
            fileContents += retrieveFileContents(fileName, pathToSlaveFolders, "slave"+i);
        }

        // write the content to the file
        try {
            System.out.println("-Writing to the file: " + fileName);
            PrintWriter writer = new PrintWriter("data/logs/" + platform + "/" + outputFileName);
            writer.write(fileContents);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String retrieveFileContents(String fileName, String basePath, String nodeName) {
        // read in the contents of a file to a line delineated string
        String fileContents = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(basePath + nodeName + "/" + fileName));
            String line;
            while ((line = br.readLine()) != null) {
                fileContents += line + "\n";
            }
            // catch an exception if the file does not exist: this happens if the node has not computed this setting.
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return fileContents;
    }

    public static void main(String[] args) {

        String platform = "boards";
        String[] splits = {"test", "train"};

        for (String split : splits) {
            ParallelisedFeatureCombiner combiner = new ParallelisedFeatureCombiner(platform, split);
            combiner.combineFeatures();
        }
    }

}
