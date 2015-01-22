package uk.ac.lancs.socialcomp.prediction.features;

import java.util.HashMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2014 / 16:46
 */
public interface FeatureProperties {

    final int INDEGREE = 1;
    final int OUTDEGREE = 2;
    final int LEXICAL = 3;

    final int PERIODENTROPY = 1;
    final int HISTORICALENTROPY = 2;
    final int COMMUNITYENTROPY = 3;

    final int STATIC = 1;
    final int RATE = 2;
}
