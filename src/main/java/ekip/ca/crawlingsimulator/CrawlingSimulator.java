/**
 * CrawlingSimulator.java
 */
package ekip.ca.crawlingsimulator;

import java.io.File;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * @author Erik Körner
 * @author Immanuel Plath
 */
// @Parameters(separators = "=")
public class CrawlingSimulator {
    private final static Logger log = LoggerFactory.getLogger(CrawlingSimulator.class);

    /**
     * Converts String to File object for JCommander.
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

    public CrawlingSimulator() {

    }

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

    public CrawlingSimulator run() throws Exception {
        // TODO: Load web graph
        WebGraph wg = setupData();

        log.info("Start Crawling Simulator ...");
        // Start simulation

        runSimulation(wg);

        log.info("Stop Crawling Simulator.");
        return this;
    }

    public WebGraph setupData() throws Exception {
        if (database_file.exists() && database_file.isFile() && discard_database) {
            database_file.delete();
        } // if
        
        DBWebGraphBuilder wg = new DBWebGraphBuilder();
        wg.connectToDB(database_file);
        
        wg.loadGraph(graph_file);
        wg.loadQualities(quality_file);
        wg.loadSeeds(seed_file);
        
        return wg;
    }

    public void runSimulation(WebGraph wg) {
        // TODO: simulate
        

//        PriorityCrawlingQueue pcq = new PriorityCrawlingQueue(wg);
//        pcq.addPages(Arrays.asList(new WebPage[] {}), 10);
//        pcq.addPages(Arrays.asList(new WebPage[] {}), 20);
//        for (int i = 0; i < 7; i++) {
//            Object o = pcq.getNextPage();
//            log.info("o=" + o);
//        } // for
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
