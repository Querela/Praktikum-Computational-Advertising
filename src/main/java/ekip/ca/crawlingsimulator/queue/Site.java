/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

/**
 * Represents a wrapper around a collection of web pages. This should in most
 * cases be the domain name.
 * <p/>
 * Also contains the page level strategy for reOrdering pages.
 */
public interface Site {
    /**
     * Returns the list/queue of pages. Shouldn't be modified, only be queried
     * (e. g. for size). Can be modified if needed.
     * 
     * @return Queue / List of pages
     */
    public LinkedList<Page> getPages();

    /**
     * Returns the page level strategy for ordering the web pages according to a
     * score.
     * 
     * @return {@link PageLevelStrategy}
     */
    public PageLevelStrategy getStrategy();

    /**
     * Returns the url of this wrapper. All contains pages are part of this
     * domain.
     * 
     * @return String (URL)
     */
    public String getUrl();
}
