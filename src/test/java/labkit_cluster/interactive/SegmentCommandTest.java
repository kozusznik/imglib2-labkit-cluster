
package labkit_cluster.interactive;


import io.scif.services.DatasetIOService;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.labkit.utils.CheckedExceptionUtils;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import cz.it4i.parallel.fst.runners.InProcessFSTRPCServerRunner;
import cz.it4i.parallel.utils.TestParadigm;
import labkit_cluster.headless.JsonIntervals;

public class SegmentCommandTest
{
	// Note: SegmentCommandTest and SegmentCommandTest2 only work because
	// this file is on my laptop and on the ostrava cluster with the same
	// name.
	public final static String inputXml = "/scratch/work/project/open-15-12/datasets/matthias-mouse-brain/export.xml";

	public final static String classifier = SegmentCommandTest.class.getResource( "/mouse_brain.classifier" ).getPath();

	@Parameter
	private Context context;

	@Parameter
	private DatasetIOService ioService;

	@Parameter
	private UIService uiService;

	public static void main(String... args)
	{
		new SegmentCommandTest( new Context() ).run();
	}

	public SegmentCommandTest( Context context )
	{
		context.inject( this );
	}

	public void run()
	{
		try (ParallelizationParadigm paradigm = new TestParadigm( new InProcessFSTRPCServerRunner( context ), context ))
		{
			run( paradigm );
		}
	}

	private void run( ParallelizationParadigm paradigm )
	{
		final Interval interval = Intervals.createMinSize( 50, 0, 0, 50, 50, 50 );
		final RandomAccessibleInterval< UnsignedByteType > output = createEmptyImage( interval );
		segment( paradigm, output );
		uiService.show(Views.zeroMin( output ));
	}

	public void segment( ParallelizationParadigm paradigm, RandomAccessibleInterval< UnsignedByteType > output )
	{
		Map< String, Object > map = prepareParameters( output );
		paradigm.runAll( SegmentCommand.class, Collections.singletonList( map ) );
	}

	public Map< String, Object > prepareParameters( RandomAccessibleInterval< UnsignedByteType > output )
	{
		Map<String, Object> map = new HashMap<>();
		map.put("input", inputXml );
		map.put("classifier", readTextFile( classifier ) );
		map.put("interval", JsonIntervals.toJson( output ));
		map.put( "output", wrapAsDataset( output ) );
		return map;
	}

	private String readTextFile( String filename )
	{
		return CheckedExceptionUtils.run( () ->
				new String( Files.readAllBytes( Paths.get( filename ) ) )
		);
	}

	private static final AtomicInteger counter = new AtomicInteger();

	private Dataset wrapAsDataset( RandomAccessibleInterval< UnsignedByteType > output )
	{
		final ImgPlus< UnsignedByteType > imgPlus = new ImgPlus<>( ImgView.wrap( Views.zeroMin( output ), null ) );
		Dataset example = new DefaultDataset( context, imgPlus );
		example.setName( "dummy" + counter.incrementAndGet() + ".png" );
		return example;
	}

	private RandomAccessibleInterval< UnsignedByteType > createEmptyImage( Interval interval )
	{
		return Views.translate( ArrayImgs.unsignedBytes( Intervals.dimensionsAsLongArray( interval ) ), Intervals.minAsLongArray( interval ));
	}

}
