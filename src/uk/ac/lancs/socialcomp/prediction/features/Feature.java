package uk.ac.lancs.socialcomp.prediction.features;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2014 / 16:44
 */
public class Feature {

    int dynamic;
    int entropy;
    int type;
    int k;
    String userid;
    int s;
    double value;


    public Feature(int dynamic, int entropy, int type, int k, String userid, int s, double value) {
        this.dynamic = dynamic;
        this.entropy = entropy;
        this.type = type;
        this.k = k;
        this.userid = userid;
        this.s = s;
        this.value = value;
    }

    public Feature() {
    }

    public int getDynamic() {
        return dynamic;
    }

    public int getEntropy() {
        return entropy;
    }

    public int getType() {
        return type;
    }

    public int getK() {
        return k;
    }

    public String getUserid() {
        return userid;
    }

    public int getS() {
        return s;
    }

    public double getValue() {
        return value;
    }


    public void setDynamic(int dynamic) {
        this.dynamic = dynamic;
    }


    public void setEntropyMeasure(int entropy) {
        this.entropy = entropy;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setLifecycleFidelity(int k) {
        this.k = k;
    }


    public void setUser(String userid) {
        this.userid = userid;
    }


    public void setLifecycleStage(int s) {
        this.s = s;
    }


    public void setValue(double value) {
        this.value = value;
    }


    public String toString() {
        return "IndegreeFeature{" +
                "dynamic=" + dynamic +
                ", entropy=" + entropy +
                ", k=" + k +
                ", userid='" + userid + '\'' +
                ", s=" + s +
                ", value=" + value +
                '}';
    }
}
