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

/**
 * Crawling simulator for a web graph.
 * 
 * @author Erik Körner
 * @author Immanuel Plath
 */
// @Parameters(separators = "=")
public class CrawlingSimulator {
    private final static Logger log = LoggerFactory.getLogger(CrawlingSimulator.class);

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

    @Parameter(names = { "-n", "--num-crawl-steps" }, required = false, description = "Number of crawling steps.")
    protected Integer number_of_crawling_steps = 5000;

    @Parameter(names = { "-c", "--num-urls-per-step" }, required = false, description = "Number of urls crawled per step.")
    protected Integer urls_per_step = 200;

    @Parameter(names = { "-d", "--database-file" }, converter = FileConverter.class, required = true, description = "File to database.")
    protected File database_file = null;

    @Parameter(names = { "-p", "--discard-database" }, description = "Discard an existing database.")
    protected boolean discard_database = false;

    @Parameter(names = { "-i", "--show-progress" }, description = "Show progress while reading files etc.")
    protected boolean show_progress = false;

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
        // not ready for start !
        // runSimulation(wg);
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
        if (discard_database) {
            if (database_file.exists() && database_file.isFile()) {
                log.info("Discarding existing database file ...");
                database_file.delete();
            } else {
                // Check other file ...
                File database_other_file = new File(database_file.getAbsolutePath() + ".mv.db");
                if (database_other_file.exists() && database_other_file.isFile()) {
                    log.info("Discarding other existing database file ...");
                    database_other_file.delete();
                } // if
            } // if-else
        } // if

        DBWebGraphBuilder wg = new DBWebGraphBuilder(show_progress);
        wg.connectToDB(database_file);

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
        // TODO: simulate

        // Init Ressources
        long startTime = System.currentTimeMillis();
        long lastStepDuration = 10000;
        PriorityCrawlingQueue pcq = new PriorityCrawlingQueue(wg);
        Float[] qualitySteps = new Float[number_of_crawling_steps];
        // Adding Seeds to Queue with Priority
        pcq.addPages(wg.getSeedWebPages(), 20);
        log.info("Seeds in Queue: {}", pcq.getNumberOfElements());
        log.info("Initialize of Ressources done!");

        // do crawling
        for (int i = 0; i < number_of_crawling_steps; i++) {
            // Init local Ressources
            long timeStartLoop = System.currentTimeMillis();
            int documents = 0;
            int goodDocuments = 0;
            float qualityCrawl = 0;
            // write status to console
            log.info("Actual Crawling Step: {}", i + 1);
            log.info("Actual Progress: {}", String.format("%.2f %", i / number_of_crawling_steps));
            log.info("Duration Last Step: {}", longToTime(lastStepDuration));
            log.info("Remaining Time: {}", longToTime((number_of_crawling_steps - i) * lastStepDuration));
            log.info("Elapsed Time: {}", longToTime(System.currentTimeMillis() - startTime));
            log.info("Elements in Queue: {}", pcq.getNumberOfElements());
            // get data from Queue
            List<WebPage> pages = pcq.getNextPages(urls_per_step);
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
                List<WebPage> linkedPagesPassToQueue = new ArrayList<>();
                for (WebPage isInDB : linkedPages) {
                    // make look up if page was already crawled
                    if (isInDB.hasBeenVisited()) {
                        linkedPagesPassToQueue.add(isInDB);
                    }
                }
                pcq.addPages(linkedPagesPassToQueue, 10);
            }

            if (documents == 0 && pcq.getNumberOfElements() == 0) {
                i = number_of_crawling_steps;
                log.info("Queue ist empty! All remaining Steps will be aborted!");
            } else {
                qualityCrawl = goodDocuments / documents;
                if(i < qualitySteps.length) {
                	qualitySteps[i] = qualityCrawl;
                } else {
                	log.info("Array index error!");
                }
            }
            // calc time for each step
            lastStepDuration = System.currentTimeMillis() - timeStartLoop;

        }
        
        // write results to output file qualitySteps
        if(qualitySteps.length > 0) {
        	// write file in output path with quality mapping
            String filepath = "out.txt";
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
                for(int i=0; i < qualitySteps.length; i++) {
                	writer.write( "Quality Step: " + String.valueOf(i) + " --> " + String.valueOf(qualitySteps[i]));
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
        
        // pcq.addPages(Arrays.asList(new WebPage[] {}), 10);
        // for (int i = 0; i < 7; i++) {
        // Object o = pcq.getNextPage();
        // log.info("o=" + o);
        // } // for
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
