/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ekip.ca.crawlingsimulator.WebPage;

/**
 * @author Erik Körner
 */
public class GeneralCrawlingQueue implements CrawlingQueue {

    public static class SiteWrapper implements Site {
        private String url;
        private LinkedList<Page> pages;
        private PageLevelStrategy pageStrategy;

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

    public static class PageWrapper implements Page {
        private WebPage wp;
        private int priority;

        public PageWrapper(WebPage wp, int priority) {
            this.wp = wp;
            this.priority = priority;
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
        public int getPriority() {
            return priority;
        }

        @Override
        public float getScore() {
            return getQuality();
        }
    }

    private LinkedList<Site> sites;
    private SiteLevelStrategy.Factory siteFact;
    private PageLevelStrategy.Factory pageFact;
    private SiteLevelStrategy siteStrategy;

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
        // get next page from site
        // unwrap
        return siteStrategy.getNext().getStrategy().getNext().getWebPage();
    }

    @Override
    public void addPages(List<WebPage> pages, int priority) {
        if (pages == null) {
            return;
        } // if

        for (WebPage wp : pages) {
            String url = wp.getURL();
            // create wrapper for queue
            Page p = new PageWrapper(wp, priority);

            // get site url
            url = url.substring(0, url.indexOf('/'));

            // Search for site or create new site
            // and add page to site
            boolean found = false;
            for (Site site : sites) {
                if (site.getUrl().equals(url)) {
                    site.getPages().offer(p);
                    found = true;
                    break;
                } // if
            } // for
            if (!found) {
                // add new site wrapper for queue with page
                Site s = new SiteWrapper(url, pageFact.get());
                sites.offer(s);
                s.getPages().offer(p);
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
