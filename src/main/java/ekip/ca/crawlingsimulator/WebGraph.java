package ekip.ca.crawlingsimulator;

import java.util.List;

public interface WebGraph {
    public WebPage fromID(long id);
    
    public List<WebPage> getSeedWebPages();
    
    public List<WebPage> getLinkedWebPages(WebPage page);
    
    // ? // in Queue
    public void setAutoVisitedOnRetrieval(boolean autoVisited);
}
