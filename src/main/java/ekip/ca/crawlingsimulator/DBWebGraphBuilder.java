package ekip.ca.crawlingsimulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBWebGraphBuilder implements WebGraph, WebGraphBuilder {
    private final static Logger log = LoggerFactory.getLogger(DBWebGraphBuilder.class);

    protected final static long refresh_line_count = 5000L;
    protected final static float refresh_beta = 0.03f;
    protected final static long gc_line_count = 100000L;

    protected boolean showProgress = false;
    protected Connection conn = null;
    protected boolean hasData = false;
    protected PreparedStatement pstmt_insert_url;
    protected PreparedStatement pstmt_insert_link;
    protected PreparedStatement pstmt_update_quality;
    protected PreparedStatement pstmt_select_from_id;
    protected PreparedStatement pstmt_select_linked;
    protected PreparedStatement pstmt_select_all_from_id;
    protected PreparedStatement pstmt_select_all_from_url;

    public DBWebGraphBuilder(boolean show_progress) {
        this.showProgress = show_progress;
    }

    public void connectToDB(File databaseFile) throws Exception {
        Class.forName("org.h2.Driver");

        conn = DriverManager.getConnection("jdbc:h2:file:" + databaseFile.getAbsolutePath()
                + ";LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0;AUTOCOMMIT=ON;CACHE_TYPE=SOFT_LRU"
        /* + ";AUTOCOMMIT=ON;CACHE_SIZE=8192" */, "", "");

        // TODO: has data
        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet tables = dbmd.getTables(null, null, "PAGES", new String[] { "TABLE" });
        if (tables.next()) {
            log.debug("database exists");
            hasData = true;
        } else {
            log.debug("database does not exists");
            Statement stmt = conn.createStatement();

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pages (id BIGINT AUTO_INCREMENT PRIMARY KEY, quality BOOLEAN DEFAULT FALSE, url VARCHAR(40) NOT NULL)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS relations_url (url1 VARCHAR(40), url2 VARCHAR(40))");
            stmt.executeUpdate("CREATE INDEX INDEX_pages_url ON pages(url)");

            pstmt_insert_url = conn
                    .prepareStatement("INSERT INTO pages (url) SELECT ? FROM DUAL WHERE NOT EXISTS (SELECT * FROM pages WHERE url = ?)");
            pstmt_insert_link = conn.prepareStatement("INSERT INTO relations_url (url1, url2) VALUES (?, ?)");
            pstmt_update_quality = conn.prepareStatement("UPDATE pages SET quality = TRUE WHERE url = ?");
            pstmt_select_from_id = conn.prepareStatement("SELECT quality FROM pages WHERE id = ? LIMIT 1");
            pstmt_select_all_from_id = conn.prepareStatement("SELECT id, quality, url FROM pages WHERE id = ? LIMIT 1");
            pstmt_select_all_from_url = conn
                    .prepareStatement("SELECT id, quality, url FROM pages WHERE url = ? LIMIT 1");
            pstmt_select_linked = conn
                    .prepareStatement("SELECT p.id FROM relations_url AS r, pages AS p WHERE r.url1 = ? AND r.url2 = p.url");

            hasData = false;

            stmt.close();
        } // if-else

        // String[] types = { "TABLE", "SYSTEM TABLE" };
        //
        // ResultSet metaRS = dbmd.getTables(null, null, "%", types);
        //
        // while (metaRS.next()) {
        //
        // // Catalog
        // String tableCatalog = metaRS.getString(1);
        // System.out.println("Catalog: " + tableCatalog);
        //
        // // Schemata
        // String tableSchema = metaRS.getString(2);
        // System.out.println("Tabellen-Schema: " + tableSchema);
        //
        // // Tabellennamen
        // String tableName = metaRS.getString(3);
        // System.out.println("Tabellen-Name: " + tableName);
        //
        // // Tabellentyp
        // String tableType = metaRS.getString(4);
        // System.out.println("Tabellen-Typ: " + tableType + "\n");
        // }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.debug("Run ShutdownHook: Close DB connection ...");
                try {
                    conn.close();
                } catch (Exception e) {
                    log.error("Closing db", e);
                } // try-catch
            }
        });
    }

    protected final static NumberFormat nf = new DecimalFormat("0.###");

    /**
     * Converts a long to human file size.
     * 
     * @param size
     *            long to convert
     * @return String
     */
    protected static String longToSize(long size) {
        if (size < 1024L) {
            return nf.format(size) + " Byte";
        } else if (size < 1048576L) {
            return nf.format(size / 1024f) + " KB";
        } else if (size < 1073741824L) {
            return nf.format(size / 1048576f) + " MB";
        } else if (size < 1099511627776L) {
            return nf.format(size / 1073741824f) + " GB";
        } else {
            return size + " ?";
        } // if-else*if-else
    }

    /**
     * Converts milli seconds from long to time string.
     * 
     * @param millis
     *            long to convert
     * @return String
     */
    protected static String longToTime(long millis) {
        if (millis < 60000L) {
            return String.format("%.2f sec", millis / 1000f);
        } else if (millis < 3600000L) {
            return String.format("%.2f min", millis / 60000f);
        } else if (millis < 86400000L) {
            return String.format("%.2f hrs", millis / 3600000f);
        } else if (millis < 30758400000L) {
            return String.format("%.2f days", millis / 86400000f);
        } else {
            return millis + " ?";
        }
    }

    protected static String formatProgressMessage(long curLine, long curPos, long length, long start) {
        // > time spend (milli seconds)
        long diff = System.currentTimeMillis() - start;
        // > spent time (seconds)
        // ---- diff / 1000
        // > speed (line numbers per second):
        // ---- myLineNr / (diff / 1000)
        // > (for 1000 lines):
        // ---- myLineNr / (diff / 1000) / 1000
        // ---- myLineNr / diff
        float speed = curLine * 1f / diff;
        // > line numbers per byte:
        // ---- myLineNr / (float) pos
        // > line numbers for whole file:
        // ---- myLineNr / pos * file_size
        // > time for all rest lines (in seconds)
        // ---- (myLineNr / pos * file_size) / (speed * 1000)
        // > (to millis for convert function)
        // ---- (myLineNr / pos * file_size) / (speed * 1000) * 1000
        // ---- (myLineNr / pos * file_size) / speed
        long timeLeft = (long) (curLine / (float) curPos * (length - curPos) / speed);
        return String.format(
                "Progress: (%.2f %%)\n    In line %s (speed: %.2f kL/s)\n    %s / %s\n    >>> %s (left: %s)", curPos
                        * 100f / length, curLine, speed, longToSize(curPos), longToSize(length), longToTime(diff),
                longToTime(timeLeft));
    }

    protected static class Progress {
        private final long start;
        public long last_start;
        private long line_nr;
        private long last_line_nr = line_nr;
        private float speed = 0.f;
        private float last_speed = speed;
        private long diff_total;
        private long diff_delta;
        private long time_left;
        private final FileChannel fc;
        private long position;
        private final long file_size;
        private final String file_size_s;
        private float percent;

        private boolean has_new_percent;
        private int last_percent;

        private boolean show_progress;
        private BufferedReader br = null;
        private JTextArea progress_label;

        public Progress(final File file, boolean show_progress) {
            this.line_nr = 0;

            this.show_progress = show_progress;
            this.start = System.currentTimeMillis();

            this.file_size = file.length();
            this.file_size_s = longToSize(file_size);

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                log.error("open file", e);
            } // try-catch
            this.fc = fis.getChannel();

            if (show_progress) {
                progress_label = new JTextArea("Progress: ");
                JLabel filename_label = new JLabel(file.getAbsolutePath(), SwingConstants.RIGHT);
                progress_label.setBackground(filename_label.getBackground());
                progress_label.setEditable(false);
                progress_label.setFont(filename_label.getFont());
                Object[] oos = new Object[] { "Reading & Parsing file ...", filename_label, progress_label };

                br = new BufferedReader(new InputStreamReader(new ProgressMonitorInputStream(null, oos, fis)));
            } else {
                br = new BufferedReader(new InputStreamReader(fis));
            } // if-else

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    log.debug("Run ShutdownHook: Close file {} ...", file.getName());
                    finish();
                }
            });
        }

        public void finish() {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error("Close file");
                } // try-catch
            } // if
        }

        public String nextLine() {
            try {
                String line = br.readLine();
                line_nr++;
                return line;
            } catch (IOException e) {
                log.error("read line", e);
                return null;
            } // try-catch
        }

        public long getLineNr() {
            return line_nr;
        }

        public Progress update() {
            try {
                position = fc.position();
            } catch (IOException e) {
            } // try-catch

            // Check if new percent
            float temp_percent = position * 100.f / file_size;
            if ((((int) temp_percent) - last_percent) > 0) {
                has_new_percent = true;
                last_percent = (int) temp_percent;
            } // if
            percent = temp_percent;

            // Update progress
            if (show_progress && (line_nr % refresh_line_count == 0)) {
                diff_total = System.currentTimeMillis() - start;
                diff_delta = diff_total + start - last_start;

                speed = ((1 - refresh_beta) * last_speed)
                        + (refresh_beta * ((line_nr - last_line_nr) * 1.f / diff_delta));

                time_left = (long) (line_nr / (float) position * (file_size - position) / speed);

                // Values for next iteration
                last_line_nr = line_nr;
                last_start = diff_delta + last_start;
                last_speed = speed;

                // Present values
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            progress_label.setText(getFormatted());
                        } catch (Exception e) {
                            progress_label.setText(e.getLocalizedMessage());
                        } // try-catch
                    }
                };

                SwingUtilities.invokeLater(r);
            } // if

            return this;
        }

        public String getFormatted() {
            return String.format(
                    "Progress: (%.2f %%)\n    In line %d (speed: %.2f kL/s)\n    %s / %s\n    >>> %s (left: %s)",
                    percent, line_nr, speed, longToSize(position), file_size_s, longToTime(diff_total),
                    longToTime(time_left));
        }

        public boolean hasNewPercent() {
            if (has_new_percent) {
                has_new_percent = false;
                return true;
            } // if

            return false;
        }

        public int getNewPercent() {
            return last_percent;
        }

        public long getTotalTime() {
            return System.currentTimeMillis() - start;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraphBuilder#loadGraph(java.io.File)
     */
    @Override
    public void loadGraph(File graph_file) {
        if (hasData) {
            return;
        } // if

        final Progress pgrs = new Progress(graph_file, showProgress);

        log.debug("Begin reading graph file ...");
        String line = null;
        while ((line = pgrs.nextLine()) != null) {
            pgrs.update();
            if (pgrs.hasNewPercent()) {
                log.debug("Reading file {} ... {} % ({} s)", graph_file.getName(), pgrs.getNewPercent(),
                        pgrs.getTotalTime() / 1000.f);
            } // if

            // Start of code

            int i = line.indexOf('\t');
            if (i == -1) {
                log.warn("Line {} contains no tab: {}", pgrs.getLineNr(), line);
                continue;
            } // if

            String url1 = line.substring(0, i);
            String url2 = line.substring(i + 1);

            try {
                pstmt_insert_url.setString(1, url1);
                pstmt_insert_url.setString(2, url1);
                pstmt_insert_url.executeUpdate();
            } catch (Exception e) {
                log.error("insert url", e);
            } // try-catch

            try {
                pstmt_insert_url.setString(1, url2);
                pstmt_insert_url.setString(2, url2);
                pstmt_insert_url.executeUpdate();
            } catch (Exception e) {
                log.error("insert url", e);
            } // try-catch

            try {
                pstmt_insert_link.setString(1, url1);
                pstmt_insert_link.setString(2, url2);
                pstmt_insert_link.executeUpdate();
            } catch (Exception e) {
                log.error(e.getLocalizedMessage());
            } // try-catch
        } // while

        pgrs.finish();
        log.info("Took {} for graph parsing.", longToTime(pgrs.getTotalTime()));

        log.info("Adding unique constraints to db web graph");
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE pages ADD UNIQUE KEY url_UNIQUE (url)");
            stmt.close();
        } catch (Exception e) {
            log.error("alter table add constraints", e);
        } // try-catch

        log.info("Adding index to db web graph links");
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE INDEX INDEX_relations_url ON relations_url(url1, url2)");
            stmt.close();
        } catch (Exception e) {
            log.error("create index", e);
        } // try-catch

        log.info("Adding referential constraints to db web graph");
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE relations_url ADD FOREIGN KEY (url1) REFERENCES pages(url)");
            stmt.executeUpdate("ALTER TABLE relations_url ADD FOREIGN KEY (url2) REFERENCES pages(url)");
            stmt.close();
        } catch (Exception e) {
            log.error("alter table add constraints", e);
        } // try-catch
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ekip.ca.crawlingsimulator.WebGraphBuilder#loadQualities(java.io.File)
     */
    @Override
    public void loadQualities(File quality_file) {
        if (hasData) {
            return;
        } // if

        final Progress pgrs = new Progress(quality_file, showProgress);

        log.debug("Begin reading quality mapping ...");
        String line = null;
        while ((line = pgrs.nextLine()) != null) {

            // Update progress
            pgrs.update();
            if (pgrs.hasNewPercent()) {
                log.debug("Reading file {} ... {} % ({} s)", quality_file.getName(), pgrs.getNewPercent(),
                        pgrs.getTotalTime() / 1000.f);
            } // if

            // Start of code

            int i = line.indexOf(' ');
            if (i == -1) {
                log.warn("Line {} contains no space: {}", pgrs.getLineNr(), line);
                continue;
            } // if

            String url = line.substring(0, i);
            String qual = line.substring(i + 1);

            // Only update qualities where 1
            // Ignore urls which are not in db -> can't be reached from
            // graph
            if ("1".equals(qual)) {
                try {
                    pstmt_update_quality.setString(1, url);
                    pstmt_update_quality.executeUpdate();
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage());
                } // try-catch
            } // if
        } // while

        pgrs.finish();
        log.info("Took {} for quality update.", longToTime(pgrs.getTotalTime()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraphBuilder#loadSeeds(java.io.File)
     */
    @Override
    public void loadSeeds(File seed_file) {
        if (hasData) {
            return;
        } // if

        try {
            final long start = System.currentTimeMillis();
            BufferedReader br = new BufferedReader(new FileReader(seed_file));

            log.debug("Begin reading seed urls ...");
            String line = null;
            while ((line = br.readLine()) != null) {

                log.debug("Seed: {}", line);
                // TODO: create web pages / retrieve ids
            } // while

            log.info("Took {} for reading seeds.", longToTime(System.currentTimeMillis() - start));
            br.close();
        } catch (Exception e) {
            log.error("read seed file fail", e);
        } // try-catch
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraphBuilder#getGraph()
     */
    @Override
    public WebGraph getGraph() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraph#getSeedWebPages()
     */
    @Override
    public List<WebPage> getSeedWebPages() {
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraph#getLinkedWebPages(ekip.ca.
     * crawlingsimulator.WebPage)
     */
    @Override
    public List<WebPage> getLinkedWebPages(WebPage page) {
        if (page == null) {
            return Collections.emptyList();
        } // if

        List<WebPage> pages = new ArrayList<>();

        try {
            // Query all ids from linked pages
            pstmt_select_linked.setString(1, page.getURL());
            ResultSet rs = pstmt_select_linked.executeQuery();
            while (rs.next()) {
                // Retrieve each page for id
                WebPage wp = fromID(rs.getLong(1));
                if (wp != null) {
                    pages.add(wp);
                } // if
            } // while
            rs.close();
        } catch (Exception e) {
            log.error("retrieve linked pages", e);
        } // try-catch

        return pages;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ekip.ca.crawlingsimulator.WebGraph#setAutoVisitedOnRetrieval(boolean)
     */
    @Override
    public void setAutoVisitedOnRetrieval(boolean autoVisited) {
        throw new RuntimeException("setAutoVisitedOnRetrieval not yet implemented!");
    }

    public WebPage fromURL(final String url) {
        // Try to get quality of abort on error
        try {
            pstmt_select_all_from_url.setString(1, url);
            ResultSet rs = pstmt_select_from_id.executeQuery();
            long id = rs.getLong(1);
            rs.close();

            return fromID(id);
        } catch (Exception e) {
            log.error("retieve page", e);
            return null;
        } // try-catch
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraph#fromID(long)
     */
    @Override
    public WebPage fromID(final long id) {
        int qual = 0;

        // Try to get quality of abort on error
        try {
            pstmt_select_from_id.setLong(1, id);
            ResultSet rs = pstmt_select_from_id.executeQuery();

            if (rs.getBoolean(1)) {
                qual = 1;
            } // if
            rs.close();
        } catch (Exception e) {
            log.error("retieve page", e);
            return null;
        } // try-catch

        // create new web page
        final int myQual = qual;
        return new WebPage() {
            private long _id = id;
            private boolean _visited = false;
            private String _url = null;
            private int _quality = myQual;

            @Override
            public void setVisited(boolean visited) {
                _visited = visited;
            }

            @Override
            public boolean hasBeenVisited() {
                return _visited;
            }

            @Override
            public String getURL() {
                // Return url if available
                if (_url != null) {
                    return null;
                } // if

                // Dynamically load url if neccessary
                try {
                    pstmt_select_all_from_id.setLong(1, _id);
                    ResultSet rs = pstmt_select_all_from_id.executeQuery();
                    if (rs.next()) {
                        _url = rs.getString(3);
                    } // if
                    rs.close();
                } catch (Exception e) {
                    log.error("page url retrieval", e);
                    return null;
                } // try-catch

                return _url;
            }

            @Override
            public int getQuality() {
                return _quality;
            }

            @Override
            public List<WebPage> getLinkedPages() {
                return getLinkedWebPages(this);
            }

            @Override
            public long getID() {
                return _id;
            }
        };
    }
}
