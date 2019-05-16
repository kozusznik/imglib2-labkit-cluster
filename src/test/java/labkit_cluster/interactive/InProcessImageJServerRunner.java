package labkit_cluster.interactive;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import net.imagej.server.ImageJServerService;

import org.scijava.Context;

import cz.it4i.parallel.AbstractImageJServerRunner;


public class InProcessImageJServerRunner extends AbstractImageJServerRunner {
	private final ImageJServerService service;

	public InProcessImageJServerRunner(Context context)
	{
		super(true);
		service = context.service( ImageJServerService.class );
	}

	@Override
	public List< Integer > getPorts()
	{
		return Collections.singletonList(8080);
	}

	@Override
	public int getNCores()
	{
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void shutdown() {
		service.dispose();
	}

	@Override
	protected void doStartImageJServer() throws IOException
	{
		service.start();
	}

}
