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

    private SiteLevelStrategy.Factory siteFact;
    private PageLevelStrategy.Factory pageFact;
    private SiteLevelStrategy siteStrategy;

    private boolean isOPIC;
    private boolean isBacklinkCount;
    private boolean isOPTIMAL;

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
        this.siteFact = siteFact;
        this.pageFact = pageFact;

        this.siteStrategy = this.siteFact.get();
        this.siteStrategy.init(new LinkedList<Site>());

        // Different logic for different scoring models according to the page
        // level strategy.
        PageLevelStrategy pls = this.pageFact.get();
        this.isOPIC = pls instanceof OPICPageLevelStrategy;
        this.isBacklinkCount = pls instanceof BackLinkCountPageLevelStrategy;
        this.isOPTIMAL = pls instanceof OPTIMALPageLevelStrategy;
    }

    @Override
    public List<WebPage> getNextPages(int count) {
        List<WebPage> l = new ArrayList<>();

        for (int i = 0; i < count && !siteStrategy.getSites().isEmpty(); i++) {
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

        if (isOPIC) {
            if (sourcePage != null) {
                // distribute score to its children
                float scoreToAssign = sourcePage.getScore() / pages.size();
                for (WebPage page : pages) {
                    page.setScore(page.getScore() + scoreToAssign);
                } // for
            } // if
        } // if

        if (isOPTIMAL) {
            // like code above in isOPIC
            if (sourcePage != null) {
                // distribute score to its children
                float scoreToAssign = sourcePage.getScore() / pages.size();
                for (WebPage page : pages) {
                    String domainUrl = page.getDomain();
                    Site site = siteStrategy.find(domainUrl);
                    // get site for page / check if exists
                    if (site == null) {
                        // add new site wrapper for queue with page
                        site = new SiteWrapper(domainUrl, pageFact.get());
                        siteStrategy.add(site);
                        ((OPTIMALPageLevelStrategy) site.getStrategy()).incOPICScore(page.getURL(), scoreToAssign);
                    } // if
                } // for
            } // if
        } // if

        for (WebPage wp : pages) {
            // get site url
            String domainUrl = wp.getDomain();
            // Search for site or create new site
            // and add page to site
            Site site = siteStrategy.find(domainUrl);

            Page page = null;
            // decide what the score represents
            if (isBacklinkCount) {
                // Initially only a single inlink
                page = new PageWrapper(wp, 1);
            } else if (isOPTIMAL) {
                // Initially only a single inlink
                ((OPTIMALPageLevelStrategy) site.getStrategy()).setBacklinkcountScore(wp.getURL(), 1);
                page = new PageWrapper(wp);
            } else if (!isOPIC) {
                page = new PageWrapper(wp, score);
            } else {
                // isOPIC
                page = new PageWrapper(wp);
            } // if-else

            if (site != null) {
                // check if already in queue?
                boolean alreadyThere = false;
                for (Page pageInner : site.getPages()) {
                    if (pageInner.getWebPage().getID() == wp.getID()) {
                        // Refresh backlink count for existing pages
                        if (isBacklinkCount) {
                            // add one more inlink if page already existing
                            pageInner.addScore(1);
                        } else if (isOPTIMAL) {
                            // add one more inlink if page already existing
                            ((OPTIMALPageLevelStrategy) site.getStrategy()).incBacklinkcountScore(pageInner.getWebPage().getURL(), 1);
                        } // else-if

                        alreadyThere = true;
                        break;
                    } // if
                } // for

                if (!alreadyThere) {
                    site.getPages().offer(page);
                } // if
            } else {
                // add new site wrapper for queue with page
                site = new SiteWrapper(domainUrl, pageFact.get());
                siteStrategy.add(site);
                site.getStrategy().add(page);
            } // if-else
        } // for
    }

    @Override
    public long getNumberOfElements() {
        long n = 0;

        for (Site site : siteStrategy.getSites()) {
            n += site.getPages().size();
        } // for

        return n;
    }

    @Override
    public void updateOrder() {
        // reorder pages in sites
        for (Site site : siteStrategy.getSites()) {
            site.getStrategy().reOrder();
        } // for

        // reorder sites
        this.siteStrategy.reOrder();
    }

}
