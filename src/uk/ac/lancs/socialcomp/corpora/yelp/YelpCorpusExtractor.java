package uk.ac.lancs.socialcomp.corpora.yelp;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.lancs.socialcomp.io.Database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 14/05/2013 / 14:13
 */
public class YelpCorpusExtractor {

    public static void main(String[] args) {

        String filePath = "/Users/mrowe/IdeaProjects/data/yelp/yelp_training_set/yelp_training_set_review.json";

        YelpCorpusExtractor yelpCorpusExtractor = new YelpCorpusExtractor();
        yelpCorpusExtractor.writeJSONToMySQL(filePath);

    }


    public void writeJSONToMySQL(String filepath) {
        String DB = "yelp";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Connection connection = Database.getConnection(DB);

            // read in the JSON file line by line
            BufferedReader in = new BufferedReader(new FileReader(filepath));
            StringBuffer b = new StringBuffer();
            String str;
            int count = 0;
            while ((str = in.readLine()) != null) {

                // parse the JSON object per line
                try {
                    JSONObject obj = new JSONObject(str);

                    // get the object's details
                    String reviewID = obj.getString("review_id");
                    String userID = obj.getString("user_id");
                    String businessID = obj.getString("business_id");

                    JSONObject votesObj = obj.getJSONObject("votes");
                    int funnyVote = votesObj.getInt("funny");
                    int usefulVote = votesObj.getInt("useful");
                    int coolVote = votesObj.getInt("cool");

                    int stars = obj.getInt("stars");
                    Date postedDate = sdf.parse(obj.getString("date"));
                    String content = obj.getString("text");
                    content = URLEncoder.encode(content, "UTF-8");
                    String type = obj.getString("type");

                    // create the MySQL INSERT query from the object
                    String insertSQL = "INSERT INTO reviews VALUES(" +
                            "'" +  reviewID + "'," +
                            "'" + userID + "'," +
                            "'" + businessID + "'," +
                            "" + funnyVote + "," +
                            "" + usefulVote + "," +
                            "" + coolVote + "," +
                            "" + stars + "," +
                            "'" + new Timestamp(postedDate.getTime()) + "'," +
                            "'" + content + "'," +
                            "'" + type + "');";


                    // input it to the db
                    Statement statement = connection.createStatement();
                    statement.executeUpdate(insertSQL);
                    statement.close();

                    count++;
                    System.out.println(count);

                } catch(Exception e) {
                    e.printStackTrace();
                    System.err.println(str);
                }



            }
            in.close();
            connection.close();


        } catch(Exception e) {
            e.printStackTrace();
        }

    }
}
