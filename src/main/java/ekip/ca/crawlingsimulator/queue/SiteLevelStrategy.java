/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

/**
 * @author Erik Körner
 */
public interface SiteLevelStrategy {
    public void init(LinkedList<Site> sites);
    public void reOrder();
    public Site getNext();
    
    public static interface Factory {
        public SiteLevelStrategy get();
    }
}
