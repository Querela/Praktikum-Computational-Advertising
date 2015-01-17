/**
 * CrawlingSimulator.java
 */
package ekip.ca.crawlingsimulator;

import static ekip.ca.crawlingsimulator.Progress.longToTime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import ekip.ca.crawlingsimulator.queue.BackLinkCountPageLevelStrategy;
import ekip.ca.crawlingsimulator.queue.CrawlingQueue;
import ekip.ca.crawlingsimulator.queue.GeneralCrawlingQueue;
import ekip.ca.crawlingsimulator.queue.MaxPagePrioritySiteLevelStrategy;
import ekip.ca.crawlingsimulator.queue.OPICPageLevelStrategy;
import ekip.ca.crawlingsimulator.queue.OPTIMALPageLevelStrategy;
import ekip.ca.crawlingsimulator.queue.PageLevelStrategy;
import ekip.ca.crawlingsimulator.queue.RoundRobinSiteLevelStrategy;
import ekip.ca.crawlingsimulator.queue.SiteLevelStrategy;

/**
 * Crawling simulator for a web graph.
 * 
 * @author Erik Körner
 * @author Immanuel Plath
 */
// @Parameters(separators = "=")
public class CrawlingSimulator {
    private final static Logger log = LoggerFactory.getLogger(CrawlingSimulator.class);

    private final static String NL = "\r\n";

    /**
     * Converts String parameer to File object for JCommander.
     */
    public static class FileConverter implements IStringConverter<File> {
        @Override
        public File convert(String value) {
            File f = new File(value);

            // if (!f.exists() || !f.isFile()) {
            // throw new ParameterException("Invalid File! : value=" + value);
            // } // if

            return f;
        }
    }

    @Parameter(names = { "-h", "--help", "-help" }, help = true, descriptionKey = "help", description = "The help message.", hidden = true)
    private boolean help;

    @Parameter(names = { "-s", "--seed-file" }, converter = FileConverter.class, required = true, description = "File with seed urls.")
    protected File seed_file = null;

    @Parameter(names = { "-g", "--graph-file" }, converter = FileConverter.class, required = true, description = "File with web graph. (url to url mapping)")
    protected File graph_file = null;

    @Parameter(names = { "-q", "--quality-file" }, converter = FileConverter.class, required = true, description = "File with url quality mapping.")
    protected File quality_file = null;

    @Parameter(names = { "-o", "--step-quality-output-file" }, converter = FileConverter.class, required = true, description = "Output file with quality per step.")
    protected File step_quality_output_file = null;

    @Parameter(names = { "-m", "--step-quality-for-single-steps" }, arity = 1, description = "Should the step quality computation be reset after each step or should it be computed over the whole crawling process.")
    protected boolean step_quality_for_single_steps = false;

    @Parameter(names = { "-n", "--num-crawl-steps" }, required = false, description = "Number of crawling steps.")
    protected Integer number_of_crawling_steps = 5000;

    @Parameter(names = { "-c", "--num-urls-per-step" }, required = false, description = "Number of urls crawled per step.")
    protected Integer urls_per_step = 200;

    @Parameter(names = { "-b", "--batch-update-queue" }, required = false, description = "Number of pages to be crawled before the queues are updated.")
    protected Integer batch_size_for_queue_update = 100;

    @Parameter(names = { "-d", "--database-file" }, converter = FileConverter.class, required = true, description = "File to database.")
    protected File database_file = null;

    @Parameter(names = { "-k", "--database-options" }, description = "Database options set in database connection url.")
    protected String database_options = "";

    @Parameter(names = { "-p", "--discard-database" }, description = "Discard an existing database.")
    protected boolean discard_database = false;

    @Parameter(names = { "-i", "--show-progress" }, description = "Show progress while reading files etc.")
    protected boolean show_progress = false;

    @Parameter(names = { "-f", "--output-format" }, arity = 1, description = "If true each line in output-file contains only quality value and no other format string.")
    protected boolean out_put_float_only_format = false;

    @Parameter(names = { "-t", "--crawling-strategy" }, description = "Crawling strategy")
    protected String crawling_strategy = "maxpagepriority-opic";

    @Parameter(names = { "-tval", "--crawling-strategy-values" }, required = false, description = "Crawling strategy parameters")
    protected String crawling_strategy_values = null;

    /**
     * Empty constructor.
     */
    public CrawlingSimulator() {

    }

