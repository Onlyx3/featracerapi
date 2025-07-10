package se.gu.defectpred;

import org.apache.commons.io.FileUtils;
import se.gu.data.DataController;
import se.gu.main.Configuration;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class generates a
 */
public class LIBSVMFileGenerator {
    DataController dc;
    Configuration configuration;

    public LIBSVMFileGenerator(Configuration configuration) throws SQLException, ClassNotFoundException {
        this.configuration = configuration;
        dc = new DataController(configuration);
    }
    //label <index:value> <index:value>...<indexN:valueN>
public void createLIBSVMFiles(){

}
    /**
     * Method returns attributes of featracer training data with indeces according to the LIBSVM format, indxed starting at 1
     * @return
     */
    public Map<String,Integer> getFeatRacerMetrics() {
        Map<String,Integer> map = new HashMap<>();

        String[] attributes = new String[]{"CSDEV", "COMM", "DDEV", "DCONT", "HDCONT", "CCC", "ACCC", "NLOC", "DNFMA", "NFMA", "NFF"};
        for(int i=0;i< attributes.length;i++){
            map.put(attributes[i],i+1 );
        }
        return map;
    }
    public Map<String,Integer> getDefectPredictionFeatureMetrics() {
        Map<String,Integer> map = new HashMap<>();

        String[] attributes = new String[]{"fcomm","fadev","fddev","fexp","foexp","fmodd","fnloc","fcyco","faddl","freml","scat","tanga","ndep","lofc"};
        for(int i=0;i< attributes.length;i++){
            map.put(attributes[i],i+1 );
        }
        return map;
    }

    public void createLIBSVMFromDefectPredictionCSVs() throws IOException {
        String filePath = "C:/studies/defectprediction/dataset_procstructmet_test_unlabeled.csv";
        //order of columns
        //dataset;project;release_number;feature;fcomm;fadev;fddev;fexp;foexp;fmodd;fnloc;fcyco;faddl;freml;scat;tanga;ndep;lofc;label
        String resultFile = "C:/studies/defectprediction/dataset_procstructmet_test_unlabeled_LIBSVM.txt";
        Utilities.deleteFile(resultFile);
        PrintWriter writer = new PrintWriter(new FileWriter(resultFile,true));
        List<String> lines = FileUtils.readLines(new File(filePath),configuration.getTextEncoding());
        for(int i=1;i<lines.size();i++){//skip header
            String[]items= lines.get(i).split(";");

            writer.print(items[items.length-1]);
            int k=0;//count metrics
            for(int m=4;m< items.length-1;m++){
                k++;
                if(m== items.length-2){
                    writer.printf(" %s:%s\n",k,items[m]);
                }else{
                    writer.printf(" %s:%s",k,items[m]);
                }
            }
        }
        writer.close();

    }
}
