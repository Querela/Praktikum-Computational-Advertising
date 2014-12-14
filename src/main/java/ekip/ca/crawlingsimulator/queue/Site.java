/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

/**
 * @author Erik Körner
 */
public interface Site {
    public LinkedList<Page> getPages();
    public PageLevelStrategy getStrategy();
    public String getUrl();
}
