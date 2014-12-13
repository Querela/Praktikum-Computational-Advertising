/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.LinkedList;

/**
 * @author Erik Körner
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

        return sites.get(positionInList);
    }

}
