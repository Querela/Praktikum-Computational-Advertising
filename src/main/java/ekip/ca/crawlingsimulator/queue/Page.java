/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import ekip.ca.crawlingsimulator.WebPage;

/**
 * @author Erik K�rner
 */
public interface Page {
    public WebPage getWebPage();
    public float getQuality();
    public void setScore(float score);
    public void addScore(float addScore);
    public float getScore();
}
