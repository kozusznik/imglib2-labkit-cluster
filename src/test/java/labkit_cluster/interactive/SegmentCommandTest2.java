package labkit_cluster.interactive;

import bdv.util.BdvFunctions;
import cz.it4i.parallel.TestParadigm;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

import static labkit_cluster.interactive.SegmentCommandTest.inputXml;

public class SegmentCommandTest2
{
	public static void main(String... args) {
		Context context = new Context();
		try( ParallelizationParadigm paradigm = new TestParadigm( new InProcessImageJServerRunner( context ), context ) )
		{
			new SegmentCommandTest2(context).run(paradigm);
		}
	}

	SegmentCommandTest segmentCommandTest;

	SegmentCommandTest2(Context context) {
		segmentCommandTest = new SegmentCommandTest(context);
	}

	private void run( ParallelizationParadigm paradigm )
	{
		SpimDataInputImage input = new SpimDataInputImage( inputXml, 0 );
		long[] dim = Intervals.dimensionsAsLongArray( input.imageForSegmentation() );
		final CellLoader< UnsignedByteType > loader = cell -> {
			synchronizedSegment( paradigm, ArrayImgs.unsignedBytes(50, 50, 50) );
		};
		final DiskCachedCellImgOptions options = DiskCachedCellImgOptions.options().cellDimensions( 50, 50, 50 );
		final DiskCachedCellImgFactory< UnsignedByteType > factory = new DiskCachedCellImgFactory<>( new UnsignedByteType(), options );
		DiskCachedCellImg< UnsignedByteType, ? > img = factory.create( dim, loader );
		BdvFunctions.show( img, "title" ).setDisplayRange( 0, 1 );
	}

	synchronized
	private void synchronizedSegment( ParallelizationParadigm paradigm, RandomAccessibleInterval< UnsignedByteType > output )
	{
		System.err.println( "start cell" );
		try{
			segmentCommandTest.segment(paradigm, output);
		}
		catch ( Exception e ) {
			e.printStackTrace();
			waitForever();
		}
		finally
		{
			System.err.println( "cell done" );
		}
	}

	private void waitForever()
	{
		try
		{
			while (true)
				Thread.sleep( 1000 );
		}
		catch ( InterruptedException e )
		{
			throw new RuntimeException( e );
		}
	}
}
