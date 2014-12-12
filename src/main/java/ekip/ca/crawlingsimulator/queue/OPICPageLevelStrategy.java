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
            // TODO: ...

            int comparisonResult = 0;

            comparisonResult = (page1.getPriority() - page2.getPriority());

            if (comparisonResult == 0) {
                comparisonResult = Float.compare(page1.getQuality(), page2.getQuality());
            } // if

            // Normalize equality
            comparisonResult = (comparisonResult < 0) ? -1 : (comparisonResult > 0) ? 1 : 0;

            return comparisonResult;
        }
    };

    /**
     * 
     */
    public OPICPageLevelStrategy() {
        // TODO Auto-generated constructor stub
    }

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
        // TODO Auto-generated method stub

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
