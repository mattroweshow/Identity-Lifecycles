package uk.ac.lancs.socialcomp.identity;

import uk.ac.lancs.socialcomp.identity.lexical.TermDistribution;
import uk.ac.lancs.socialcomp.identity.social.IndegreeDistribution;
import uk.ac.lancs.socialcomp.identity.social.OutdegreeDistribution;

public class DynamicsRunner {

    public static void main(String[] args) {
        String[] dbNames = {"facebook","sap","serverfault"};
        String split = "test";


        for (String dbName : dbNames) {
            // Indegree
//            System.out.println("In-degree: " + dbName);
//            IndegreeDistribution indegreeDistribution = new IndegreeDistribution(dbName,split);
//            indegreeDistribution.deriveEntropyPerStageDistributions();
//            indegreeDistribution.deriveCrossEntropyPerStageDistributions();
//            indegreeDistribution.deriveCommunityDependentStageDistributions();
//
//            // Outdegree
//            System.out.println("Out-Degree: " + dbName);
//            OutdegreeDistribution outdegreeDistribution = new OutdegreeDistribution(dbName,split);
//            outdegreeDistribution.deriveEntropyPerStageDistributions();
//            outdegreeDistribution.deriveCrossEntropyPerStageDistributions();
//            outdegreeDistribution.deriveCommunityDependentStageDistributions();
//
//            // Terms
//            System.out.println("Lexical: " + dbName);
////            TermDistribution termDistribution = new TermDistribution(dbName,split);
////            termDistribution.deriveEntropyPerStageDistributions();
////            termDistribution.deriveCrossEntropyPerStageDistributions();
////            termDistribution.deriveCommunityDependentStageDistributions();
        }

    }
}
