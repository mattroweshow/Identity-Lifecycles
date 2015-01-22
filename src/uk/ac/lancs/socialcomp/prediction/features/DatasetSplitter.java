package uk.ac.lancs.socialcomp.prediction.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 21/07/2014 / 12:15
 */
public class DatasetSplitter {

    Dataset dataset;

    public DatasetSplitter(Dataset dataset) {
        this.dataset = dataset;
    }

    public DatasetSplitter() {
    }

    public TreeMap<Integer, Dataset> splitToFolds(int foldCount) {
        TreeMap<Integer, Dataset> folds = new TreeMap<Integer, Dataset>();

        // work out the segment size
        int segmentSize = dataset.getInstances().length / foldCount;

        // work out the indexation of indices
        for (int i = 0; i < (foldCount-1); i++) {

            // get the fold
            Instance[] foldInstances = new Instance[segmentSize];
            int foldElementIndex = 0;

            for (int j = (i*segmentSize); j < ((i+1)*segmentSize); j++) {
                Instance foldInstance = dataset.getInstances()[j];
                foldInstances[foldElementIndex] = foldInstance;
                foldElementIndex++;
            }

            // generate the dataset object for the fold
            Dataset fold = new Dataset(dataset.platform, dataset.split, dataset.getK(), foldInstances);
            folds.put(i, fold);
        }

        return folds;
    }

    /*
     * Method for deriving the folds from the local disk - where the mapping between fold id and user id is persisted
     * If the file does not exist then a new file is created
     */
    public TreeMap<Integer, Dataset> getSplitFoldsFromFile(String platform, int k, int foldCount, String split) {
        // check that the persisted fold mapping file exists
        String filePathString = "";
        File f = new File(filePathString);

        TreeMap<Integer, Dataset> folds;

        // if so, then load the folds from the file
        if(f.exists()) {
            folds = deriveFoldsFromLocalFile(platform, k, foldCount, split);
        // if not, then derive the folds and do the mapping
        } else {
            folds = splitToFolds(foldCount);
            // write the mapping to a file
            writeFoldsToLocalFile(platform, k, foldCount, folds, split);
        }

        return folds;
    }

    private TreeMap<Integer, Dataset> deriveFoldsFromLocalFile(String platform, int k, int foldCount,
                                                               String split) {
        TreeMap<Integer, Dataset> folds = new TreeMap<Integer, Dataset>();

        // read in the fold mapping
        TreeMap<Integer, HashSet<String>> foldToUsers = new TreeMap<Integer, HashSet<String>>();
        try {
            String filePath = "data/datasets/" + platform + "/"
                    + split + "_stages_" + k + "_folds_" + foldCount + ".tsv";
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null) {
                // split the line by the tab delimiter
                String[] toks = line.split("\t");

                // first token is the fold id
                int foldID = Integer.parseInt(toks[0]);
                // second token is the user id
                String userid = toks[1];

                // do the mapping
                if(foldToUsers.containsKey(foldID)) {
                    HashSet<String> users = foldToUsers.get(foldID);
                    users.add(userid);
                    foldToUsers.put(foldCount,users);
                } else {
                    HashSet<String> users = new HashSet<String>();
                    users.add(userid);
                    foldToUsers.put(foldCount,users);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // given the fold mapping, now load the given fold datasets
        for (Integer foldID : foldToUsers.keySet()) {
            HashSet<String> users = foldToUsers.get(foldID);

            // create a new foldDataset
            Instance[] instances = new Instance[users.size()];
            int instanceIndex = 0;
            for (Instance instance : dataset.getInstances()) {
                if(users.contains(instance.getUserid())) {
                    instances[instanceIndex] = instance;
                    instanceIndex++;
                }
            }
            Dataset foldDataset = new Dataset(platform, split, k, instances);

            // map this to the treemap
            folds.put(foldID,foldDataset);
        }

        return folds;
    }

    private void writeFoldsToLocalFile(String platform, int k, int foldCount,
                                       TreeMap<Integer, Dataset> folds, String split) {

        // map the fold id to the user
        StringBuffer buffer = new StringBuffer();
        for (Integer foldID : folds.keySet()) {
            Dataset foldDataset = folds.get(foldID);
            for (Instance instance : foldDataset.getInstances()) {
                buffer.append(foldID + "\t" + instance.getUserid() + "\n");
            }
        }

        // write the mapping to a file
        // write the whole thing to a file
        try {
            PrintWriter writer = new PrintWriter("data/datasets/" + platform + "/"
                    + split + "_stages_" + k + "_folds_" + foldCount + ".tsv");
            writer.write(buffer.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }



    }


}
