import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class setup {
    public static void main(String args[]){
        String inpFile = args.length >= 1 ? args[0] : "P0-sample.txt.gz";
        String outputFile = args.length >= 2 ? args[1] : "P0-sampleout.txt";
        int k = args.length >= 3 ? Integer.parseInt(args[2]) : 7;
        
        try {
            FileInputStream fis = new FileInputStream(inpFile);
            GZIPInputStream gis = new GZIPInputStream(fis);
            BufferedReader reader = new BufferedReader(new InputStreamReader(gis));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            String strLine;
            int lines = 0;
            String maxStr = null, minStr = null;
            int counter = 0;
            while ((strLine = reader.readLine()) != null)   {
                lines++;
                String[] tokens = strLine.split(" ");
                if(tokens.length < k){
                    writer.write("Too Short\n");
                    continue;
                }
                else{
                    if(counter == 0){ 
                        minStr = tokens[k - 1]; 
                        maxStr = tokens[k - 1];
                        counter++;
                    }
                    writer.write(tokens[k - 1] + "\n");
                    if(tokens[k - 1].compareTo(minStr) < 1){ minStr = tokens[k - 1]; }
                    if(tokens[k - 1].compareTo(maxStr) > 1){ maxStr = tokens[k - 1]; }
                }
            }
            writer.write(lines + " " + minStr + " " + maxStr);
            reader.close();
            writer.close();
            gis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
