package ekip.ca.crawlingsimulator;

import static ekip.ca.crawlingsimulator.Progress.longToSize;
import static ekip.ca.crawlingsimulator.Progress.longToTime;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBWebGraphBuilder implements WebGraph, WebGraphBuilder {
    final static Logger log = LoggerFactory.getLogger(DBWebGraphBuilder.class);

    protected final static long gc_line_count = 100000L;

    protected boolean showProgress = false;
    protected Connection conn = null;
    protected boolean hasData = false;
    protected PreparedStatement pstmt_insert_url;
    protected PreparedStatement pstmt_insert_good_url;
    protected PreparedStatement pstmt_insert_link;
    protected PreparedStatement pstmt_update_quality;
    protected PreparedStatement pstmt_select_from_id;
    protected PreparedStatement pstmt_select_linked;
    protected PreparedStatement pstmt_select_all_from_id;
    protected PreparedStatement pstmt_select_id_from_url;
    protected PreparedStatement pstmt_select_all_from_url;
    protected PreparedStatement pstmt_select_was_page_visited;
    protected PreparedStatement pstmt_insert_page_visited;

    protected List<WebPage> seedPages;

    public DBWebGraphBuilder(boolean show_progress) {
        this.showProgress = show_progress;
        this.seedPages = new ArrayList<>();
    }

    public void connectToDB(File databaseFile) throws Exception {
        Class.forName("org.h2.Driver");

        conn = DriverManager.getConnection("jdbc:h2:file:" + databaseFile.getAbsolutePath()
                + ";LOG=0;LOCK_MODE=0;UNDO_LOG=0;AUTOCOMMIT=ON;AUTO_RECONNECT=TRUE;CACHE_TYPE=SOFT_LRU", "", "");

        log.debug("(Re-) Create queue tables ...");
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DROP TABLE IF EXISTS pages_visited");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pages_visited (id BIGINT PRIMARY KEY)");
        stmt.close();

        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet tables = dbmd.getTables(null, null, "PAGES", new String[] { "TABLE" });
        if (tables.next()) {
            log.debug("Table for web graph already exists!");
            hasData = true;
        } else {
            log.debug("Tables for web graph do not exist! Will be set up ...");
            hasData = false;

            log.debug("Create page and web graph tables ...");
            stmt = conn.createStatement();
            stmt.executeUpdate("DROP INDEX IF EXISTS INDEX_pages_url");
            stmt.executeUpdate("DROP TABLE IF EXISTS relations");
            stmt.executeUpdate("DROP TABLE IF EXISTS pages");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pages (id BIGINT AUTO_INCREMENT PRIMARY KEY, quality BOOLEAN DEFAULT FALSE, url VARCHAR(40) NOT NULL)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS relations (id1 BIGINT, id2 BIGINT(40))");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS INDEX_pages_url ON pages(url)");
            stmt.close();

            log.debug("Prepare statements ...");
            pstmt_insert_url = conn.prepareStatement("INSERT INTO pages (url) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS);
            pstmt_insert_good_url = conn
                    .prepareStatement("INSERT INTO pages (url, quality) SELECT ?, TRUE FROM DUAL WHERE NOT EXISTS (SELECT * FROM pages WHERE url = ?)");
            pstmt_insert_link = conn.prepareStatement("INSERT INTO relations (id1, id2) VALUES (?, ?)");

            pstmt_update_quality = conn.prepareStatement("UPDATE pages SET quality = TRUE WHERE url = ?");

            pstmt_select_from_id = conn.prepareStatement("SELECT quality FROM pages WHERE id = ?");
            pstmt_select_all_from_id = conn.prepareStatement("SELECT id, quality, url FROM pages WHERE id = ?");
            pstmt_select_id_from_url = conn.prepareStatement("SELECT TOP 1 id FROM pages WHERE url = ?");
            pstmt_select_all_from_url = conn.prepareStatement("SELECT TOP 1 id, quality, url FROM pages WHERE url = ?");
            pstmt_select_linked = conn.prepareStatement("SELECT r.id2 FROM relations AS r WHERE r.id1 = ?");

            pstmt_select_was_page_visited = conn.prepareStatement("SELECT TOP 1 * FROM pages_visited WHERE id = ?");
            pstmt_insert_page_visited = conn.prepareStatement("INSERT INTO pages_visited (id) VALUES (?)");
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
                    pstmt_insert_url.close();
                    pstmt_insert_good_url.close();
                    pstmt_insert_link.close();
                    pstmt_update_quality.close();
                    pstmt_select_from_id.close();
                    pstmt_select_all_from_id.close();
                    pstmt_select_id_from_url.close();
                    pstmt_select_all_from_url.close();
                    pstmt_select_linked.close();
                    pstmt_select_was_page_visited.close();
                    pstmt_insert_page_visited.close();
                } catch (Exception e) {
                    log.error("Closing statements", e);
                } // try-catch

                try {
                    conn.close();
                } catch (Exception e) {
                    log.error("Closing db", e);
                } // try-catch
            }
        });
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

            // TODO: query ids first !!!

            long id1 = addNewPageToDB(url1);
            long id2 = addNewPageToDB(url2);

            try {
                pstmt_insert_link.setLong(1, id1);
                pstmt_insert_link.setLong(2, id2);
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
            log.error("sql update: unique", e);
        } // try-catch

        log.info("Adding indix to db web graph");
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE INDEX INDEX_relations_url ON relations(id1, id2)");
            stmt.close();
        } catch (Exception e) {
            log.error("sql update: index relations", e);
        } // try-catch

        log.info("Adding referential constraints to db web graph");
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE relations ADD FOREIGN KEY (id1) REFERENCES pages(id)");
            stmt.executeUpdate("ALTER TABLE relations ADD FOREIGN KEY (id2) REFERENCES pages(id)");
            stmt.close();
        } catch (Exception e) {
            log.error("sql update: references", e);
        } // try-catch
    }

    /**
     * Adds a new row (page: id, qual, url) to db and/or returns the id of the
     * existing row.
     * 
     * @param url
     *            page to check or add
     * @return id (long) of url (page)
     */
    protected long addNewPageToDB(String url) {
        long id = -1;

        try {
            pstmt_select_id_from_url.setString(1, url);
            ResultSet rs = pstmt_select_id_from_url.executeQuery();
            if (rs.next()) {
                id = rs.getLong(1);
            } // if
            rs.close();
        } catch (SQLException e) {
            log.error("get id for url", e);
        } // try-catch

        if (id == -1) {
            // Insert new row if no valid id
            try {
                pstmt_insert_url.setString(1, url);
                pstmt_insert_url.executeUpdate();
                ResultSet rs = pstmt_insert_url.getGeneratedKeys();
                if (rs.next()) {
                    id = rs.getLong(1);
                } // if
                rs.close();
            } catch (Exception e) {
                log.error("insert url", e);
            } // try-catch
        } // if

        return id;
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

        // Check whether to simply insert or update the rows
        boolean doInsert = false;
        try {
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM pages");
            if (rs.next()) {
                if (rs.getLong(1) > 0) {
                    // has rows -> update
                    log.debug("Do quality pages update because pages exist already.");
                    doInsert = false;
                } else {
                    // empty -> insert
                    log.debug("Do quality pages insert because no pages exist.");
                    doInsert = false;
                } // if-else
            } // if
            rs.close();
        } catch (SQLException e) {
            log.error("sql", e);
        } // try-catch

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

            // Only insert/update qualities where 1
            // Ignore urls which are not in db -> can't be reached from
            // graph
            if ("1".equals(qual)) {
                if (doInsert) {
                    try {
                        pstmt_insert_good_url.setString(1, url);
                        pstmt_insert_good_url.setString(2, url);
                        pstmt_update_quality.executeUpdate();
                    } catch (Exception e) {
                        log.error(e.getLocalizedMessage());
                    } // try-catch
                } else {
                    try {
                        pstmt_update_quality.setString(1, url);
                        pstmt_update_quality.executeUpdate();
                    } catch (Exception e) {
                        log.error(e.getLocalizedMessage());
                    } // try-catch
                } // if-else
            } // if
        } // while

        log.info("Took {} for quality insert.", longToTime(pgrs.getTotalTime()));

        pgrs.finish();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraphBuilder#loadSeeds(java.io.File)
     */
    @Override
    public void loadSeeds(File seed_file) {
        try {
            Progress pgrs = new Progress(seed_file, showProgress);
            log.debug("Begin reading seed urls ...");

            String line = null;
            while ((line = pgrs.nextLine()) != null) {
                WebPage wp = fromURL(line);
                if (wp != null) {
                    seedPages.add(wp);
                } // if
            } // while

            pgrs.finish();
            log.info("Took {} for reading {} seeds.", longToTime(pgrs.getTotalTime()), seedPages.size());
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
        return Collections.unmodifiableList(seedPages);
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
            pstmt_select_linked.setLong(1, page.getID());
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

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraph#fromURL(java.lang.String)
     */
    @Override
    public WebPage fromURL(final String url) {
        if (url == null) {
            return null;
        } // if

        // Try to get quality of abort on error
        try {
            pstmt_select_all_from_url.setString(1, url);
            ResultSet rs = pstmt_select_all_from_url.executeQuery();
            long id = rs.getLong(1);
            int qual = (rs.getBoolean(2)) ? 1 : 0;
            rs.close();

            return newWebPage(id, qual, url);
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

        return newWebPage(id, qual, null);
    }

    /**
     * Wrapper to create a new WebPage object.
     * 
     * @param id
     *            long with db id
     * @param qual
     *            quality (0, 1)
     * @param url
     *            String with url
     * @return WebPage
     */
    protected WebPage newWebPage(final long id, final int qual, final String url) {
        // create new web page
        return new WebPage() {
            private long _id = id;
            private boolean _visited = false;
            private String _url = url;
            private int _quality = qual;

            @Override
            public void setVisited(boolean visited) {
                // Only react to true
                if (visited) {
                    // Only update if we need an update ...
                    if (!_visited) {

                        try {
                            pstmt_insert_page_visited.setLong(1, _id);
                            pstmt_insert_page_visited.executeUpdate();
                        } catch (Exception e) {
                            log.error("update visited", e);
                        } // try-catch

                        _visited = true;
                    } // if
                } // if
            }

            @Override
            public boolean hasBeenVisited() {
                if (_visited) {
                    // if visited then exit -> can't unvisit a page ...
                    return _visited;
                } // if

                try {
                    pstmt_select_was_page_visited.setLong(1, _id);
                    ResultSet rs = pstmt_select_was_page_visited.executeQuery();
                    if (rs.next()) {
                        _visited = true;
                    } // if
                    rs.close();
                } catch (Exception e) {
                    log.error("check visited", e);
                } // try-catch

                return _visited;
            }

            @Override
            public String getURL() {
                // Return url if available
                if (_url != null) {
                    return _url;
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
