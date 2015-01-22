package uk.ac.lancs.socialcomp.identity.statistics;

import java.util.TreeMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 31/07/2014 / 14:07
 */
public class UtilFunctions {

    public static String convertToStringVector(TreeMap<Integer,Double> stageProportions) {
        String vector = "";
        boolean afterFirst = false;
        for (Integer stage : stageProportions.keySet()) {
            double proportion = stageProportions.get(stage);
            if(afterFirst) {
                vector += "\t" + proportion;
            } else {
                vector = "" + proportion;
                afterFirst = true;
            }
        }
        return vector;
    }
}
