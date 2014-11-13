/**
 * 
 */
package ekip.ca.crawlingsimulator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Deprecated
public class MemoryWebGraphBuilder implements WebGraph, WebGraphBuilder {

    protected Set<WebPage> webPages;
    protected Map<String, WebPage> urlToWebPage;
    protected Map<WebPage, List<WebPage>> mapWebPageLinks;
    protected List<WebPage> seedURLs;
    protected boolean setAutoVisited;

    public MemoryWebGraphBuilder() {
        webPages = new HashSet<>();
        urlToWebPage = new HashMap<>();
        mapWebPageLinks = new HashMap<>();
        seedURLs = new ArrayList<>();
    }

    protected WebPage getOrCreateWebPage(final String url) {
        if (url == null) {
            return null;
        } // if
        if (urlToWebPage.containsKey(url)) {
            // TODO: or create new object and overwrite hashcode/equals?
            // or urlToWebPage only when loading?
            return urlToWebPage.get(url);
        } // if

        WebPage wp = new WebPage() {
            private String _url = url;
            private boolean _visited = false;
            private int _quality = 0;
            
            @Override
            public long getID() {
                // TODO:
                return 0;
            }

            @Override
            public void setVisited(boolean visited) {
                _visited = visited;
            }

            @Override
            public boolean hasBeenVisited() {
                return _visited;
            }

            @Override
            public String getURL() {
                return _url;
            }

            @Override
            public int getQuality() {
                return _quality;
            }

            @Override
            public List<WebPage> getLinkedPages() {
                return getLinkedWebPages(this);
            }
        };

        webPages.add(wp);
        mapWebPageLinks.put(wp, new ArrayList<WebPage>());

        return wp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraphBuilder#loadGraph(java.io.File)
     */
    @Override
    public void loadGraph(File graph_file) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ekip.ca.crawlingsimulator.WebGraphBuilder#loadQualities(java.io.File)
     */
    @Override
    public void loadQualities(File quality_file) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraphBuilder#loadSeeds(java.io.File)
     */
    @Override
    public void loadSeeds(File seed_file) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraphBuilder#getGraph()
     */
    @Override
    public WebGraph getGraph() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraph#getSeedWebPages()
     */
    @Override
    public List<WebPage> getSeedWebPages() {
        return Collections.unmodifiableList(seedURLs);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraph#getLinkedWebPages(ekip.ca.
     * crawlingsimulator.WebPage)
     */
    @Override
    public List<WebPage> getLinkedWebPages(WebPage page) {
        if (page == null) {
            return Collections.emptyList();
        } // if

        if (setAutoVisited) {
            List<WebPage> webPages = new ArrayList<>(mapWebPageLinks.get(page));

            // TODO: remove visited pages? or only while simulating?
            Iterator<WebPage> iter = webPages.iterator();
            while (iter.hasNext()) {
                WebPage webPage = iter.next();
                if (webPage.hasBeenVisited()) {
                    iter.remove();
                } else {
                    webPage.setVisited(true);
                } // if-else
            } // while

            return webPages;
        } else {
            return Collections.unmodifiableList(mapWebPageLinks.get(page));
        } // if-else
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ekip.ca.crawlingsimulator.WebGraph#setAutoVisitedOnRetrieval(boolean)
     */
    @Override
    public void setAutoVisitedOnRetrieval(boolean autoVisited) {
        setAutoVisited = autoVisited;
    }

    @Override
    public WebPage fromID(long id) {
        // TODO Auto-generated method stub
        return null;
    }

}
