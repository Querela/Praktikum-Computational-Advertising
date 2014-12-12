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
    public int getPriority();
    public float getScore();
}
