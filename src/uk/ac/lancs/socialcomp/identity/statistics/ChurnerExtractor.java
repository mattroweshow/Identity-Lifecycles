package uk.ac.lancs.socialcomp.identity.statistics;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 05/06/2013 / 17:26
 */
public class ChurnerExtractor {

    String DB;

    public ChurnerExtractor(String DB) {
        this.DB = DB;
    }

    /*
    * Returns a mapping between a user and whether they were a churner given the 90% cutoff point
    */
    public HashMap<String,Integer> deriveChurnersAndNonChurners() {
        HashMap<String,Integer> userToLabel = new HashMap<String, Integer>();
        try {
            // Get the cutoff date for the platform
            Properties properties = new Properties();
            properties.load(new FileInputStream("data/properties/" + DB + "-stats.properties"));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date cutoff = sdf.parse(properties.getProperty("churn_cutoff").toString());

            // get the lifetimes of each user
            LifeTimeExtractor lifeTimeExtractor = new LifeTimeExtractor(this.DB);
            List<Lifetime> lifeTimes = lifeTimeExtractor.deriveLifetimes();

            for (Lifetime lifeTime : lifeTimes) {
                // get the end of the lifeTime
                Date death = lifeTime.getDeath();
                if(death.before(cutoff)) {
                    userToLabel.put(lifeTime.getUserid(),1);
                } else {
                    userToLabel.put(lifeTime.getUserid(), 0);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return userToLabel;
    }

    /*
     * Writes the result of extracting the churners to a file
     */
    public void writeUserToChurnOrNonChurnLabels(HashMap<String,Integer> userToLabel) {
        StringBuffer buffer = new StringBuffer();
        for (String s : userToLabel.keySet()) {
            buffer.append(s + "\t" + userToLabel.get(s) + "\n");
        }

        try {
            // write this to a file
            PrintWriter writer = new PrintWriter("data/logs/" + DB + "_user_churn_labels.tsv");
            writer.write(buffer.toString());
            writer.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        String[] dbNames = {"facebook","sap","serverfault"};

        for (String dbName : dbNames) {
            System.out.println("Getting the churners and non-churners from: " + dbName);
            ChurnerExtractor churnerExtractor = new ChurnerExtractor(dbName);
            HashMap<String,Integer> userToLabel = churnerExtractor.deriveChurnersAndNonChurners();
            churnerExtractor.writeUserToChurnOrNonChurnLabels(userToLabel);
        }




    }

}
