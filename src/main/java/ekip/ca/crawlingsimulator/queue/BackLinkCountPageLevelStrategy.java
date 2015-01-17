/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * A page level strategy that uses the in-links of pages to provide a score.
 * 
 * @author Erik Körner
 * @author Immanuel Plath
 */
public class BackLinkCountPageLevelStrategy extends PageLevelStrategy {

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.queue.PageLevelStrategy#reOrder()
     */
    @Override
    public void reOrder() {
        Comparator<Page> pageComparator = new Comparator<Page>() {
            private HashMap<Page, Integer> linkCountMapping = new HashMap<>();

            private void prepareLinkCount(Page page1, Page page2) {
                // cache ingoing links
                if (!linkCountMapping.containsKey(page1)) {
                    linkCountMapping.put(page1, (int) page1.getScore());
                } // if
                if (!linkCountMapping.containsKey(page2)) {
                    linkCountMapping.put(page2, (int) page2.getScore());
                } // if
            }

            @Override
            public int compare(Page page1, Page page2) {
                prepareLinkCount(page1, page2);

                return linkCountMapping.get(page1).compareTo(linkCountMapping.get(page2));
            }
        };

        Collections.sort(pages, pageComparator);
    }
}
