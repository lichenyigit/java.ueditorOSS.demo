package ueditorOSS.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class ExecutorUtil {
	private static final Logger logger = LogManager.getLogger();
	private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, new NamePrefixedThreadFactory("Schedule-pool-thread-"));

	private static final ThreadPoolExecutor workerExecutor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 5000L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1),
			new NamePrefixedThreadFactory("worker-pool-thread-"));
	private static final Map<String, ScheduledFuture<?>> registedScheduleJobMap = new HashMap<>();
	private static final Map<String, ScheduledFuture<?>> registedRepeatJobMap = new HashMap<>();
	public static String scheduleJobAtFixRate(Job repeatJob,long firstDelay,long interval){
		String refKey = repeatJob.getRefKey();
		synchronized (registedRepeatJobMap) {
			if(registedRepeatJobMap.containsKey(refKey)){
				registedRepeatJobMap.get(refKey).cancel(false);
			}
			ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(repeatJob, firstDelay, interval, TimeUnit.MILLISECONDS);
			registedRepeatJobMap.put(refKey, scheduledFuture);
			return refKey;
		}
	}
	
	public static void scheduleJobAtTime(final Job job,long timestamp){
		final String jobKey = job.getRefKey();
		synchronized (registedScheduleJobMap) {
			if(registedScheduleJobMap.containsKey(jobKey)){
				logger.trace("Try to schedule job["+jobKey+"], but duplicated, cancelled.");
				return;
			}else{
				registedScheduleJobMap.put(jobKey,
					scheduledExecutorService.schedule(new Runnable() {
						public void run() {
							job.run();
							synchronized (registedScheduleJobMap) {
								registedScheduleJobMap.remove(jobKey);
							}
						}
					}, timestamp- System.currentTimeMillis(), TimeUnit.MILLISECONDS));
				logger.trace("Try to schedule job["+jobKey+"], successed.");
			}
		}
	}
	
	public static void cancelRepeatJob(String refKey){
		synchronized (registedRepeatJobMap){
			ScheduledFuture<?> scheduledFuture = registedRepeatJobMap.remove(refKey);
			if(scheduledFuture!=null){
				scheduledFuture.cancel(false);
			}
		}
	}
	
	public static void cancelScheduleJob(String refKey){
		synchronized (registedScheduleJobMap) {
			ScheduledFuture<?> scheduledFuture = registedScheduleJobMap.remove(refKey);
			if(scheduledFuture!=null){
				scheduledFuture.cancel(false);
			}
		}
	}
	
	public static void run(Job runnable){
		workerExecutor.execute(runnable);
	}
	
	/**
	 * 鍏抽棴鎵ц鍣�
	 */
	public static void shutdownNow(){
		try{
			scheduledExecutorService.shutdownNow();
		}finally{
			workerExecutor.shutdownNow();
		}
		ExecutorService pool = Executors.newFixedThreadPool(2,new NamePrefixedThreadFactory("shutdown-thread-pool"));
		try {
			pool.invokeAll(Arrays.asList(new Callable<Void>(){
				public Void call() throws Exception{
					scheduledExecutorService.awaitTermination(1000*30, TimeUnit.MILLISECONDS);
					return null;
			}},new Callable<Void>(){
				public Void call() throws Exception{
					workerExecutor.awaitTermination(1000*30, TimeUnit.MILLISECONDS);
					return null;
			}}), 1000*30, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		pool.shutdown();
		try {
			pool.awaitTermination(1000*30, TimeUnit.MICROSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public interface Job extends Runnable{
		public String getRefKey();
	}
	
	public static class NamePrefixedThreadFactory implements ThreadFactory {
		private String namePrefix;
		private long counter = 0;;
		public NamePrefixedThreadFactory(String namePrefix) {
			this.namePrefix = namePrefix;
		}
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, namePrefix+counter++);
		}
	}
}

