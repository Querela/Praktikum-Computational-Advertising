/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

/**
 * @author Erik K�rner
 */
public interface Site {
    public LinkedList<Page> getPages();
    public PageLevelStrategy getStrategy();
    public String getUrl();
}
