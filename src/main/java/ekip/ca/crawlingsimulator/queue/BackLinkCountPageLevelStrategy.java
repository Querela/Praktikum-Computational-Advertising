/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author Erik Körner
 * @author Immanuel Plath
 */
public class BackLinkCountPageLevelStrategy implements PageLevelStrategy {
    private LinkedList<Page> pages;

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
        Comparator<Page> pageComparator = new Comparator<Page>() {
            private HashMap<Page, Integer> linkCountMapping = new HashMap<>();

            private void prepareLinkCount(Page page1, Page page2) {
                // cache ingoing links
                if (!linkCountMapping.containsKey(page1)) {
                    linkCountMapping.put(page1, page1.getWebPage().getInLinkCount());
                } // if
                if (!linkCountMapping.containsKey(page2)) {
                    linkCountMapping.put(page2, page2.getWebPage().getInLinkCount());
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
