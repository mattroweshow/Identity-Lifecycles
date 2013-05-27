package uk.ac.lancs.socialcomp.distribution;

import java.util.HashMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 17/05/2013 / 12:48
 */
public class DiscreteProbabilityDistribution {

    String distributionName;
    int timePeriod;
    HashMap<String,Double> probValues;

    public DiscreteProbabilityDistribution() {
    }

    public DiscreteProbabilityDistribution(String distributionName, int timePeriod) {
        this.distributionName = distributionName;
        this.timePeriod = timePeriod;
        this.probValues = new HashMap<String, Double>();
    }

    public DiscreteProbabilityDistribution(String distributionName, int timePeriod, HashMap<String, Double> probValues) {
        this.distributionName = distributionName;
        this.timePeriod = timePeriod;
        this.probValues = probValues;
    }

    public String getDistributionName() {
        return distributionName;
    }

    public void setDistributionName(String distributionName) {
        this.distributionName = distributionName;
    }

    public int getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(int timePeriod) {
        this.timePeriod = timePeriod;
    }

    public HashMap<String, Double> getProbValues() {
        return probValues;
    }

    public void setProbValues(HashMap<String, Double> probValues) {
        this.probValues = probValues;
    }

    public double getProbValue(String name) {
        return this.probValues.get(name);
    }

    public void setProbValue(String name, double value) {
        this.probValues.put(name,value);
    }

    @Override
    public String toString() {
        return "DiscreteProbabilityDistribution{" +
                "distributionName='" + distributionName + '\'' +
                ", timePeriod=" + timePeriod +
                ", probValues=" + probValues.size() +
                '}';
    }
}
