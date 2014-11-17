package ekip.ca.crawlingsimulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to monitor progress when reading lines from a large file. Can
 * automatically update a GUI.
 */
public class Progress {
    private final static Logger log = LoggerFactory.getLogger(Progress.class);

    protected boolean show_progress = false;

    protected final static long refresh_line_count = 5000L;
    protected final static float refresh_beta = 0.03f;

    protected final static NumberFormat nf = new DecimalFormat("0.###");

    /**
     * Converts a long to human file size.
     * 
     * @param size
     *            long to convert
     * @return String
     */
    public static String longToSize(long size) {
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
    public static String longToTime(long millis) {
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

    /**
     * Converts milli seconds from long to extended time string.
     * 
     * @param millis
     *            long to convert
     * @return String
     */
    public static String longToTimeEx(long millis) {
        StringBuilder sb = new StringBuilder();

        if (millis > 86400000L) {
            sb.append(millis / 86400000L).append(" days ");
            millis = millis % 86400000L;
        } // if
        if (millis > 3600000L) {
            sb.append(millis / 3600000L).append(" hrs ");
            millis = millis % 3600000L;
        } // if
        if (millis > 60000L) {
            sb.append(millis / 60000L).append(" min ");
            millis = millis % 60000L;
        } // if
        if (millis > 1000L) {
            sb.append(millis / 1000L).append(" sec ");
            millis = millis % 1000L;
        } // if
        if (millis > 0L) {
            sb.append(millis).append(" msec ");
        } // if

        return sb.deleteCharAt(sb.lastIndexOf(" ")).toString();
    }

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

    private BufferedReader br = null;
    private JTextArea progress_label;

    /**
     * Creates new progress object.
     * 
     * @param file
     *            File to read
     * @param show_progress
     *            Show visible GUI
     */
    public Progress(final File file, boolean show_progress) {
        this.line_nr = 0;

        this.show_progress = show_progress;
        this.start = System.currentTimeMillis();

        this.file_size = file.length();
        this.file_size_s = longToSize(file_size);

        // Stream to read from
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            log.error("open file", e);
        } // try-catch
        this.fc = fis.getChannel();

        // Create thingies when showing GUI
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

        // Close file at end if not already closed
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.debug("Run ShutdownHook: Close file {} ...", file.getName());
                finish();
            }
        });
    }

    /**
     * Close file.
     */
    public void finish() {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                log.error("Close file");
            } // try-catch
        } // if
    }

    /**
     * Reads next line from file.
     * 
     * @return String or null if no more lines.
     */
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

    /**
     * Get current line number.
     * 
     * @return long
     */
    public long getLineNr() {
        return line_nr;
    }

    /**
     * Update progress.
     * 
     * @return this
     */
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

            // TODO: compute next refresh_line_count

            // Compute speed for lines processed since last time
            speed = ((1 - refresh_beta) * last_speed) + (refresh_beta * ((line_nr - last_line_nr) * 1.f / diff_delta));

            // Compute time left from speed and remaining data
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

    /**
     * Get formattet multiline text for progress GUI.
     * 
     * @return String
     */
    public String getFormatted() {
        return String.format(
                "Progress: (%.2f %%)\n    In line %d (speed: %.2f %sL/s)\n    %s / %s\n    >>> %s (left: %s)", percent,
                line_nr, (speed > 1.f) ? speed : speed * 1000, (speed > 1.f) ? "k" : "", longToSize(position),
                file_size_s, longToTime(diff_total), longToTime(time_left));
    }

    /**
     * Returns true when a new percent is reached. After true is returned
     * successive calls will return false.
     * 
     * @return true for next percent
     */
    public boolean hasNewPercent() {
        if (has_new_percent) {
            has_new_percent = false;
            return true;
        } // if

        return false;
    }

    /**
     * Get percent of progress. (0-100)
     * 
     * @return int
     */
    public int getNewPercent() {
        return last_percent;
    }

    /**
     * Returns the time spend since start as milli seconds.
     * 
     * @return long with milli seconds
     */
    public long getTotalTime() {
        return System.currentTimeMillis() - start;
    }
}
