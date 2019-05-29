package labkit_cluster.command;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LabkitCommandTest
{

	private static final String imageXml = "/home/arzt/Documents/Datasets/Mouse Brain/hdf5/export.xml";

	private static final String classifier = "/home/arzt/Documents/Datasets/Mouse Brain/hdf5/classifier.classifier";

	@Test
	public void testCreateDataset()
	{
		Path output = createOutputN5();
		List< String > files = Arrays.asList( output.toFile().list() );
		assertTrue( files.contains( "attributes.json" ) );
		assertTrue( files.contains( "segmentation" ) );
	}

	private static Path createOutputN5()
	{
		try
		{
			Path path = Files.createTempDirectory( "test-dataset" );
			int exitCode = LabkitCommand.mainWithoutExit( LabkitCommand.PREPARE, imageXml, path.toString() );
			assertEquals( 0, exitCode );
			return path;
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	@Ignore
	@Test
	public void testSegmentRange()
	{
		Path output = createOutputN5();
		int exitCode = LabkitCommand.mainWithoutExit( LabkitCommand.SEGMENT, imageXml, classifier, output.toString(), "50", "100" );
		assertEquals( 0, exitCode );
	}

	@Test
	public void testSaveHdf5() throws IOException
	{
		int exitCode = LabkitCommand.mainWithoutExit( LabkitCommand.CREATE_HDF5, "/home/arzt/tmp/output", "/home/arzt/tmp/seg.xml" );
		assertEquals( 0, exitCode );
	}

	public static void main( String... args )
	{
		Path output = createOutputN5();
		LabkitCommand.mainWithoutExit( "segment-range", imageXml, classifier, output.toString(), "50", "100" );
		LabkitCommand.showN5( output.toString() );
	}
}
