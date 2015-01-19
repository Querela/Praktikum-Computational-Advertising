/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.Collections;
import java.util.Comparator;

/**
 * A simple strategy for comparing pages according to their score and then
 * quality.
 * 
 * @author Erik K�rner
 */
public class MaxPriorityPageLevelStrategy extends PageLevelStrategy {
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
     * @see ekip.ca.crawlingsimulator.queue.PageLevelStrategy#reOrder()
     */
    @Override
    public void reOrder() {
        Collections.sort(pages, pageComparator);
    }
}
