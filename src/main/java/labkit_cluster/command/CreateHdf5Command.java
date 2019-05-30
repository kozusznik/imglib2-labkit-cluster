package labkit_cluster.command;

import bdv.export.HDF5Saver;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import picocli.CommandLine;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;

import static labkit_cluster.command.PrepareCommand.N5_DATASET_NAME;

@CommandLine.Command( name = "create-hdf5", description = "Save the segmentation as BigDataViewer compatible HDF5." )
public class CreateHdf5Command implements Callable<Optional<Integer> >
{
	@CommandLine.Option( names = { "-N", "--n5" }, description = "N5 folder that contains the intermediate results.", required = true )
	private File n5;

	@CommandLine.Option( names = { "-X", "--xml" }, description = "Location to store the XML and HDF5 files, that can be opened with BigDataViewer.", required = true )
	private File xml;

	@Override
	public Optional< Integer > call() throws Exception
	{
		N5FSReader reader = new N5FSReader( n5.getAbsolutePath() );
		RandomAccessibleInterval< UnsignedByteType > result = N5Utils
				.openWithDiskCache( reader, N5_DATASET_NAME, new UnsignedByteType() );
		new HDF5Saver().save( xml.getAbsolutePath(), result );
		return Optional.of( 0 ); // exit code
	}
}
