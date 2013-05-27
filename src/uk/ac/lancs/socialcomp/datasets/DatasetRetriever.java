package uk.ac.lancs.socialcomp.datasets;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 15/05/2013 / 14:14
 */
public class DatasetRetriever {

    public HashSet<String> getTrainingUsers(String DB) {
        HashSet<String> users = new HashSet<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("data/datasets/" + DB + "/training_split.tsv"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                users.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    public HashSet<String> getValidationUsers(String DB) {
        HashSet<String> users = new HashSet<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("data/datasets/" + DB + "/validation_split.tsv"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                users.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    public HashSet<String> getTestingUsers(String DB) {
        HashSet<String> users = new HashSet<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("data/datasets/" + DB + "/testing_split.tsv"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                users.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
}
