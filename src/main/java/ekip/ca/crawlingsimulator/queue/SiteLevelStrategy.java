/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.HashMap;
import java.util.LinkedList;

import ekip.ca.crawlingsimulator.CrawlingSimulator;

/**
 * The site level strategy is a strategy for ordering a collection of
 * collections of pages. That means it is for ordering domains or similar
 * things. Initially the list/queue with sites has to be given as a reference. (
 * {@link #init(LinkedList)} )
 */
public abstract class SiteLevelStrategy {
    protected LinkedList<Site> sites = new LinkedList<>();
    protected HashMap<String, Site> urlSiteLookup = new HashMap<>();

    /**
     * Needs to be initialized with the refence to the list of sites.
     * 
     * @param sites
     *            List/Queue with sites reference
     */
    public void init(LinkedList<Site> sites) {
        for (Site site : sites) {
            add(site);
        } // for
    }

    /**
     * Reorders the sites according to the implemented logic.
     */
    public abstract void reOrder();

    /**
     * Gets the next site from the list/queue.
     * 
     * @return Site
     */
    public abstract Site getNext();

    /**
     * Returns the sites in this queue.
     * 
     * @return list/queue with sites
     */
    public LinkedList<Site> getSites() {
        return sites;
    }

    /**
     * Finds a site with the given domain url or null if not found.
     * 
     * @param url
     *            Domain-Url of site
     * @return Site or null
     */
    public Site find(String url) {
        if (urlSiteLookup.containsKey(url)) {
            return urlSiteLookup.get(url);
        } else {
            return null;
        } // if-else
    }

    /**
     * Adds a site to the queue.
     * 
     * @param site
     *            Site to add
     */
    public void add(Site site) {
        if (site == null) {
            return;
        } // if

        sites.offer(site);
        urlSiteLookup.put(site.getUrl(), site);
    }

    /**
     * Removes a site from the queue.
     * 
     * @param site
     *            Site to remove
     */
    protected void remove(Site site) {
        if (site == null) {
            return;
        } // if

        sites.remove(site);
        urlSiteLookup.remove(site.getUrl());
    }

    /**
     * A factory for more generalized coding. See {@link CrawlingSimulator}
     */
    public static interface Factory {
        /**
         * Returns the site level strategy.
         * 
         * @return {@link SiteLevelStrategy}
         */
        public SiteLevelStrategy get();
    }
}