    /**
     * Initializes the CrawlingSimulator.
     * 
     * @param args
     *            Command line args
     * @return this
     * @throws Exception
     */
    public CrawlingSimulator init(String... args) throws Exception {
        log.debug("Initialize Crawling Simulator ...");

        // Initialize
        JCommander jc = new JCommander(this);
        jc.setProgramName("CrawlingSimulator");
        // Check for errors and display help/usage
        try {
            jc.parse(args);
            if (help == true) {
                jc.usage();
            } // if
        } catch (ParameterException e) {
            log.error("Parameter parsing error.", e);
            jc.usage();
            System.exit(1);
        } // try-catch

        // TODO: other ...

        return this;
    }

    /**
     * Run whole process (reading web graph and simulating crawling).
     * 
     * @return this
     * @throws Exception
     */
    public CrawlingSimulator run() throws Exception {
        // read/load data
        log.info("Set up data (web graph) ...");
        WebGraph wg = setupData();
        log.info("Data ready.");

        // simulate
        log.info("Start Crawling Simulator ...");
        runSimulation(wg);
        log.info("Stop Crawling Simulator.");
        return this;
    }

    /**
     * Creates a new or returns an existing web graph.
     * 
     * @return Web graph
     * @throws Exception
     */
    public WebGraph setupData() throws Exception {
        DBWebGraphBuilder wg = new DBWebGraphBuilder(show_progress, discard_database);
        wg.connectToDB(database_file, database_options);
        wg.setCreateCrawlGraph(false);

        wg.loadQualities(quality_file);
        wg.loadGraph(graph_file);
        wg.loadSeeds(seed_file);

        return wg;
    }

