package ekip.ca.crawlingsimulator;

import java.util.List;

public interface CrawlingQueue {
    public List<WebPage> getNextPages(int count);
    
    public WebPage getNextPage();
    
    public void addPages(List<WebPage> pages, int priority);
}
