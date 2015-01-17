/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.Collections;
import java.util.Comparator;

/**
 * A strategy that distributes cash from the source page to its child pages.
 * Only the ordering ...
 * 
 * @author Erik Körner
 * @author Immanuel Plath
 */
public class OPICPageLevelStrategy extends PageLevelStrategy {
    private static Comparator<Page> pageComparator = new Comparator<Page>() {
        @Override
        public int compare(Page page1, Page page2) {
            return Float.compare(page1.getScore(), page2.getScore());
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
