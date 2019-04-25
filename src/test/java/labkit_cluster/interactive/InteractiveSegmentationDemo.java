package labkit_cluster.interactive;

import bdv.util.BdvFunctions;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import cz.it4i.parallel.HPCImageJServerRunner;
import cz.it4i.parallel.TestParadigm;
import cz.it4i.parallel.ui.HPCImageJServerRunnerWithUI;
import net.imagej.ImageJ;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

public class InteractiveSegmentationDemo
{
	public static void main( String... args )
	{
		final ImageJ imageJ = new ImageJ();
		imageJ.ui().showUI();
		Context context = imageJ.context();
		ParallelizationParadigm paradigm = initHpcParadigm( context );
		Runtime.getRuntime().addShutdownHook( new Thread( paradigm::close ) );
		new InteractiveSegmentationDemo( context ).run( paradigm );
	}

	private static ParallelizationParadigm initHpcParadigm( Context context )
	{
		final HPCImageJServerRunner runner = HPCImageJServerRunnerWithUI.gui(
				context );
		return new TestParadigm( runner, context );
	}

	private static TestParadigm initTestParadigm( Context context )
	{
		return new TestParadigm( new InProcessImageJServerRunner( context ), context );
	}

	SegmentCommandTest segmentCommandTest;

	InteractiveSegmentationDemo( Context context )
	{
		segmentCommandTest = new SegmentCommandTest( context );
	}

	private void run( ParallelizationParadigm paradigm )
	{
		SpimDataInputImage input = new SpimDataInputImage( SegmentCommandTest.inputXml, 0 );
		long[] dim = Intervals.dimensionsAsLongArray( input.imageForSegmentation() );
		CombineSegmentCommandCalls s = new CombineSegmentCommandCalls( paradigm );
		final CellLoader< UnsignedByteType > loader = cell -> {
			System.out.println( System.identityHashCode( Thread.currentThread() ) );
			s.run( segmentCommandTest.prepareParameters( cell ) );
		};
		final DiskCachedCellImgOptions options = DiskCachedCellImgOptions.options().cellDimensions( 50, 50, 50 );
		final DiskCachedCellImgFactory< UnsignedByteType > factory = new DiskCachedCellImgFactory<>( new UnsignedByteType(), options );
		DiskCachedCellImg< UnsignedByteType, ? > img = factory.create( dim, loader );
		SharedQueue queue = new SharedQueue( 24 );
		BdvFunctions.show( VolatileViews.wrapAsVolatile( img, queue ), "title" ).setDisplayRange( 0, 1 );
	}

}
