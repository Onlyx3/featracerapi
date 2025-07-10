package se.gu.ml.preprocessing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MetricsExecutorService  implements Serializable {
    private static final long serialVersionUID = -8291420548304005285L;
    private ExecutorService executor;
    private List<Future<MetricValue>> futures;

    public MetricsExecutorService() {
        futures = new ArrayList<Future<MetricValue>>();
        executor = Executors.newCachedThreadPool();
    }

    public Future<?> submit(Runnable task) {
        return executor.submit(task);

    }

    public Future<MetricValue> submit(Callable<MetricValue> task) {
        return executor.submit(task);
    }


    public void addFuture(Future<MetricValue> future) {
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

    public List<Future<MetricValue>> getFutures() {
		/*List<Future<?>> listToReturn = new ArrayList<>();

		listToReturn.addAll(futures);
		futures.clear();*/
        return futures;

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
