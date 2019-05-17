package labkit_cluster.interactive;

import com.esotericsoftware.minlog.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.scijava.parallel.ParallelizationParadigm;

import groovy.util.logging.Slf4j;

/**
 * This class enqueue calls to the {@link SegmentCommand}, and executes
 * them with a combined runAll call.
 */
@Slf4j
class CombineSegmentCommandCalls
{

	
	private static final long TIMEOUT = 20;

	private BlockingQueue< Task > queue;

	private final ParallelizationParadigm paradigm;

	private int queueLength;

	public CombineSegmentCommandCalls( ParallelizationParadigm paradigm, int queueLength ) {
		this.paradigm = paradigm;
		this.queueLength = queueLength;
		this.queue = new ArrayBlockingQueue<>( queueLength );
		final Worker worker = new Worker();
		worker.setPriority(Thread.MIN_PRIORITY);
		worker.setDaemon( true );
		worker.start();
	}

	/**
	 * Runs {@link SegmentCommand} with the given parameters,
	 * and waits for completion.
	 */
	public void run( Map<String, Object> parameters )
	{
		try
		{
			final Task task = new Task( parameters );
			queue.put( task );
			synchronized (queue) {
				queue.notify();
			}
			task.get();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	private class Worker extends Thread {

		@Override
		public  void run()
		{
			List< Task > batch = new ArrayList<>( queueLength );
			long timestamp;
			while(true) {
				timestamp = System.currentTimeMillis();
				synchronized(queue) {
					while (queue.size()< queueLength) {
						if (System.currentTimeMillis() - timestamp < TIMEOUT) {
							try {
								long waitTime = Math.max(TIMEOUT - System.currentTimeMillis() + timestamp, 0);
								if (waitTime > 0) {
									queue.wait(waitTime);
								}
							}
							catch (InterruptedException exc) {
								return;
							}
						} else if (!queue.isEmpty()){
							break;
						}
					}
				}
				
				queue.drainTo( batch );
				processTasks( batch );
				batch.clear();
				
			}
		}

		private void processTasks( List< Task > tasks )
		{
			try
			{
				if (!tasks.isEmpty()) {
					Log.info("tasks.size: " + tasks.size());
					List< Map< String, Object > > parameters = tasks.stream().map( Task::getParameters ).collect( Collectors.toList());
					paradigm.runAll( SegmentCommand.class, parameters );
				}
			} catch ( Exception e )
			{
				for( Task task : tasks )
					task.completeExceptionally( e );
			}
			for( Task task : tasks )
				task.complete( null );
		}
	}

	private static class Task extends CompletableFuture<Void>
	{

		private final Map<String, Object> parameters;

		public Task( Map<String, Object> parameters )
		{
			this.parameters = parameters;
		}

		public Map<String, Object> getParameters()
		{
			return parameters;
		}
	}
}
