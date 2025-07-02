package se.gu.git;

import java.io.*;
import java.util.concurrent.Callable;

public class StreamGobbler implements Runnable, Callable<BufferedReader>, Serializable {
    private static final long serialVersionUID = -8248851171382555581L;
    private final InputStream is;
    private final String type;

    public StreamGobbler(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }
    @Override
    public void run() {
if(type.equalsIgnoreCase("ERROR")){
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(type + "> " + line);
        }
    } catch (IOException ioe) {
        ioe.printStackTrace();
    }
}
    }

    @Override
    public BufferedReader call() throws Exception {
        BufferedReader bufferedReader;
        bufferedReader = new BufferedReader(new InputStreamReader(is));
        return bufferedReader;
    }
}
