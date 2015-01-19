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
public abstract class PageLevelStrategy {
    protected LinkedList<Page> pages = new LinkedList<>();

    /**
     * Initializes the page level strategy.
     * 
     * @param pages
     *            List/Queue with pages
     */
    public void init(LinkedList<Page> pages) {
        for (Page page : pages) {
            add(page);
        } // for
    }

    /**
     * Reorders the pages according to the implemented strategy.
     */
    public abstract void reOrder();

    /**
     * Adds a page to the queue.
     * 
     * @param page
     *            Page to add
     */
    public void add(Page page) {
        if (page == null) {
            return;
        } // if

        pages.offer(page);
    }

    /**
     * Removes a page from the queue.
     * 
     * @param page
     *            Page to remove
     */
    protected void remove(Page page) {
        if (page == null) {
            return;
        } // if

        pages.remove(page);
    }

    /**
     * Returns the next page from the queue.
     * 
     * @return {@link Page} or null if empty
     */
    public Page getNext() {
        return pages.poll();
    }

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
