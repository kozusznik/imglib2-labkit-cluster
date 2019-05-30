package labkit_cluster.command;

import picocli.CommandLine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(
		name = "java -jar imglib2-labkit-cluster.jar",
		subcommands = { PrepareCommand.class, SegmentCommand.class, ShowCommand.class, CreateHdf5Command.class },
		description = "Labkit command line tool for the segmentation of large files."
)
public class LabkitClusterCommand implements Callable< Optional< Integer > >
{

	@Override
	public Optional< Integer > call() throws Exception
	{
		CommandLine.usage( new LabkitClusterCommand(), System.err );
		return Optional.of( 1 ); // exit code
	}

	public static void main( String... args ) {
		try
		{
			Optional< Integer > exitCode = run( args );
			exitCode.ifPresent( System::exit );
		} catch ( Exception e ) {
			e.printStackTrace();
			System.exit( 3 );
		}
	}

	static Optional< Integer > run( String... args )
	{
		List< Object > exitCodes = new CommandLine( new LabkitClusterCommand() ).parseWithHandlers(
				new CommandLine.RunLast(),
				CommandLine.defaultExceptionHandler().andExit( 1 ),
				args
		);
		@SuppressWarnings( "unchecked" )
		Optional< Integer > exitCode = ( Optional< Integer > ) exitCodes.get( 0 );
		return exitCode;
	}
}
