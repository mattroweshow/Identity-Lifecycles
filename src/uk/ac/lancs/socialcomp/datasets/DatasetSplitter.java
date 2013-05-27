package uk.ac.lancs.socialcomp.datasets;

import uk.ac.lancs.socialcomp.io.Database;
import uk.ac.lancs.socialcomp.io.QueryGrabber;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2013 / 13:38
 */
public class DatasetSplitter {

    public static final String DB = "facebook";

    public static void main(String[] args) {

        DatasetSplitter splitter = new DatasetSplitter();
        splitter.splitUsers();

    }

    /*
     * Splits users up into 3 chunks for training, validation, testing according to 70, 10, 20% splits
     */
    public void splitUsers() {

        try {

            // get the cutoff point for user activity
            Properties properties = new Properties();
            properties.load(new FileInputStream("data/properties/" + DB + "-stats.properties"));
            int cutoff = Integer.parseInt(properties.getProperty("posts_mean"));

            // get the users from that platform and their activity
            String query = QueryGrabber.getQuery(DB, "getPostDetails");

            HashMap<String,HashSet<String>> userToPosts = new HashMap<String, HashSet<String>>();

            // set up the db connection
            Connection connection = Database.getConnection(DB);

            // query the db
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            while(results.next())  {
                String postid = results.getString("messageuri");
                String userid = results.getString("contributor");

                // insert into the maps
                if(userToPosts.containsKey(userid)) {
                    HashSet<String> posts = userToPosts.get(userid);
                    posts.add(postid);
                    userToPosts.put(userid,posts);
                } else {
                    HashSet<String> posts = new HashSet<String>();
                    posts.add(postid);
                    userToPosts.put(userid,posts);
                }
            }
            statement.close();

            // filter through users whose activity is greater than the threshold
            ArrayList<String> filteredUsers = new ArrayList<String>();
            for (String user : userToPosts.keySet()) {
                if(userToPosts.get(user).size() > cutoff)
                    filteredUsers.add(user);
            }

            // split the users into 70, 10, 20 splits using random ordering
            HashSet<String> training = new HashSet<String>();
            HashSet<String> validation = new HashSet<String>();
            HashSet<String> testing = new HashSet<String>();

            // work out the size of each split
            int trainSize = (int)(filteredUsers.size() * 0.7);
            int validSize = (int)(filteredUsers.size() * 0.1);
            int testSize = (int)(filteredUsers.size() * 0.2);

            // generate the training set
            while(training.size() < trainSize) {
                int randIndex = (int)(Math.random() * filteredUsers.size());
                String user = filteredUsers.get(randIndex);
                if(!training.contains(user))
                    training.add(user);
            }

            // generate the validation set
            while(validation.size() < validSize) {
                int randIndex = (int)(Math.random() * filteredUsers.size());
                String user = filteredUsers.get(randIndex);
                if(!training.contains(user) && !validation.contains(user))
                    validation.add(user);
            }

            // generate the testing set
            while(testing.size() < testSize) {
                int randIndex = (int)(Math.random() * filteredUsers.size());
                String user = filteredUsers.get(randIndex);
                if((!training.contains(user) && !validation.contains(user)) && !testing.contains(user))
                    testing.add(user);
            }

            // write the datasets to files
            this.writeDatasetToFile("training",training);
            this.writeDatasetToFile("validation",validation);
            this.writeDatasetToFile("testing",testing);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void writeDatasetToFile(String split, HashSet<String> users) {
        try {

            StringBuffer buffer = new StringBuffer();

            for (String user : users) {
                buffer.append(user + "\n");
            }

            PrintWriter writer = new PrintWriter("data/datasets/" + DB + "/" + split + "_split.tsv");
            writer.write(buffer.toString());
            writer.close();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }


}