    /**
     * Runs the simulation
     * 
     * @param wg
     *            Web graph to simulate on
     */
    public void runSimulation(WebGraph wg) {
        // Init Ressources
        long startTime = System.currentTimeMillis();
        long lastStepDuration = 10000;

        SiteLevelStrategy.Factory sf = null;
        PageLevelStrategy.Factory pf = null;

        crawling_strategy = crawling_strategy.toLowerCase();

        if (crawling_strategy.contains("roundrobin")) {
            log.info("SiteLevelStrategy: RoundRobin");
            sf = new SiteLevelStrategy.Factory() {
                @Override
                public SiteLevelStrategy get() {
                    return new RoundRobinSiteLevelStrategy();
                }
            };
            // } else if (crawling_strategy.contains("maxpagepriority")) {
        } else {
            log.info("SiteLevelStrategy: MaxPagePriority");
            sf = new SiteLevelStrategy.Factory() {
                @Override
                public SiteLevelStrategy get() {
                    return new MaxPagePrioritySiteLevelStrategy();
                }
            };
        } // if-else

        if (crawling_strategy.contains("backlink")) {
            log.info("PageLevelStrategy: BacklinkCount");
            pf = new PageLevelStrategy.Factory() {
                @Override
                public PageLevelStrategy get() {
                    return new BackLinkCountPageLevelStrategy();
                }
            };
            // } else if (crawling_strategy.contains("opic")) {
        } else if (crawling_strategy.contains("optimal")) {
            log.info("PageLevelStrategy: OPTIMAL");
            log.debug("Parameters: {}", crawling_strategy_values);

            if (crawling_strategy_values == null) {
                log.warn("Switch to default because there are no parameters!");
                crawling_strategy_values = "1.0;1.0;1.0";
            } // if
            String[] crawling_strategy_values_array = crawling_strategy_values.split(";");
            if (crawling_strategy_values_array.length != 3) {
                log.warn("Switch to default because there are less parameters than expected!");
                crawling_strategy_values_array = new String[] { "1.0", "1.0", "1.0" };
            } // if
            float[] crawling_strategy_values_array_float = new float[3];
            for (int i = 0; i < 3; i++) {
                try {
                    crawling_strategy_values_array_float[i] = Float.valueOf(crawling_strategy_values_array[i].replace(
                            ',', '.'));
                } catch (Exception e) {
                    log.error("Parsing parameters failed!", e);
                } // try-catch
            } // for

            final float deltaForOPIC = crawling_strategy_values_array_float[0];
            final float deltaForBacklinkCount = crawling_strategy_values_array_float[1];
            final float deltaForQuality = crawling_strategy_values_array_float[2];

            log.debug("Use delta = {} for OPIC.", deltaForOPIC);
            log.debug("Use delta = {} for BackLinkCount.", deltaForBacklinkCount);
            log.debug("Use delta = {} for Quality (good/total documents).", deltaForQuality);

            pf = new PageLevelStrategy.Factory() {
                @Override
                public PageLevelStrategy get() {
                    return new OPTIMALPageLevelStrategy(deltaForOPIC, deltaForBacklinkCount, deltaForQuality);
                }
            };
        } else {
            log.info("PageLevelStrategy: OPIC");
            pf = new PageLevelStrategy.Factory() {
                @Override
                public PageLevelStrategy get() {
                    return new OPICPageLevelStrategy();
                }
            };
        } // if-else

        CrawlingQueue pcq = new GeneralCrawlingQueue(sf, pf);
        // PriorityCrawlingQueue pcq = new PriorityCrawlingQueue(wg);

        Float[] qualitySteps = new Float[number_of_crawling_steps];
        int documents = 0;
        int goodDocuments = 0;
        // Adding Seeds to Queue with Cash
        pcq.addPages(null, wg.getSeedWebPages(), 10);
        log.info("Seeds in Queue: {}", pcq.getNumberOfElements());
        log.info("Initialize of Ressources done!");

        // do crawling
        for (int i = 0; i < number_of_crawling_steps; i++) {
            // Init local Ressources
            long timeStartLoop = System.currentTimeMillis();

            if (step_quality_for_single_steps) {
                documents = 0;
                // only good documents will be reset for each step?
                goodDocuments = 0;
            } // if

            // write status to console
            log.info("Actual Crawling Step: {}", i + 1);
            log.info("Actual Progress: {} %", String.format("%.2f", ((i / (float) number_of_crawling_steps) * 100)));

            log.debug("Duration Last Step: {}", longToTime(lastStepDuration));
            log.info("Remaining Time: {}", longToTime((number_of_crawling_steps - i) * lastStepDuration));
            log.info("Elapsed Time: {}", longToTime(System.currentTimeMillis() - startTime));
            log.debug("Elements in Queue: {}", pcq.getNumberOfElements());
            // get data from Queue
            List<WebPage> pages = pcq.getNextPages(urls_per_step);
            log.debug("Size from Queue after poll pages: {}", pcq.getNumberOfElements());
            log.debug("Value from Param urls_per_step: {}", urls_per_step);
            log.debug("Number of Pages poll from queue: {}", pages.size());
            for (WebPage page : pages) {
                // 1.Step search each Page in quality database
                // 2.Step calc quality
                int tmp = page.getQuality();
                documents++;
                if (tmp == 1) {
                    goodDocuments++;
                }
                // 3.Step insert page in visited database
                page.setVisited(true);
                // rufe passende Funktion in WebGraph
                // 4.Step insert found links in Queue
                List<WebPage> linkedPages = wg.getLinkedWebPages(page);

                // Filter visited pages
                List<WebPage> linkedPagesPassToQueue = new ArrayList<>();
                for (WebPage isInDB : linkedPages) {
                    // make look up if page was already crawled
                    if (!isInDB.hasBeenVisited()) {
                        linkedPagesPassToQueue.add(isInDB);
                    }
                }

                pcq.addPages(page, linkedPagesPassToQueue, 0);

                // Check if reordering is required
                if ((documents % batch_size_for_queue_update) == 0) {
                    long start = System.currentTimeMillis();
                    pcq.updateOrder();
                    log.debug("Reordering after {} documents took {}", documents,
                            Progress.longToTimeEx(System.currentTimeMillis() - start));
                } // if
            }

            float qualityCrawl = (float) goodDocuments / (float) documents;
            log.info("Calced Quality: {}", qualityCrawl);
            log.debug("goodDocuments: {}", goodDocuments);
            log.debug("documents: {}", documents);
            if (i < qualitySteps.length) {
                qualitySteps[i] = qualityCrawl;
            } else {
                log.warn("Array index error!");
            }

            if (pcq.getNumberOfElements() == 0) {
                i = number_of_crawling_steps;
                log.info("Queue ist empty! All remaining Steps will be aborted!");
            }

            // calc time for each step
            lastStepDuration = System.currentTimeMillis() - timeStartLoop;
        }

        // write results to output file qualitySteps
        if (qualitySteps.length > 0) {
            // write file in output path with quality mapping
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(step_quality_output_file),
                        "utf-8"));
                for (int i = 0; i < qualitySteps.length; i++) {
                    if (out_put_float_only_format) {
                        writer.write(String.valueOf(qualitySteps[i]) + NL);
                    } else {
                        writer.write("Quality Step: " + String.valueOf(i) + " --> " + String.valueOf(qualitySteps[i])
                                + NL);
                    }
                }
            } catch (IOException ex) {
                log.debug("While try to create quality mapping output file a error occur!", ex);
            } finally {
                try {
                    writer.close();
                } catch (Exception ex) {
                }
            }

        } else {
            log.info("No quality steps recorded!");
        }
    }

    /**
     * Entry point. Starting simulator.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new CrawlingSimulator().init(args).run();
    }

}
