package ekip.ca.crawlingsimulator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class CrawlingQueuePage implements CrawlingQueue {
	
	protected WebGraph webGraph;
	
	public static class Page  {
        private long id;
        private float cash;
        private int inLinks;

        public Page(long id) {
            this.id = id;
        }

        public WebPage getWebPage() {
            return null;
        }
        
        public long getID() {
            return id;
        }
        
        public void setID(long newId) {
            this.id = newId;
        }
        
        public float getCash() {
            return cash;
        }
        
        public float calcCash() {
            return cash;
        }
        
        public void setCash(float newCash) {
            this.cash = newCash;
        }
        
        public void addCash(float newCash) {
            this.cash = cash + newCash;
        }
        
        public int getLinkNumber() {
            return inLinks;
        }
        
        public int calcLinkNumber() {
            return inLinks;
        }
        
        public void setLinkNumber(int newInLinks) {
            this.inLinks = newInLinks;
        }

        @Override
        public String toString() {
            return String.format("PriorityPage [id=%s, quality=%s, priority=%s, insert=%s]", id, cash, inLinks);
        }
    }

    protected Queue<Page> queue;

    public CrawlingQueuePage(WebGraph wg) {
        webGraph = wg;
        queue = new LinkedList<Page>();
    }

    @Override
    public long getNumberOfElements() {
        long size = queue.size();
        return size;
    }

    @Override
    public List<WebPage> getNextPages(int count) {
        List<WebPage> l = new ArrayList<>();

        for (int i = 0; i < count && !queue.isEmpty(); i++) {
            WebPage page = getNextPage();
            if (page != null) {
                l.add(page);
            } // if
        }

        return l;
    }

    @Override
    public WebPage getNextPage() {
        Page pp = queue.poll();
        if (pp == null) {
            return null;
        } // if

        return webGraph.fromID(pp.id);
    }

    @Override
    public void addPages(List<WebPage> pages, int priority) {
        for (WebPage page : pages) {
            queue.offer(new Page(page.getID()));
        } // for
    }
    
    public void performReorderBacklink() {
    	Page[] sortArray = new Page[queue.size()];
    	Queue<Page> tmpQueue = new LinkedList<Page>();
    	tmpQueue = queue;
    	
    	//convert queue to Array to perform a bubble sort
    	for (int i = 0; i < queue.size(); i++) {
    		Page g = tmpQueue.poll();
    		// search on kown link graph for in-going links
    		g.calcLinkNumber();
    		sortArray[i] = g;
        } // for
    	
    	// do bubble sort
    	for (int n = sortArray.length; n > 1; n--){
    	    for (int i = 0 ; i < n-1; i++){
    	    	if (sortArray[i].getLinkNumber() > sortArray[i+1].getLinkNumber()){
    	    		// swap values in Array
    	    		Page tmpPage = sortArray[i];
    	    		sortArray[i] = sortArray[i+1];
    	    		sortArray[i+1] = tmpPage;
    	    	} // ende if
    	    } // ende innere for-Schleife
    	} // ende ‰uﬂere for-Schleife
    	
    	for (Page p : sortArray) {
    		tmpQueue.offer(p);
        } // for
    	
    	// push new sorted queue to onject queue
    	queue = tmpQueue;
    	
    }
    
    public void performReorderOPIC() {
    	Page[] sortArray = new Page[queue.size()];
    	Queue<Page> tmpQueue = new LinkedList<Page>();
    	tmpQueue = queue;
    	
    	//convert queue to Array to perform a bubble sort
    	for (int i = 0; i < queue.size(); i++) {
    		Page g = tmpQueue.poll();
    		sortArray[i] = g;
        } // for
    	
    	// do bubble sort
    	for (int n = sortArray.length; n > 1; n--){
    	    for (int i = 0 ; i < n-1; i++){
    	    	if (sortArray[i].getCash() > sortArray[i+1].getCash()){
    	    		// swap values in Array
    	    		Page tmpPage = sortArray[i];
    	    		sortArray[i] = sortArray[i+1];
    	    		sortArray[i+1] = tmpPage;
    	    	} // ende if
    	    } // ende innere for-Schleife
    	} // ende ‰uﬂere for-Schleife
    	
    	for (Page p : sortArray) {
    		tmpQueue.offer(p);
        } // for
    	
    	// push new sorted queue to onject queue
    	queue = tmpQueue;
    	
    }
    
    public void addCashToPage(long id, float newCash) {
    	Iterator<Page> itr = queue.iterator();
    	while(itr.hasNext()) {
            Page p = itr.next();
            if( p.getID() == id){
            	p.addCash(newCash);
            	break;
            }
    	}	
    }

}
