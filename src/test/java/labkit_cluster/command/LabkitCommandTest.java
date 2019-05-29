package labkit_cluster.command;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LabkitCommandTest
{

	private static final String imageXml = LabkitCommandTest.class.getResource( "/small-t1-head/input.xml" ).getPath();

	private static final String classifier = LabkitCommandTest.class.getResource( "/small-t1-head/small-t1-head.classifier" ).getPath();

	private static final String n5 = LabkitCommandTest.class.getResource( "/small-t1-head/segmentation.n5" ).getPath();

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

	@Test
	public void testSegmentRange() throws IOException
	{
		Path output = createOutputN5();
		int exitCode1 = LabkitCommand.mainWithoutExit( LabkitCommand.SEGMENT, imageXml, classifier, output.toString(), "0", "2" );
		assertEquals( 0, exitCode1 );
		int exitCode2 = LabkitCommand.mainWithoutExit( LabkitCommand.SEGMENT, imageXml, classifier, output.toString(), "1", "2" );
		assertEquals( 0, exitCode2 );
		assertTrue( output.resolve( LabkitCommand.N5_DATASET_NAME ).resolve( "0/0/0" ).toFile().exists() );
		System.out.println( output );
	}

	@Test
	public void testSaveHdf5() throws IOException
	{
		File file = File.createTempFile( "test-data", ".xml" );
		assertTrue( file.delete() );
		int exitCode = LabkitCommand.mainWithoutExit( LabkitCommand.CREATE_HDF5, n5, file.getAbsolutePath() );
		assertEquals( 0, exitCode );
		assertTrue( file.exists() );
	}

	public static void main( String... args )
	{
		LabkitCommand.mainWithoutExit( LabkitCommand.SHOW, n5 );
	}
}
