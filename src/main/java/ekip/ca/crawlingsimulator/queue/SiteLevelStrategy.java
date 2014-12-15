/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

import ekip.ca.crawlingsimulator.CrawlingSimulator;

/**
 * The site level strategy is a strategy for ordering a collection of
 * collections of pages. That means it is for ordering domains or similar
 * things. Initially the list/queue with sites has to be given as a reference. (
 * {@link #init(LinkedList)} )
 */
public interface SiteLevelStrategy {
    /**
     * Needs to be initialized with the refence to the list of sites.
     * 
     * @param sites
     *            List/Queue with sites reference
     */
    public void init(LinkedList<Site> sites);

    /**
     * Reorders the sites according to the implemented logic.
     */
    public void reOrder();

    /**
     * Gets the next site from the list/queue.
     * 
     * @return Site
     */
    public Site getNext();

    /**
     * A factory for more generalized coding. See {@link CrawlingSimulator}
     */
    public static interface Factory {
        /**
         * Returns the site level strategy.
         * 
         * @return {@link SiteLevelStrategy}
         */
        public SiteLevelStrategy get();
    }
}
