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
     * Domain-URL of this web page
     * 
     * @return String
     */
    public String getDomain();

    /**
     * Quality of this page (good 1 or bad 0)
     * 
     * @return int
     */
    public int getQuality();

    /**
     * Returns a score for this page.
     * 
     * @return float
     */
    public float getScore();

    /**
     * Sets a score for this page.
     * 
     * @param score
     *            Float score to set
     */
    public void setScore(float score);

    /**
     * Get pages this page links to.
     * 
     * @return List of web pages
     */
    public List<WebPage> getLinkedPages();

    /**
     * Get pages that link to this page.
     * 
     * @return List of web pages
     */
    public List<WebPage> getLinksToThisPages();

    /**
     * Returns the number of in-going links. (links to this page)
     * 
     * @return int
     */
    public int getInLinkCount();

    /**
     * Returns the number of out-going links. (links on page)
     * 
     * @return int
     */
    public int getOutLinkCount();

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
