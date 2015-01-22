package uk.ac.lancs.socialcomp.prediction.features;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2014 / 16:47
 */
public class FeatureBuilder implements FeatureProperties {

    String DB;
    String split;
    int k;

    public FeatureBuilder(String DB, String split, int k) {
        this.DB = DB;
        this.split = split;
        this.k = k;
    }

    /*
     * Gets all of the features given the dynamic and entropy measure
     * Returns a map between the user and his features
     */
    public HashMap<String,ArrayList<Feature>> getStaticFeatures(int dynamic, int entropyMeasure) {
        HashMap<String,ArrayList<Feature>> userToFeatures = new HashMap<String, ArrayList<Feature>>();

        // given the dynamic and the entropy measurre, read in the relevant file for the platform
        String filePath = "data/logs/" + DB + "/" + DB + "_" + split + "_";

        // modify the filepath
        // 1. based on the dynamic
        switch(dynamic) {
            case INDEGREE:
                filePath += "indegree_";
                break;
            case OUTDEGREE:
                filePath += "outdegree_";
                break;
            case LEXICAL:
                filePath += "lexical_";
                break;
        }

        // 2. based on the entropy measure
        switch (entropyMeasure) {
            case PERIODENTROPY:
                filePath += "entropies_stages_";
                break;
            case HISTORICALENTROPY:
                filePath += "user_crossentropies_stages_";
                break;
            case COMMUNITYENTROPY:
                filePath += "community_crossentropies_stages_";
                break;
        }

        // append on the final part
        filePath += k + ".tsv";
//        System.out.println(filePath);

        // read in the file line by line
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null) {
                // split the line by the tab delimiter
                String[] toks = line.split("\t");

                // collect the user features
                ArrayList<Feature> userFeatures = new ArrayList<Feature>();

                // depending on the number of tokens in the line, the next 4 or 5 tokens are the stage specific features
                if(toks.length == k) {
                    String userid = toks[0];

                    for (int i = 1; i < k; i++) {
                        double value = Double.parseDouble(toks[i]);
                        Feature feature = new Feature(dynamic,entropyMeasure,STATIC,k,userid,i+1,value);
                        userFeatures.add(feature);
                    }

                    // map the user to his features
                    userToFeatures.put(userid, userFeatures);

                } else if(toks.length == (k+1)) {
                    String userid = toks[0];

                    for (int i = 1; i <= k; i++) {
                        double value = Double.parseDouble(toks[i]);
                        Feature feature = new Feature(dynamic,entropyMeasure,STATIC,k,userid,i,value);
                        userFeatures.add(feature);
                    }

                    // map the user to his features
                    userToFeatures.put(userid, userFeatures);
                }
//                System.out.println(lineCount);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return userToFeatures;
    }


    /*
    * Gets all of the rate features given the dynamic and entropy measure
    * Returns a map between the user and his features
    */
    public HashMap<String,ArrayList<Feature>> getRateFeatures(int dynamic, int entropyMeasure) {
        HashMap<String,ArrayList<Feature>> userToFeatures = new HashMap<String, ArrayList<Feature>>();

        // given the dynamic and the entropy measurre, read in the relevant file for the platform
        String filePath = "data/logs/" + DB + "/" + DB + "_" + split + "_";

        // modify the filepath
        // 1. based on the dynamic
        switch(dynamic) {
            case INDEGREE:
                filePath += "indegree_";
                break;
            case OUTDEGREE:
                filePath += "outdegree_";
                break;
            case LEXICAL:
                filePath += "lexical_";
                break;
        }

        // 2. based on the entropy measure
        switch (entropyMeasure) {
            case PERIODENTROPY:
                filePath += "entropies_stages_";
                break;
            case HISTORICALENTROPY:
                filePath += "user_crossentropies_stages_";
                break;
            case COMMUNITYENTROPY:
                filePath += "community_crossentropies_stages_";
                break;
        }

        // append on the final part
        filePath += k + ".tsv";

        // read in the file line by line
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null) {
                // split the line by the tab delimiter
                String[] toks = line.split("\t");

                // collect the user features
                ArrayList<Feature> userFeatures = new ArrayList<Feature>();

                // depending on the number of tokens in the line, the next 4 or 5 tokens are the stage specific features
                if(toks.length == k) {
                    String userid = toks[0];

                    for (int i = 2; i < k; i++) {                          
                        double valueI = Double.parseDouble(toks[i-1]);
                        double valueJ = Double.parseDouble(toks[i]);
                        double rate = computeRate(valueI,valueJ);
                        Feature feature = new Feature(dynamic,entropyMeasure,RATE,k,userid,i+1,rate);
                        userFeatures.add(feature);
                    }                                        

                    // map the user to his features
                    userToFeatures.put(userid, userFeatures);

                } else if(toks.length == k+1) {
                    String userid = toks[0];

                    for (int i = 1; i < k; i++) {

                        double valueI = Double.parseDouble(toks[i]);
                        double valueJ = Double.parseDouble(toks[i+1]);
                        double rate = computeRate(valueI,valueJ);
                        Feature feature = new Feature(dynamic,entropyMeasure,RATE,k,userid,i+1,rate);
                        userFeatures.add(feature);
                    }

                    // map the user to his features
                    userToFeatures.put(userid, userFeatures);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return userToFeatures;
    }

    // Convenience function to work out the rate change from r to s in the lifecycle static feature
    private double computeRate(double valueR, double valueS) {

        // work out the proportionate change from r to s:
        double numerator = valueS - valueR;
        double denominator = 1;
        if(valueR != 0)
            denominator = valueR;

        double rate = 0;
        if(numerator != 0)
            rate = numerator / denominator;

        return rate;
    }
}
