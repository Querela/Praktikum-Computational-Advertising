/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

/**
 * A simple strategy that selects always the next site from the queue/list. If
 * it reaches the end it will start from the beginning.
 */
public class RoundRobinSiteLevelStrategy implements SiteLevelStrategy {
    private LinkedList<Site> sites;
    private int positionInList = -1;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ekip.ca.crawlingsimulator.queue.SiteLevelStrategy#init(java.util.LinkedList
     * )
     */
    @Override
    public void init(LinkedList<Site> sites) {
        this.sites = sites;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.queue.SiteLevelStrategy#reOrder()
     */
    @Override
    public void reOrder() {
        // pass -> no ordering required?
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.queue.SiteLevelStrategy#getNext()
     */
    @Override
    public Site getNext() {
        // goto next position
        positionInList++;
        // if at end then goto null
        if (positionInList >= sites.size()) {
            positionInList = 0;
        } // if

        // abort recursion
        if (sites.size() == 0) {
            return null;
        } // if

        Site site = sites.get(positionInList);
        if (site.getPages().size() == 0) {
            sites.remove(site);

            // do recursive until empty
            site = getNext();
        } // if

        return site;
    }

}
