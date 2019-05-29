package labkit_cluster.command;

import bdv.export.HDF5Saver;
import bdv.util.BdvFunctions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.labkit.segmentation.weka.TrainableSegmentationSegmenter;
import net.imglib2.labkit.utils.LabkitUtils;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.scijava.Context;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class LabkitCommand
{

	public static final String PREPARE = "prepare";

	public static final String SEGMENT = "segment";

	public static final String SHOW = "show";

	public static final String CREATE_HDF5 = "create-hdf5";

	public static final String N5_DATASET_NAME = "segmentation";

	public static void main( String... args )
	{
		try
		{
			int exitCode = mainWithoutExit( args );
			if ( exitCode >= 0 )
				System.exit( exitCode );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			System.exit( 3 );
		}
	}

	public static int mainWithoutExit( String... args )
	{
		try
		{
			String command = args[ 0 ];
			if ( command.equals( PREPARE ) )
			{
				String image = args[ 1 ];
				String output = args[ 2 ];
				try
				{
					prepare( image, output );
					return 0;
				}
				catch ( Exception e )
				{
					System.err.println( "Creating the N5 output dataset failed." );
					e.printStackTrace();
					return 1;
				}
			}
			else if ( command.equals( SEGMENT ) )
			{
				String image = args[ 1 ];
				String classifier = args[ 2 ];
				String output = args[ 3 ];
				int index = Integer.valueOf( args[ 4 ] );
				int count = Integer.valueOf( args[ 5 ] );
				try
				{
					segment( image, classifier, output, index, count );
					return 0;
				}
				catch ( Exception e )
				{
					System.err.println( "Segmentation failed." );
					e.printStackTrace();
					return 1;
				}
			}
			else if ( command.equals( SHOW ) )
			{
				String image = args[ 1 ];
				showN5( image );
				return -1;
			}
			else if ( command.equals( CREATE_HDF5 ) )
			{
				String n5 = args[ 1 ];
				String hdf5 = args[ 2 ];
				try
				{
					createHdf5( n5, hdf5 );
					return 0;
				}
				catch ( Exception e )
				{
					System.err.println( "Writing hdf5 failed:" );
					e.printStackTrace();
					return 1;
				}
			}
		}
		catch ( Exception e )
		{
			System.err.println( "Error parsing the command line." );
			e.printStackTrace();
		}
		showUsage();
		return 2;
	}

	private static void showUsage()
	{
		String command = "java -jar imglib2-labkit-cluster.jar ";
		System.err.println( "Usage:" );
		System.err.println( "    " + command + PREPARE + " path/to/hdf5/image.xml path/to/output/dir" );
		System.err.println( "    " + command + SEGMENT + " imglib2-labkit-cluster.jar path/to/model.classifier path/to/output/dir {index} {count}" );
		System.err.println( "    " + command + SHOW + " path/to/output/dir" );
		System.err.println( "    " + command + CREATE_HDF5 + " path/to/output/dir path/to/output.xml" );
	}

	private static void prepare( String imageXml, String output ) throws IOException
	{
		SpimDataInputImage image = new SpimDataInputImage( imageXml, 0 );
		CellGrid grid = LabkitUtils.suggestGrid( image.interval(), image.isTimeSeries() );
		N5Writer writer = new N5FSWriter( output );
		int[] cellDimensions = new int[ grid.numDimensions() ];
		grid.cellDimensions( cellDimensions );
		writer.createDataset( N5_DATASET_NAME, grid.getImgDimensions(),
				cellDimensions, DataType.UINT8, new GzipCompression() );
	}

	private static void segment( String imageXml, String classifier, String output, int index, int numberOfChunks )
	{
		try
		{
			SpimDataInputImage image = new SpimDataInputImage( imageXml, 0 );
			TrainableSegmentationSegmenter segmenter = openSegmenter( classifier, image );
			writeN5Range( output, index, numberOfChunks, block -> segmenter.segment( image.imageForSegmentation(), block ) );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	static void showN5( String path )
	{
		try
		{
			N5FSReader reader = new N5FSReader( path );
			RandomAccessibleInterval< UnsignedByteType > result = N5Utils
					.openWithDiskCache( reader, "segmentation", new UnsignedByteType() );
			BdvFunctions.show( result, "N5" ).setDisplayRange( 0, 5 );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	private static void createHdf5( String n5, String xml ) throws IOException
	{
		N5FSReader reader = new N5FSReader( n5 );
		RandomAccessibleInterval< UnsignedByteType > result = N5Utils
				.openWithDiskCache( reader, N5_DATASET_NAME, new UnsignedByteType() );
		new HDF5Saver().save( xml, result );
	}

	private static TrainableSegmentationSegmenter openSegmenter( String classifier, SpimDataInputImage image )
	{
		TrainableSegmentationSegmenter segmenter = new TrainableSegmentationSegmenter( new Context(), image );
		segmenter.openModel( classifier );
		return segmenter;
	}

	private static void writeN5Range( String output, int index, int numberOfChunks, Consumer< RandomAccessibleInterval< UnsignedByteType > > loader ) throws IOException
	{
		N5Writer writer = new N5FSWriter( output );
		long[] gridDimensions = getCellGrid( writer ).getGridDimensions();
		long size = Intervals.numElements( gridDimensions );
		long chunkSize = ( size + numberOfChunks - 1 ) / numberOfChunks;
		long start = index * chunkSize;
		long end = Math.min( size, start + chunkSize );
		for ( long i = start; i < end; i++ )
		{
			long[] blockOffset = new long[ gridDimensions.length ];
			IntervalIndexer.indexToPosition( i, gridDimensions, blockOffset );
			saveBlock( writer, blockOffset, loader );
			System.out.println( "Block " + ( i - start ) + " of " + ( end - start ) + " has been segmented. Block coordinates: " + Arrays.toString( blockOffset ) );
		}
	}

	private static void saveBlock( N5Writer writer, long[] blockOffset, Consumer< RandomAccessibleInterval< UnsignedByteType > > loader ) throws IOException
	{
		CellGrid grid = getCellGrid( writer );
		long[] cellMin = new long[ grid.numDimensions() ];
		int[] cellDims = new int[ grid.numDimensions() ];
		grid.getCellDimensions( blockOffset, cellMin, cellDims );
		Img< UnsignedByteType > block = ArrayImgs.unsignedBytes( toLongs( cellDims ) );
		loader.accept( Views.translate( block, cellMin ) );
		N5Utils.saveBlock( block, writer, N5_DATASET_NAME, blockOffset );
	}

	private static CellGrid getCellGrid( N5Writer writer ) throws IOException
	{
		DatasetAttributes attributes = writer.getDatasetAttributes( N5_DATASET_NAME );
		return new CellGrid( attributes.getDimensions(), attributes.getBlockSize() );
	}

	private static long[] toLongs( int[] values )
	{
		return IntStream.of( values ).mapToLong( x -> x ).toArray();
	}
}
