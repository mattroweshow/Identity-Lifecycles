package uk.ac.lancs.socialcomp.distribution;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 17/05/2013 / 12:52
 */
public class DistributionMeasurer {

    /*
     * Measures the entropy of the random variable, characterised as a discrete probability distribution
     */
    public double measureEntropy(DiscreteProbabilityDistribution pd) {
        double entropy = 0;
        for (String name : pd.probValues.keySet()) {
            double prob = pd.getProbValue(name);
            double innerEntropy = prob * Math.log(prob);
            entropy += innerEntropy;
        }
        if(entropy < 0)
            entropy *= -1;
        return entropy;
    }

    public double measureCrossEntropy(DiscreteProbabilityDistribution pd1, DiscreteProbabilityDistribution pd2) {

        double crossEntropy = 0;
        for (String name : pd1.probValues.keySet()) {
            double probA = pd1.getProbValue(name);
            double probB = 0;
            if(pd2.probValues.containsKey(name)) {
                probB = pd2.getProbValue(name);
                double innerEntropy = probA * Math.log(probB);
                crossEntropy += innerEntropy;
            }
        }
        if(crossEntropy < 0)
            crossEntropy *= -1;
        // check that a comparison has occurred between the distributions
//        if(!compared)
//            crossEntropy = 10000000;
        return crossEntropy;
    }
}
