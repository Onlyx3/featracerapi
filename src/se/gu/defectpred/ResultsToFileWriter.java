package se.gu.defectpred;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.locks.StampedLock;

public class ResultsToFileWriter {
    private StampedLock lock = new StampedLock();
    public void writeResult(String line, File resultsFile) {
        long stamp = lock.writeLock();
        PrintWriter writer=null;
        try {
            writer = new PrintWriter(new FileWriter(resultsFile, true));
            writer.println(line);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if(writer!=null){
                writer.close();
            }
            lock.unlockWrite(stamp);
        }
    }
}
