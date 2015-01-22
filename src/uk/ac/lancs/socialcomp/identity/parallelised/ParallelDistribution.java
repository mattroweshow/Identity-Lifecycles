package uk.ac.lancs.socialcomp.identity.parallelised;

import uk.ac.lancs.socialcomp.identity.statistics.Lifetime;

/**
 * Author: Matthew Rowe
 * Email: m.rowe@lancaster.ac.uk
 * Date / Time : 31/07/2014 / 13:43
 */
public interface ParallelDistribution {

    public JobResult derivePerStageEntropies(String userid, Lifetime lifetime);

    public JobResult deriveHistoricalEntropies(String userid, Lifetime lifetime);

    public JobResult deriveCommunityEntropies(String userid, Lifetime lifetime);
}
