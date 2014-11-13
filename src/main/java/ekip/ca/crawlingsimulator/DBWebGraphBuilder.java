package ekip.ca.crawlingsimulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBWebGraphBuilder implements WebGraph, WebGraphBuilder {
    private final static Logger log = LoggerFactory.getLogger(DBWebGraphBuilder.class);

    protected Connection conn = null;
    protected boolean hasData = false;
    protected PreparedStatement pstmt_insert_url;

    public DBWebGraphBuilder() {

    }

    public void connectToDB(File databaseFile) throws Exception {
        Class.forName("org.h2.Driver");

        conn = DriverManager.getConnection("jdbc:h2:file:" + databaseFile.getAbsolutePath(), "", "");

        // TODO: has data
        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet tables = dbmd.getTables(null, null, "PAGES", new String[] { "TABLE" });
        if (tables.next()) {
            log.debug("database exists");
            hasData = true;
        } else {
            log.debug("database does not exists");
            Statement stmt = conn.createStatement();

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pages (id BIGINT AUTO_INCREMENT PRIMARY KEY, quality BOOLEAN DEFAULT FALSE, url VARCHAR(100), UNIQUE KEY url_UNIQUE (url))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS relations (id1 BIGINT, id2 BIGINT, FOREIGN KEY (id1) REFERENCES pages(id), FOREIGN KEY (id2) REFERENCES pages(id))");

            pstmt_insert_url = conn.prepareStatement("INSERT INTO pages (url) VALUES (?)");

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

        try {
            BufferedReader br = new BufferedReader(new FileReader(graph_file));

            long lineNr = 0;
            String line = null;
            while ((line = br.readLine()) != null) {
                lineNr++;
                int i = line.indexOf('\t');
                if (i == -1) {
                    log.warn("Line {} contains no tab: {}", lineNr, line);
                    continue;
                } // if

                String url1 = line.substring(0, i);
                String url2 = line.substring(i + 1);

                try {
                    pstmt_insert_url.setString(1, url1);
                    pstmt_insert_url.executeUpdate();
                } catch (Exception e) {
                    log.error("insert url", e);
                } // try-catch

                try {
                    pstmt_insert_url.setString(1, url1);
                    pstmt_insert_url.executeUpdate();
                } catch (Exception e) {
                    log.error("insert url", e);
                } // try-catch

                // TODO: add relation

            } // while

            br.close();
        } catch (Exception e) {
            log.error("read graph file fail", e);
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

        // TODO: ...
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

        // TODO: ...
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
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ekip.ca.crawlingsimulator.WebGraph#getLinkedWebPages(ekip.ca.
     * crawlingsimulator.WebPage)
     */
    @Override
    public List<WebPage> getLinkedWebPages(WebPage page) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ekip.ca.crawlingsimulator.WebGraph#setAutoVisitedOnRetrieval(boolean)
     */
    @Override
    public void setAutoVisitedOnRetrieval(boolean autoVisited) {
        // TODO Auto-generated method stub

    }

    @Override
    public WebPage fromID(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    // protected WebPage createNewWebPage();
}
