package uk.ac.lancs.socialcomp.identity.statistics;

import java.util.Date;

public class Lifetime {
    String userid;
    Date birth;
    Date death;

    public Lifetime(String userid, Date birth, Date death) {
        this.userid = userid;
        this.birth = birth;
        this.death = death;
    }

    public Lifetime() {
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public Date getBirth() {
        return birth;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }

    public Date getDeath() {
        return death;
    }

    public void setDeath(Date death) {
        this.death = death;
    }

    @Override
    public String toString() {
        return "Lifetime{" +
                "userid='" + userid + '\'' +
                ", birth=" + birth +
                ", death=" + death +
                '}';
    }
}
