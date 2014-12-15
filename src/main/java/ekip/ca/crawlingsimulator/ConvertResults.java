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
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        System.out.println("Starting: parse");
        //readFile("F:\\Uni\\Praktikum Computational Advertising\\Diagramme\\Daten\\5000-4");
        readFile("C:\\Users\\M.A.C\\Desktop\\5000-4");
        System.out.println("Finished: parse");
    }

    public static void readFile(String path) {
        BufferedReader br = null;
        String output = "steps" + TAB + "quali" + NL;
        int counter = 1;
        int lastStep = 110;
        try {
            String sCurrentLine;
            br = new BufferedReader(new FileReader(path + ".txt"));
            while ((sCurrentLine = br.readLine()) != null) {
                // System.out.println(sCurrentLine);
                if( counter > 100) {
                    if(counter > lastStep){
                        output = output + counter + TAB + sCurrentLine + NL;
                        lastStep = lastStep + 80;
                        if(lastStep > 4999) {
                            lastStep = 4999;
                        }
                        counter++;
                    } else {
                        counter++;
                    }
                } else {
                    output = output + counter + TAB + sCurrentLine + NL;
                    counter++;
                }
            } // end of while
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            writefile((path + "-parsed.txt"), output);
        } // end of try
    }
    
    public static void writefile (String path, String data)
    {
       File f = new File( path );
         try
         {
          FileWriter writer = new FileWriter(f ,true);
          BufferedWriter bwriter = new BufferedWriter (writer);
          bwriter.write(data);
          bwriter.flush();
          bwriter.close();
           
         }
         catch (IOException e) {
             e.printStackTrace();
           }
    }

}
