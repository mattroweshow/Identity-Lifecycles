package uk.ac.lancs.socialcomp.identity.parallelised;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 31/07/2014 / 13:30
 */
public class JobResult {
    public String platform;
    public String split;
    public int k;
    public String userid;
    public int featureType;
    public int measureType;

    public boolean validUser;

    public String outputValue;

    public JobResult(String platform, String split, int k, String userid, int featureType, int measureType, String outputValue, boolean validUser) {
        this.platform = platform;
        this.split = split;
        this.k = k;
        this.userid = userid;
        this.featureType = featureType;
        this.measureType = measureType;
        this.validUser = validUser;
        this.outputValue = outputValue;
    }

    public JobResult() {
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSplit() {
        return split;
    }

    public void setSplit(String split) {
        this.split = split;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public int getFeatureType() {
        return featureType;
    }

    public void setFeatureType(int featureType) {
        this.featureType = featureType;
    }

    public int getMeasureType() {
        return measureType;
    }

    public void setMeasureType(int measureType) {
        this.measureType = measureType;
    }

    public boolean isValidUser() {
        return validUser;
    }

    public void setValidUser(boolean validUser) {
        this.validUser = validUser;
    }

    public String getOutputValue() {
        return outputValue;
    }

    public void setOutputValue(String outputValue) {
        this.outputValue = outputValue;
    }

    @Override
    public String toString() {
        return "JobResult{" +
                "platform='" + platform + '\'' +
                ", split='" + split + '\'' +
                ", k=" + k +
                ", userid='" + userid + '\'' +
                ", featureType=" + featureType +
                ", measureType=" + measureType +
                ", validUser=" + validUser +
                ", outputValue='" + outputValue + '\'' +
                '}';
    }
}
