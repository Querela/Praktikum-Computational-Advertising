/**
 * CrawlingSimulator.java
 */
package ekip.ca.crawlingsimulator;

import java.io.File;

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
            return new File(value);
        }
    }

    @Parameter(names = { "--help", "-help", "-h" }, help = true, descriptionKey = "help", description = "This help message.", hidden = true)
    private boolean help;

    @Parameter(names = { "--seed-file", "-s" }, converter = FileConverter.class, required = true, description = "File to seed urls.")
    protected File seed_file = null;

    @Parameter(names = { "--graph-file", "-g" }, converter = FileConverter.class, required = true, description = "File to web graph. (url to url mapping)")
    protected File graph_file = null;

    @Parameter(names = { "--quality-file", "-q" }, converter = FileConverter.class, required = true, description = "File to url quality mapping.")
    protected File quality_file = null;

    @Parameter(names = { "--step-quality-output-file", "-o" }, converter = FileConverter.class, required = true, description = "Output file with quality per step.")
    protected File step_quality_output_file = null;

    @Parameter(names = { "--num-crawl-steps", "-n" }, required = false, description = "Number of crawling steps.")
    protected Integer number_of_crawling_steps = 5000;

    @Parameter(names = { "--num-urls-per-step", "-c" }, required = false, description = "Number of urls crawled per step.")
    protected Integer urls_per_step = 200;

    @Parameter(names = { "--database-file", "-d" }, converter = FileConverter.class, required = true, description = "File to database.")
    protected File database_file = null;

    public CrawlingSimulator() {

    }

    public void init() throws Exception {

    }

    public void run() throws Exception {

    }

    /**
     * Entry point. Starting simulator.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        log.debug("Initialize Crawling Simulator ...");
        // Initialisieren
        CrawlingSimulator cs = new CrawlingSimulator();
        JCommander jc = new JCommander(cs);
        jc.setProgramName("CrawlingSimulator");
        // Check for errors and display help/usage
        try {
            jc.parse(args);
            if (cs.help == true) {
                jc.usage();
            } // if
        } catch (ParameterException e) {
            log.error("Parameter parsing error.", e);
            jc.usage();
            System.exit(1);
        } // try-catch
        cs.init();

        // Start simulation
        log.info("Start Crawling Simulator ...");
        cs.run();
        log.info("Stop Crawling Simulator.");
    }

}
