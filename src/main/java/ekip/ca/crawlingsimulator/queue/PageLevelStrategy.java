/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

/**
 * The page level strategy is a strategy for ordering pages within a site. The
 * pages should be ordered according to their score. Initially the list/queue
 * with pages has to be given as a reference. ({@link #init(LinkedList)} )
 */
public interface PageLevelStrategy {
    /**
     * Initializes the page level strategy. Needs the reference to the pages.
     * 
     * @param pages
     *            List/Queue with pages
     */
    public void init(LinkedList<Page> pages);

    /**
     * Reorders the pages according to the implemented strategy.
     */
    public void reOrder();

    /**
     * Returns the next page from the queue.
     * 
     * @return {@link Page} or null if empty
     */
    public Page getNext();

    /**
     * A factory method to allow generalized coding.
     */
    public static interface Factory {
        /**
         * Returns the page level strategy.
         * 
         * @return {@link PageLevelStrategy}
         */
        public PageLevelStrategy get();
    }
}
