package uk.ac.lancs.socialcomp.identity.statistics;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 23/05/2013 / 12:53
 */
public class Interval {

    Date startInterval;
    Date endInterval;
    ArrayList<String> posts;

    public Interval(Date startInterval, Date endInterval, ArrayList<String> posts) {
        this.startInterval = startInterval;
        this.endInterval = endInterval;
        this.posts = posts;
    }

    public Date getStartInterval() {
        return startInterval;
    }

    public void setStartInterval(Date startInterval) {
        this.startInterval = startInterval;
    }

    public Date getEndInterval() {
        return endInterval;
    }

    public void setEndInterval(Date endInterval) {
        this.endInterval = endInterval;
    }

    public ArrayList<String> getPosts() {
        return posts;
    }

    public void setPosts(ArrayList<String> posts) {
        this.posts = posts;
    }
}
