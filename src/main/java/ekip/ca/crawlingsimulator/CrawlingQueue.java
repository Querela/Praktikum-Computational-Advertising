package ekip.ca.crawlingsimulator;

import java.util.List;

/**
 * Interface for an abstract crawling queue with a strategy for assigning
 * priorities.
 */
public interface CrawlingQueue {
    /**
     * Retrieves count web pages from the queue. Can be less if no more in
     * queue.
     * 
     * @param count
     *            number of pages to retrieve
     * @return list of web pages no more than count (can be less or empty)
     */
    public List<WebPage> getNextPages(int count);

    /**
     * Returns the next web page from the queue. Can be null if empty (shouldn't
     * happen)
     * 
     * @return next web page or null
     */
    public WebPage getNextPage();

    /**
     * Add a list of web pages to the queue. Will be given a priority.<br />
     * List of web pages will be added in order of given list. Priority queue
     * should have a stable sort!
     * 
     * @param pages
     *            list of web pages to add to the queue
     * @param priority
     *            priority for each web page
     */
    public void addPages(List<WebPage> pages, int priority);

    /**
     * Returns the number of elements in the queue.
     * 
     * @return long number of elements in queue
     */
    public long getNumberOfElements();
}
