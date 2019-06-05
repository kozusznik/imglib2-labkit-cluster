package labkit_cluster.interactive;

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

import bdv.util.BdvFunctions;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import cz.it4i.parallel.fst.runners.HPCFSTRPCServerRunnerUI;
import cz.it4i.parallel.fst.utils.TestFSTRPCParadigm;
import cz.it4i.parallel.runners.HPCImageJServerRunner;
import cz.it4i.parallel.runners.HPCSettings;
import cz.it4i.parallel.ui.HPCSettingsGui;
import labkit_cluster.utils.CombineSegmentCommandCalls;

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

	static ParallelizationParadigm initHpcParadigm( Context context )
	{
		
		
		final HPCSettings settings = HPCSettingsGui.showDialog(context);

		final HPCImageJServerRunner runner = new HPCFSTRPCServerRunnerUI(settings,
			settings.isShutdownOnClose());
		return TestFSTRPCParadigm.runner(runner, context);
	}

	SegmentCommandTest segmentCommandTest;
	private Context context;

	InteractiveSegmentationDemo( Context context )
	{
		this.context = context;
		segmentCommandTest = new SegmentCommandTest( context );
	}

	private void run( ParallelizationParadigm paradigm )
	{
		SpimDataInputImage input = new SpimDataInputImage( SegmentCommandTest.inputXml, 0 );
		long[] dim = Intervals.dimensionsAsLongArray( input.imageForSegmentation() );
		CombineSegmentCommandCalls s = new CombineSegmentCommandCalls(context ,paradigm, 12 );
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
