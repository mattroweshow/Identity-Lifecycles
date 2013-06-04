package uk.ac.lancs.socialcomp.identity;

import uk.ac.lancs.socialcomp.identity.lexical.TermDistribution;
import uk.ac.lancs.socialcomp.identity.social.IndegreeDistribution;
import uk.ac.lancs.socialcomp.identity.social.OutdegreeDistribution;

public class DynamicsRunner {

    public static void main(String[] args) {
        String[] dbNames = {"sap","serverfault"};


        for (String dbName : dbNames) {
            // Indegree
            System.out.println("In-degree: " + dbName);
            IndegreeDistribution indegreeDistribution = new IndegreeDistribution(dbName);
            indegreeDistribution.deriveEntropyPerStageDistributions();
            indegreeDistribution.deriveCrossEntropyPerStageDistributions();
            indegreeDistribution.deriveCommunityDependentStageDistributions();

            // Outdegree
            System.out.println("Out-Degree: " + dbName);
            OutdegreeDistribution outdegreeDistribution = new OutdegreeDistribution(dbName);
            outdegreeDistribution.deriveEntropyPerStageDistributions();
            outdegreeDistribution.deriveCrossEntropyPerStageDistributions();
            outdegreeDistribution.deriveCommunityDependentStageDistributions();

            // Terms
            System.out.println("Lexical: " + dbName);
            TermDistribution termDistribution = new TermDistribution(dbName);
            termDistribution.deriveEntropyPerStageDistributions();
            termDistribution.deriveCrossEntropyPerStageDistributions();
            termDistribution.deriveCommunityDependentStageDistributions();
        }

    }
}
