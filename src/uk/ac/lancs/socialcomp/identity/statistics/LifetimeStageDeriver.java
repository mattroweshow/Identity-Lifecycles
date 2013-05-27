package uk.ac.lancs.socialcomp.identity.statistics;

import java.util.*;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 23/05/2013 / 12:43
 */
public class LifetimeStageDeriver {

    Lifetime lifetime;
    HashMap<String,Date> postToDate;

    public LifetimeStageDeriver(Lifetime lifetime, HashMap<String,Date> postToDate) {
        this.lifetime = lifetime;
        this.postToDate = postToDate;
    }

    public TreeMap<Date,Interval> deriveStageIntervals(int intervalCount) {

        // work out the chunks of posts
        int postsTotal = this.lifetime.getPosts().size();
        int chunkSize = postsTotal / intervalCount;
//        chunkSize++;    // increase this in case of outliers

        // order the posts by date
        HashMap<String,Date> userPostsToDates = new HashMap<String, Date>();
        for (String post : this.lifetime.getPosts()) {
            Date postDate = postToDate.get(post);
            userPostsToDates.put(post,postDate);
        }
        ValueComparator bvc =  new ValueComparator(userPostsToDates);
        TreeMap<String,Date> sortedUserPostsToDates = new TreeMap<String, Date>(bvc);
        sortedUserPostsToDates.putAll(userPostsToDates);

        // convert to an array list containing the post ids
        ArrayList<String> orderedPosts = new ArrayList<String>();
        for (String post : sortedUserPostsToDates.keySet()) {
            orderedPosts.add(post);
        }

        // derive the intervals for the user
        TreeMap<Date,Interval> intervals = new TreeMap<Date, Interval>();
        for (int i = 0; i < intervalCount; i++) {
            int innerIndexStart = i * chunkSize;
            int innerIndexEnd = (i+1) * chunkSize;
            if(innerIndexEnd > this.lifetime.posts.size())
                innerIndexEnd = this.lifetime.posts.size();

            ArrayList<String> innerPosts = new ArrayList<String>();
            boolean afterFirst = false;
            Date startInterval = null;
            Date endInterval = null;

            for (int j = innerIndexStart; j < innerIndexEnd; j++) {
                String post = orderedPosts.get(j);
                innerPosts.add(post);
                if(afterFirst) {
                    endInterval = postToDate.get(post);
                } else {
                    afterFirst = true;
                    startInterval = postToDate.get(post);
                }
            }

            // create an interval object and record the data
            Interval interval = new Interval(startInterval,endInterval,innerPosts);
            intervals.put(startInterval,interval);
        }

        return intervals;
    }

    // inner class for ordering by date
    class ValueComparator implements Comparator<String> {

        Map<String, Date> base;
        public ValueComparator(Map<String, Date> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.
        public int compare(String a, String b) {
            if (base.get(a).after(base.get(b))) {
                return 1;
            } else {
                return -1;
            } // returning 0 would merge keys
        }
    }
}
