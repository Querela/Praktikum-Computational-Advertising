/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ekip.ca.crawlingsimulator.WebPage;

/**
 * Represents a general crawling queue for two-level queuing. The fist level are
 * the sites (domains) and the second are the contained pages within. It can be
 * used with different crawling strategies.
 */
public class GeneralCrawlingQueue implements CrawlingQueue {

    /**
     * Simple site wrapper implementation according to its interface.
     */
    public static class SiteWrapper implements Site {
        private String url;
        private LinkedList<Page> pages;
        private PageLevelStrategy pageStrategy;

        /**
         * Constructor.
         * 
         * @param url
         *            Domain-URL of the site
         * @param strategy
         *            {@link PageLevelStrategy} for ordering the pages within a
         *            site.
         */
        public SiteWrapper(String url, PageLevelStrategy strategy) {
            this.url = url;
            this.pages = new LinkedList<>();
            this.pageStrategy = strategy;

            this.pageStrategy.init(this.pages);
        }

        @Override
        public LinkedList<Page> getPages() {
            return pages;
        }

        @Override
        public PageLevelStrategy getStrategy() {
            return pageStrategy;
        }

        @Override
        public String getUrl() {
            return url;
        }
    }

    /**
     * Simple page wrapper for web pages according to the interface.
     */
    public static class PageWrapper implements Page {
        private WebPage wp;

        public PageWrapper(WebPage wp) {
            this.wp = wp;
        }

        public PageWrapper(WebPage wp, float score) {
            this.wp = wp;
            this.setScore(score);
        }

        @Override
        public WebPage getWebPage() {
            return wp;
        }

        @Override
        public float getQuality() {
            return wp.getQuality();
        }

        @Override
        public float getScore() {
            return wp.getScore();
        }

        @Override
        public void setScore(float score) {
            wp.setScore(score);
        }

        @Override
        public void addScore(float addScore) {
            wp.setScore(wp.getScore() + addScore);
        }
    }

    private LinkedList<Site> sites;
    private SiteLevelStrategy.Factory siteFact;
    private PageLevelStrategy.Factory pageFact;
    private SiteLevelStrategy siteStrategy;

    /**
     * Constructor.
     * 
     * @param siteFact
     *            {@link SiteLevelStrategy} the strategy for ordering sites
     * @param pageFact
     *            {@link PageLevelStrategy} the strategy for ordering pages
     *            within their sites
     */
    public GeneralCrawlingQueue(SiteLevelStrategy.Factory siteFact, PageLevelStrategy.Factory pageFact) {
        this.sites = new LinkedList<>();
        this.siteFact = siteFact;
        this.pageFact = pageFact;

        this.siteStrategy = this.siteFact.get();
        this.siteStrategy.init(this.sites);
    }

    @Override
    public List<WebPage> getNextPages(int count) {
        List<WebPage> l = new ArrayList<>();

        for (int i = 0; i < count && !sites.isEmpty(); i++) {
            WebPage page = getNextPage();
            if (page != null) {
                l.add(page);
            } // if
        } // for

        return l;
    }

    @Override
    public WebPage getNextPage() {
        // get next site
        Site site = siteStrategy.getNext();
        if (site != null) {
            // get next page from site
            Page page = site.getStrategy().getNext();
            if (page != null) {
                // unwrap
                return page.getWebPage();
            } // if
        } // if
        return null;
    }

    @Override
    public void addPages(WebPage sourcePage, List<WebPage> pages, int score) {
        if (pages == null) {
            return;
        } // if

        // Different logic for different scoring models according to the page
        // level strategy.
        boolean isOPIC = pageFact instanceof OPICPageLevelStrategy;
        boolean isBacklinkCount = pageFact instanceof BackLinkCountPageLevelStrategy;

        if (isOPIC) {
            // distribute score to its child
            float scoreToAssign = sourcePage.getScore() / pages.size();
            for (WebPage page : pages) {
                page.setScore(page.getScore() + scoreToAssign);
            } // for
        } // if

        for (WebPage wp : pages) {
            String url = wp.getURL();

            Page page = null;
            // decide what the score represents
            if (isBacklinkCount) {
                page = new PageWrapper(wp, wp.getInLinkCount());
            } else if (!isOPIC) {
                page = new PageWrapper(wp, score);
            } else {
                page = new PageWrapper(wp);
            } // if-else

            // get site url
            String domainUrl = url.substring(0, url.lastIndexOf('/')); // wp.getDomain();

            // Search for site or create new site
            // and add page to site
            boolean found = false;
            for (Site site : sites) {
                if (site.getUrl().equals(domainUrl)) {
                    // check if already in queue?
                    boolean alreadyThere = false;
                    for (Page pageInner : site.getPages()) {
                        if (pageInner.getWebPage().getID() == wp.getID()) {
                            // Refresh backlink count for existing pages
                            if (isBacklinkCount) {
                                pageInner.setScore(pageInner.getWebPage().getInLinkCount());
                            } // if

                            alreadyThere = true;
                            break;
                        } // if
                    } // for

                    if (!alreadyThere) {
                        site.getPages().offer(page);
                    } // if
                    found = true;
                    break;
                } // if
            } // for
            if (!found) {
                // add new site wrapper for queue with page
                Site s = new SiteWrapper(domainUrl, pageFact.get());
                sites.offer(s);
                s.getPages().offer(page);
            } // if
        } // for
    }

    @Override
    public long getNumberOfElements() {
        long n = 0;

        for (Site site : sites) {
            n += site.getPages().size();
        } // for

        return n;
    }

    @Override
    public void updateOrder() {
        // reorder pages in sites
        for (Site site : sites) {
            site.getStrategy().reOrder();
        } // for

        // reorder sites
        this.siteStrategy.reOrder();
    }

}
