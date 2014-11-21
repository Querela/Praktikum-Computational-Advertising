package ekip.ca.crawlingsimulator;

import java.util.List;

/**
 * Interface for an abstract web page with url and links to other pages. And a
 * quality.
 */
public interface WebPage {
    /**
     * Id for this web page (auto generated)
     * 
     * @return long
     */
    public long getID();

    /**
     * URL of this web page
     * 
     * @return String
     */
    public String getURL();

    /**
     * Quality of this page (good 1 or bad 0)
     * 
     * @return int
     */
    public int getQuality();

    /**
     * Get pages this page links to.
     * 
     * @return List of web pages
     */
    public List<WebPage> getLinkedPages();

    /**
     * Has this page been visited/processed?
     * 
     * @return boolean
     */
    public boolean hasBeenVisited();

    /**
     * Set whether this page was visited or not.
     * 
     * @param visited
     *            boolean
     */
    public void setVisited(boolean visited);
}
