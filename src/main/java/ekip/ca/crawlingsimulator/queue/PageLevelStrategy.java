/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

/**
 * @author Erik Körner
 */
public interface PageLevelStrategy {
    public void init(LinkedList<Page> pages);
    public void reOrder();
    public Page getNext();
    
    public static interface Factory {
        public PageLevelStrategy get();
    }
}
