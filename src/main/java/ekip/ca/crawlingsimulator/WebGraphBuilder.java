package ekip.ca.crawlingsimulator;

import java.io.File;

/**
 * Interface for building (parsing) a web graph.
 */
public interface WebGraphBuilder {

    /**
     * Loads the graph from the given file. Creates pages and the links
     * inbetween.
     * 
     * @param graph_file
     *            file with graph data
     */
    public abstract void loadGraph(File graph_file);

    /**
     * Loads quality values (1 or 0) for urls from a mapping file.
     * 
     * @param quality_file
     *            file with mapping quality to url
     */
    public abstract void loadQualities(File quality_file);

    /**
     * Load the seed urls from which the crawling process will start.
     * 
     * @param seed_file
     *            file with seed urls as crawling start
     */
    public abstract void loadSeeds(File seed_file);

    /**
     * Returns the web graph object for retrieving pages and links.
     * 
     * @return WebGraph
     */
    public abstract WebGraph getGraph();

}