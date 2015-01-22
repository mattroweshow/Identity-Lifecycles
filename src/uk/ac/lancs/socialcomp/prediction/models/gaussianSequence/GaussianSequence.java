package uk.ac.lancs.socialcomp.prediction.models.gaussianSequence;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 18/07/2014 / 14:09
 */
public class GaussianSequence {

    TreeMap<Integer,GaussianDistribution> churnGaussianSequence;
    TreeMap<Integer,GaussianDistribution> nonChurnGaussianSequence;

    HashMap<Integer,String> featureIndexToLabel;
    HashMap<String,Integer> featureLabelToIndex;

    int fidelity;

    public GaussianSequence(TreeMap<Integer, GaussianDistribution> churnGaussianSequence,
                            TreeMap<Integer, GaussianDistribution> nonChurnGaussianSequence,
                            HashMap<Integer, String> featureIndexToLabel,
                            HashMap<String, Integer> featureLabelToIndex,
                            int fidelity) {
        this.churnGaussianSequence = churnGaussianSequence;
        this.nonChurnGaussianSequence = nonChurnGaussianSequence;
        this.featureIndexToLabel = featureIndexToLabel;
        this.featureLabelToIndex = featureLabelToIndex;
        this.fidelity = fidelity;
    }

    public GaussianSequence() {
    }

    public TreeMap<Integer, GaussianDistribution> getChurnGaussianSequence() {
        return churnGaussianSequence;
    }

    public void setChurnGaussianSequence(TreeMap<Integer, GaussianDistribution> churnGaussianSequence) {
        this.churnGaussianSequence = churnGaussianSequence;
    }

    public TreeMap<Integer, GaussianDistribution> getNonChurnGaussianSequence() {
        return nonChurnGaussianSequence;
    }

    public void setNonChurnGaussianSequence(TreeMap<Integer, GaussianDistribution> nonChurnGaussianSequence) {
        this.nonChurnGaussianSequence = nonChurnGaussianSequence;
    }

    public HashMap<Integer, String> getFeatureIndexToLabel() {
        return featureIndexToLabel;
    }

    public void setFeatureIndexToLabel(HashMap<Integer, String> featureIndexToLabel) {
        this.featureIndexToLabel = featureIndexToLabel;
    }

    public HashMap<String, Integer> getFeatureLabelToIndex() {
        return featureLabelToIndex;
    }

    public void setFeatureLabelToIndex(HashMap<String, Integer> featureLabelToIndex) {
        this.featureLabelToIndex = featureLabelToIndex;
    }

    public int getFidelity() {
        return fidelity;
    }

    public void setFidelity(int fidelity) {
        this.fidelity = fidelity;
    }

    @Override
    public String toString() {
        return "GaussianSequence{" +
                "churnGaussianSequence=" + churnGaussianSequence +
                ", nonChurnGaussianSequence=" + nonChurnGaussianSequence +
                ", featureIndexToLabel=" + featureIndexToLabel.size() +
                ", featureLabelToIndex=" + featureLabelToIndex.size() +
                ", fidelity=" + fidelity +
                '}';
    }
}
