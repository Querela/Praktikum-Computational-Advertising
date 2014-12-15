/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import ekip.ca.crawlingsimulator.WebPage;

/**
 * Represents a wrapper around a web page. Only used to stuff additional data
 * into it or allows different logic.
 */
public interface Page {
    /**
     * Returns the wrapped web page.
     * 
     * @return {@link WebPage}
     */
    public WebPage getWebPage();

    /**
     * Returns the quality stored for the web page.
     * 
     * @return float
     */
    public float getQuality();

    /**
     * Set a score/value/priority for the wrapped web page.
     * 
     * @param score
     *            value to set (can represent diffent things)
     */
    public void setScore(float score);

    /**
     * Adds a score/value/priority to the existing value of the web page.
     * 
     * @param addScore
     *            value to add to the existing score
     */
    public void addScore(float addScore);

    /**
     * Returns the stored value/score/priority for the web page. Can represent
     * different things
     * 
     * @return float
     */
    public float getScore();
}
