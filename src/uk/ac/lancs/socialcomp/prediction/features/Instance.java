package uk.ac.lancs.socialcomp.prediction.features;

import java.util.ArrayList;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2014 / 17:12
 */
public class Instance {

    String userid;
    String platform;
    String split;
    ArrayList<Feature> features;
    public double response;

    public Instance(String userid, String platform, String split, ArrayList<Feature> features, double response) {
        this.userid = userid;
        this.platform = platform;
        this.split = split;
        this.features = features;
        this.response = response;
    }

    public Instance() {
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
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

    public ArrayList<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(ArrayList<Feature> features) {
        this.features = features;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "response=" + response +
                ", features=" + features.size() +
                ", split='" + split + '\'' +
                ", platform='" + platform + '\'' +
                ", userid='" + userid + '\'' +
                '}';
    }
}
