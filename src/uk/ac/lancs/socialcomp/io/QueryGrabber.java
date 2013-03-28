package uk.ac.lancs.socialcomp.io;

import java.io.*;

public class QueryGrabber {


    /*
     * Given the db and the query, this method returns the SQL from the local file
     */
    public static String getQuery(String DB, String query) {

        String filePath = "." + File.separator + "data"
                + File.separator + "queries"
                + File.separator + DB
                + File.separator + query + ".sql";

        StringBuffer output = new StringBuffer();

        try {
            FileInputStream fis = new FileInputStream(new File(filePath));
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String content;
            while ((content = br.readLine()) != null) {
                output.append(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return output.toString();
    }

}
