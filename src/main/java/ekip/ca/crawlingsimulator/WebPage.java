package ekip.ca.crawlingsimulator;

import java.util.List;

public interface WebPage {
    public long getID();
    
    public String getURL();
    
    public float getQuality();
    
    public List<WebPage> getLinkedPages();
    
    public boolean hasBeenVisited();
    
    public void setVisited(boolean visited);
}
