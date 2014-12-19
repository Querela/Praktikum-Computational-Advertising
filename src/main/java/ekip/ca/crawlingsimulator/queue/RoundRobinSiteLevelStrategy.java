/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

/**
 * A simple strategy that selects always the next site from the queue/list. If
 * it reaches the end it will start from the beginning.
 */
public class RoundRobinSiteLevelStrategy extends SiteLevelStrategy {
    private int positionInList = -1;

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
            remove(site);

            // do recursive until empty
            site = getNext();
        } // if

        return site;
    }
}
