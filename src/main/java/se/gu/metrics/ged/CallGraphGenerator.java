package se.gu.metrics.ged;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class CallGraphGenerator implements Runnable {
    private String callGraphRepository;

    public CallGraphGenerator(String callGraphRepository) {
        this.callGraphRepository = callGraphRepository;
    }

    @Override
    public void run() {
        try {
        String processDirectory  = getProcessDirectory();

        String command = String.format("js-callgraph --cg \"%s\" --output callgraph.json", callGraphRepository);
        //write command to a batch file since executing commands directly doesnt seem to work well

        //TODO:check what OS it is then use correct batch file e.g. .bat on windows

        File batchFile =  getBatFile(processDirectory, command);

        //System.out.printf("batch file exists %b",batchFile.exists());

        ProcessBuilder builder = new ProcessBuilder(batchFile.getAbsolutePath());
        builder.directory(new File(processDirectory));
        //start process
        final Process process = builder.start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String getProcessDirectory() throws FileNotFoundException {
        File file = new File(callGraphRepository);
        if (!file.exists()) {
            throw new FileNotFoundException(String.format("%s does not exist", callGraphRepository));
        }

        return file.isDirectory() ? callGraphRepository : file.getParent();
    }

    private File getBatFile(String processDirectory, String command) throws IOException {
        FileWriter fileWriter = new FileWriter(processDirectory + "\\callgraph.bat", false);
        fileWriter.write(command);
        fileWriter.close();
        return new File(processDirectory + "\\callgraph.bat");
    }
}
