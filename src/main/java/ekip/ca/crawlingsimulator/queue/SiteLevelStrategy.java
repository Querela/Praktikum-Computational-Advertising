/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

/**
 * @author Erik K�rner
 */
public interface SiteLevelStrategy {
    public void init(LinkedList<Site> sites);
    public void reOrder();
    public Site getNext();
    
    public static interface Factory {
        public SiteLevelStrategy get();
    }
}
