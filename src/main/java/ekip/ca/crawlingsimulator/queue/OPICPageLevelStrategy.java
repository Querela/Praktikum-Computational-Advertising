/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * @author Erik Körner
 * @author Immanuel Plath
 */
public class OPICPageLevelStrategy implements PageLevelStrategy {
    private LinkedList<Page> pages;
    private static Comparator<Page> pageComparator = new Comparator<Page>() {
        @Override
        public int compare(Page page1, Page page2) {
            return Float.compare(page1.getScore(), page2.getScore());
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see
     * ekip.ca.crawlingsimulator.queue.PageLevelStrategy#init(java.util.LinkedList
     * )
     */
    @Override
    public void init(LinkedList<Page> pages) {
        this.pages = pages;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.queue.PageLevelStrategy#reOrder()
     */
    @Override
    public void reOrder() {
        Collections.sort(pages, pageComparator);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.queue.PageLevelStrategy#getNext()
     */
    @Override
    public Page getNext() {
        return pages.poll();
    }
}
