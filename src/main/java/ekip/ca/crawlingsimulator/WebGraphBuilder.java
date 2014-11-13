package ekip.ca.crawlingsimulator;

import java.io.File;

public interface WebGraphBuilder {

    public abstract void loadGraph(File graph_file);

    public abstract void loadQualities(File quality_file);

    public abstract void loadSeeds(File seed_file);

    public abstract WebGraph getGraph();

}