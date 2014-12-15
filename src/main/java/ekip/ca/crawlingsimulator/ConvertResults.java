package ekip.ca.crawlingsimulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConvertResults {

    private final static String NL = "\r\n";
    private final static String TAB = "\t";
    private final static String PATH = "C:\\Users\\M.A.C\\Desktop\\5000-4";

    public static void main(String[] args) {
        System.out.println("Starting: parse");
        // readFile("F:\\Uni\\Praktikum Computational Advertising\\Diagramme\\Daten\\5000-4");
        readFile(PATH);
        System.out.println("Finished: parse");
    }

    public static void readFile(String path) {
        StringBuffer output = new StringBuffer();
        output.append("steps").append(TAB).append("quali").append(NL);
        int counter = 1;
        int lastStep = 110;
        try (BufferedReader br = new BufferedReader(new FileReader(path + ".txt"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                // System.out.println(sCurrentLine);
                if (counter > 100) {
                    if (counter > lastStep) {
                        output.append(counter).append(TAB).append(sCurrentLine).append(NL);
                        lastStep = lastStep + 80;
                        if (lastStep > 4999) {
                            lastStep = 4999;
                        }
                        counter++;
                    } else {
                        counter++;
                    }
                } else {
                    output.append(counter).append(TAB).append(sCurrentLine).append(NL);
                    counter++;
                }
            } // end of while
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writefile((path + "-parsed.txt"), output.toString());
        } // end of try
    }

    public static void writefile(String path, String data) {
        File f = new File(path);
        try {
            FileWriter writer = new FileWriter(f, true);
            BufferedWriter bwriter = new BufferedWriter(writer);
            bwriter.write(data);
            bwriter.flush();
            bwriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
