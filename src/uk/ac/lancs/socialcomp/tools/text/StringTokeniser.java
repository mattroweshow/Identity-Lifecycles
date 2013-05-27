package uk.ac.lancs.socialcomp.tools.text;

import java.util.HashMap;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 21/05/2013 / 16:37
 */
public class StringTokeniser {

    public static String[] tokenise(String input) {
        input = input.toLowerCase();

        // remove HTML tags
        input = removeHTML(input);

        // strip out punctuation
        input = stripPunctuation(input);

        // remove stop words
        input = StopWords.removeStopWords(input);

        // tokenize
        String[] tokens = input.split(" ");

        return tokens;
    }

    private static String removeHTML(String content){
        content = content.replaceAll("<p>","");
        content = content.replaceAll("</p>","");
        return content;
    }


    private static String stripPunctuation(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            Character c = s.charAt(i);
            if (Character.isWhitespace(c) || ((c >= 65 && c <= 90) || (c >= 97 && c <= 122))) {
                sb = sb.append(c);
            }
        }
        return sb.toString();
    }
}
