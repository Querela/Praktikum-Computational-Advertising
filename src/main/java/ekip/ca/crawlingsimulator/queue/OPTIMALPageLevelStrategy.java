/**
 * 
 */
package ekip.ca.crawlingsimulator.queue;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * A strategy combination of partial opic, backlinkcount and good/bad sites
 * (site level quality).
 * 
 * @author Erik Körner
 * @author Immanuel Plath
 */
public class OPTIMALPageLevelStrategy extends PageLevelStrategy {
    private HashMap<String, PageValueWrapper> pageValues = new HashMap<>();
    private static HashMap<String, SiteValueWrapper> siteValues = new HashMap<>();

    private float deltaForOPIC = 1f;
    private float deltaForBacklinkCount = 1f;
    private float deltaForQuality = 1f;

    public OPTIMALPageLevelStrategy(float deltaForOPIC, float deltaForBacklinkCount, float deltaForQuality) {
        this.deltaForOPIC = deltaForOPIC;
        this.deltaForBacklinkCount = deltaForBacklinkCount;
        this.deltaForQuality = deltaForQuality;
    }

    /**
     * Wrapper for site level strategy scores (only the number of (good)
     * documents).
     */
    private final static class SiteValueWrapper {
        private int coundDocuments = 0;
        private int coundGoodDocuments = 0;

        public void incrementOneDocument(float quality) {
            coundDocuments += 1;

            if (quality > 0f) {
                coundGoodDocuments += 1;
            }
        }

        @SuppressWarnings("unused")
        public int getCountDocuments() {
            return coundDocuments;
        }

        @SuppressWarnings("unused")
        public int getCountGoodDocuments() {
            return coundGoodDocuments;
        }

        @SuppressWarnings("unused")
        public int getCountBadDocuments() {
            return coundDocuments - coundGoodDocuments;
        }

        public float getQuality() {
            return coundGoodDocuments / Math.max(coundDocuments, 1);
        }
    }

    /**
     * Wrapper for page level strategy scores.
     */
    private final static class PageValueWrapper {
        public float opicScore = 0f;
        public int backlinkcount = 0;
    }

    /**
     * Increments the exisiting score of a page for the partial opic.
     * 
     * @param pageUrl
     *            url of page
     * @param score
     *            score to add to existing score
     */
    public void incOPICScore(String pageUrl, float score) {
        PageValueWrapper pvw = getPageValueWrapper(pageUrl);
        pvw.opicScore += score;
    }

    public void setOPICScore(String pageUrl, float score) {
        getPageValueWrapper(pageUrl).opicScore = score;
    }

    public float getOPICScore(String pageUrl) {
        return getPageValueWrapper(pageUrl).opicScore;
    }

    /**
     * Increments the exisiting score of a page for the backlinkcount.
     * 
     * @param pageUrl
     *            url of page
     * @param score
     *            score to add to existing score
     */
    public void incBacklinkcountScore(String pageUrl, int score) {
        PageValueWrapper pvw = getPageValueWrapper(pageUrl);
        pvw.backlinkcount += score;
    }

    public void setBacklinkcountScore(String pageUrl, int score) {
        getPageValueWrapper(pageUrl).backlinkcount = score;
    }

    public int getBacklinkcountScore(String pageUrl) {
        return getPageValueWrapper(pageUrl).backlinkcount;
    }

    /**
     * Returns statistics object with scores for backlinkcount and partial opic.
     * 
     * @param pageUrl
     *            Page url
     * @return information about page scores
     */
    private PageValueWrapper getPageValueWrapper(String pageUrl) {
        PageValueWrapper pvw = this.pageValues.get(pageUrl);
        if (pvw == null) {
            // Create default
            pvw = new PageValueWrapper();
            this.pageValues.put(pageUrl, pvw);
        } // if
        return pvw;
    }

    /**
     * Returns statistics object with counters for (good) documents.
     * 
     * @param domainUrl
     *            Domain url
     * @return information for site counters
     */
    private static SiteValueWrapper getSiteValueWrapper(String domainUrl) {
        SiteValueWrapper svw = siteValues.get(domainUrl);
        if (svw == null) {
            // Create default
            svw = new SiteValueWrapper();
            siteValues.put(domainUrl, svw);
        } // if
        return svw;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.queue.PageLevelStrategy#add(ekip.ca.
     * crawlingsimulator.queue.Page)
     */
    @Override
    public void add(Page page) {
        super.add(page);

        // add document count/quality count
        if (page != null) {
            SiteValueWrapper svw = getSiteValueWrapper(page.getWebPage().getDomain());
            svw.incrementOneDocument(page.getQuality());
        } // if
    };

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.queue.PageLevelStrategy#reOrder()
     */
    @Override
    public void reOrder() {
        Comparator<Page> pageComparator = new Comparator<Page>() {
            private HashMap<Page, Float> valueMapping = new HashMap<>();

            private Float computeScore(Page page) {
                // get stored scores (partials)
                float siteQuality = getSiteValueWrapper(page.getWebPage().getDomain()).getQuality();
                PageValueWrapper pvw = getPageValueWrapper(page.getWebPage().getURL());

                // TODO: how to compute score?
                page.setScore(deltaForQuality * siteQuality + deltaForBacklinkCount * pvw.backlinkcount + deltaForOPIC
                        * pvw.opicScore);

                return page.getScore();
            }

            private void prepareScores(Page page1, Page page2) {
                // cache score computation
                if (!valueMapping.containsKey(page1)) {
                    valueMapping.put(page1, computeScore(page1));
                } // if
                if (!valueMapping.containsKey(page2)) {
                    valueMapping.put(page2, computeScore(page2));
                } // if
            }

            @Override
            public int compare(Page page1, Page page2) {
                prepareScores(page1, page2);

                return valueMapping.get(page1).compareTo(valueMapping.get(page2));
            }
        };

        Collections.sort(pages, pageComparator);
    }
}
