package labkit_cluster.interactive;

import static labkit_cluster.interactive.SegmentCommandTest.inputXml;

import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;

import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

import bdv.util.BdvFunctions;
import bdv.util.volatiles.VolatileViews;
import cz.it4i.parallel.fst.runners.InProcessFSTRPCServerRunner;
import cz.it4i.parallel.utils.TestParadigm;

public class SegmentCommandTest2
{
	public static void main(String... args) {
		Context context = new Context();
		ParallelizationParadigm paradigm = new TestParadigm( new InProcessFSTRPCServerRunner( context ), context );
		new SegmentCommandTest2(context).run(paradigm);
		Runtime.getRuntime().addShutdownHook( new Thread( paradigm::close ) );
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
			segmentCommandTest.segment( paradigm, cell );
		};
		final DiskCachedCellImgOptions options = DiskCachedCellImgOptions.options().cellDimensions( 50, 50, 50 );
		final DiskCachedCellImgFactory< UnsignedByteType > factory = new DiskCachedCellImgFactory<>( new UnsignedByteType(), options );
		DiskCachedCellImg< UnsignedByteType, ? > img = factory.create( dim, loader );
		BdvFunctions.show( VolatileViews.wrapAsVolatile( img ), "title" ).setDisplayRange( 0, 1 );
	}

}
