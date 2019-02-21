package labkit_cluster.interactive;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.ImageJServerRunner;
import net.imagej.server.ImageJServerService;
import org.scijava.Context;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class InProcessImageJServerRunner extends AbstractImageJServerRunner
{

	private final ImageJServerService service;

	public InProcessImageJServerRunner(Context context)
	{
		service = context.service( ImageJServerService.class );
	}

	@Override
	public List< Integer > getPorts()
	{
		return Collections.singletonList(8080);
	}

	@Override
	public void close()
	{
		service.dispose();
	}

	@Override
	protected void doStartImageJServer() throws IOException
	{
		service.start();
	}
}
