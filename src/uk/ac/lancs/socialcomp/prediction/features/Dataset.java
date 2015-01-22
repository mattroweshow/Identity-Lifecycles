package uk.ac.lancs.socialcomp.prediction.features;

import java.util.ArrayList;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2014 / 17:03
 */
public class Dataset {

    String platform;
    String split;
    int k;
    Instance[] instances;

    public Dataset(String platform, String split, int k, Instance[] instances) {
        this.platform = platform;
        this.split = split;
        this.k = k;
        this.instances = instances;
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

    public Instance[] getInstances() {
        return instances;
    }

    public void setInstances(Instance[] instances) {
        this.instances = instances;
    }

    @Override
    public String toString() {
        return "Dataset{" +
                "platform='" + platform + '\'' +
                ", split='" + split + '\'' +
                ", k=" + k +
                ", instances (size)=" + instances.length +
                '}';
    }
}
