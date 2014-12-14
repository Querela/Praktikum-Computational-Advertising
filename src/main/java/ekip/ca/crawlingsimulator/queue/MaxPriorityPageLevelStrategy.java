/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * @author Erik Körner
 */
public class MaxPriorityPageLevelStrategy implements PageLevelStrategy {
    private LinkedList<Page> pages;
    private static Comparator<Page> pageComparator = new Comparator<Page>() {
        @Override
        public int compare(Page page1, Page page2) {
            int comparisonResult = 0;

            comparisonResult = Float.compare(page1.getScore(), page2.getScore());

            if (comparisonResult == 0) {
                comparisonResult = Float.compare(page1.getQuality(), page2.getQuality());
            } // if

            // Normalize equality
            comparisonResult = (comparisonResult < 0) ? -1 : (comparisonResult > 0) ? 1 : 0;

            return comparisonResult;
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
