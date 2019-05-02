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

	
	private static final int NCORES = 24;

	private BlockingQueue< Task > queue = new ArrayBlockingQueue<>( NCORES );

	private final ParallelizationParadigm paradigm;

	public CombineSegmentCommandCalls( ParallelizationParadigm paradigm ) {
		this.paradigm = paradigm;
		final Worker worker = new Worker();
		worker.setPriority((Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2);
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
			task.get();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			throw new RuntimeException( e );
		}
	}

	private class Worker extends Thread {

		@Override
		public void run()
		{
			List< Task > batch = new ArrayList<>( NCORES );
			while(true) {
				batch.clear();
				queue.drainTo( batch );
				processTasks( batch );
			}
		}

		private void processTasks( List< Task > tasks )
		{
			try
			{
				Log.info("tasks.size: " + tasks.size());
				List< Map< String, Object > > parameters = tasks.stream().map( Task::getParameters ).collect( Collectors.toList());
				paradigm.runAll( SegmentCommand.class, parameters );
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
