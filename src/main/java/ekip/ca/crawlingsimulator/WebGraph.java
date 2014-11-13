package ekip.ca.crawlingsimulator;

import java.util.List;

/**
 * Interface for an abstract web graph. Contains pages and links between them.
 */
public interface WebGraph {
    /**
     * Returns a web page with the given id.
     * 
     * @param id
     *            long
     * @return web page or null if not found
     */
    public WebPage fromID(long id);

    /**
     * Returns the seed list of web pages for crawling the whole web graph.
     * 
     * @return list of web pages
     */
    public List<WebPage> getSeedWebPages();

    /**
     * Returns the linked pages from the given page. (all out-going links of a
     * page)
     * 
     * @param page
     *            page which links to other pages
     * @return list of web pages which were linked to
     */
    public List<WebPage> getLinkedWebPages(WebPage page);

    /**
     * Set page automatically as visited if retrieved from web graph?<br />
     * Experimental ...
     * 
     * @param autoVisited
     *            boolean
     */
    public void setAutoVisitedOnRetrieval(boolean autoVisited);
}
