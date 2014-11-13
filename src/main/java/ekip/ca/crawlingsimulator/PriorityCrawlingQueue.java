package ekip.ca.crawlingsimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class PriorityCrawlingQueue implements CrawlingQueue {
    protected WebGraph webGraph;

    public static class PriorityPage implements Comparable<PriorityPage> {
        private static long insertAll = 0;
        private long insert;
        private long id;
        private int quality;
        private int priority;

        public PriorityPage(long id, int quality, int priority) {
            this.id = id;
            this.quality = quality;
            this.priority = priority;
            this.insert = insertAll++;
        }

        @Override
        public int compareTo(PriorityPage arg0) {
            int i = 0;

            i = (this.priority - arg0.priority);

            i = (this.quality - arg0.quality);
            // if (i == 0) {
            // i = (this.quality < arg0.quality) ? -1 : 1;
            // } // if

            if (i == 0) {
                i = (int) (this.insert - arg0.insert);
            } // if

            i = (i < 0) ? -1 : (i > 0) ? 1 : 0;

            return i;
        }

        public WebPage getWebPage() {
            return null;
        }

        @Override
        public String toString() {
            return String.format("PriorityPage [id=%s, quality=%s, priority=%s, insert=%s]", id, quality, priority,
                    insert);
        }
    }

    protected PriorityQueue<PriorityPage> queue;

    public PriorityCrawlingQueue(WebGraph wg) {
        webGraph = wg;
        queue = new PriorityQueue<>();
    }

    @Override
    public List<WebPage> getNextPages(int count) {
        List<WebPage> l = new ArrayList<>();

        for (int i = 0; i < count && !queue.isEmpty(); i++) {
            WebPage o = getNextPage();
            if (o != null) {
                l.add(o);
                break;
            } // if
        }

        return l;
    }

    @Override
    public WebPage getNextPage() {
        PriorityPage pp = queue.poll();
        if (pp == null) {
            return null;
        } // if

        return webGraph.fromID(pp.id);
    }

    @Override
    public void addPages(List<WebPage> pages, int priority) {
        int q = 0;

        for (WebPage o : pages) {
            queue.offer(new PriorityPage(o.getID(), 0, priority));
        } // for
    }

}