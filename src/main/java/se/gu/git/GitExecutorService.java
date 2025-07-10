package se.gu.git;

import java.io.BufferedReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GitExecutorService  implements Serializable {
    private static final long serialVersionUID = -4923582946188625502L;
    private ExecutorService executor;
    private List<Future<BufferedReader>> futures;

    public GitExecutorService() {
        futures = new ArrayList<Future<BufferedReader>>();
        executor = Executors.newCachedThreadPool();
    }
    public List<Future<BufferedReader>> getFutures() {
		/*List<Future<?>> listToReturn = new ArrayList<>();

		listToReturn.addAll(futures);
		futures.clear();*/
        return futures;

    }

    public Future<BufferedReader> submit(Callable<BufferedReader> task) {
        return executor.submit(task);
    }

    public void addFuture(Future<BufferedReader> future) {
        this.futures.add(future);
    }

    public void waitForTaskToFinish() {

        while (!allTasksCompleted()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //
    }
    /** Check if all tasks are done */
    private boolean allTasksCompleted() {
        boolean allDone = true;
        for (Future<?> future : futures) {
            allDone &= future.isDone(); // check if future is done
        }
        return allDone;
    }
    public void shutdown(){
        executor.shutdown();
    }


    public boolean isShutDown(){
        return executor.isShutdown();
    }


    public void clearFutures(){
        this.futures.clear();
    }
}

