/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * A site level strategy for ordering sites according to the accumulated score
 * of their pages.
 */
public class MaxPagePrioritySiteLevelStrategy extends SiteLevelStrategy {

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.queue.SiteLevelStrategy#reOrder()
     */
    @Override
    public void reOrder() {
        // Create new cachable comparator for sorting
        Comparator<Site> siteComparator = new Comparator<Site>() {
            private HashMap<Site, Float> scoreMapping = new HashMap<>();

            /**
             * Computes the score of a page.
             * 
             * @param site
             *            Site with pages with scores
             * @return float
             */
            private float computeScore(Site site) {
                float score = 0f;

                // accumulate scores from each page
                for (Page p : site.getPages()) {
                    score += p.getScore();
                } // for

                return score;
            }

            /**
             * Prepares the score of the to pages to compare.
             * 
             * @param site1
             * @param site2
             */
            private void prepareScores(Site site1, Site site2) {
                // compute scores and cache them
                if (!scoreMapping.containsKey(site1)) {
                    scoreMapping.put(site1, computeScore(site1));
                } // if
                if (!scoreMapping.containsKey(site2)) {
                    scoreMapping.put(site2, computeScore(site2));
                } // if
            }

            @Override
            public int compare(Site site1, Site site2) {
                prepareScores(site1, site2);

                // Compare scores
                return scoreMapping.get(site1).compareTo(scoreMapping.get(site2));
            }
        };

        // Sort
        Collections.sort(sites, siteComparator);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.queue.SiteLevelStrategy#getNext()
     */
    @Override
    public Site getNext() {
        // Check sites from the beginning that they have pages
        // If a site is empty remove it
        for (int i = 0; i < sites.size(); i++) {
            // TODO: getFirst() only if removeFirst()
            Site site = sites.getFirst();
            if (site.getPages().size() > 0) {
                return site;
            } else {
                remove(site);
            } // if-else
        } // for

        return null;
    }
}
