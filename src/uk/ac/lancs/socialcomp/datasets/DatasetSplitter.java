package uk.ac.lancs.socialcomp.datasets;

import uk.ac.lancs.socialcomp.io.Database;
import uk.ac.lancs.socialcomp.io.QueryGrabber;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
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

    public String DB;

    public static void main(String[] args) {
//        String[] dbNames = {"boards","facebook","sap","serverfault"};
        String[] dbNames = {"boards"};
        for (String dbName : dbNames) {
            DatasetSplitter splitter = new DatasetSplitter(dbName);
            splitter.splitUsers();
        }
    }

    public DatasetSplitter(String DB) {
        this.DB = DB;
    }

    /*
    * Splits users up into 3 chunks for training, validation, testing according to 70, 10, 20% splits
    */
    public void splitUsers() {

        try {

            // get the list of user labels (from the churn deriver file)
            ArrayList<String> users = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new FileReader("data/logs/" + DB + "_user_churn_labels.tsv"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] toks = line.split("\t");
                if(toks.length ==2) {
                    users.add(toks[0]);
                }
            }

            // randomly split those users into 80% training and 20% test users
            HashSet<String> training = new HashSet<String>();
            HashSet<String> testing = new HashSet<String>();

            // work out the size of each split
            int trainSize = (int)(users.size() * 0.8);
            int testSize = (int)(users.size() * 0.2);

            // generate the training set
            while(training.size() < trainSize) {
                int randIndex = (int)(Math.random() * users.size());
                String user = users.get(randIndex);
                if(!training.contains(user))
                    training.add(user);
            }

            // generate the testing set
            while(testing.size() < testSize) {
                int randIndex = (int)(Math.random() * users.size());
                String user = users.get(randIndex);
                if((!training.contains(user) && !testing.contains(user)))
                    testing.add(user);
            }

            // write the datasets to files
            this.writeDatasetToFile("training",training);
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
