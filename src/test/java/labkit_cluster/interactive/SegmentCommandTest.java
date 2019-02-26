
package labkit_cluster.interactive;

import cz.it4i.parallel.TestParadigm;
import io.scif.services.DatasetIOService;
import labkit_cluster.headless.JsonIntervals;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SegmentCommandTest
{
	public final static String inputXml = "/home/arzt/Documents/Datasets/Mouse Brain/hdf5/export.xml";
	public final static String classifier = "/home/arzt/Documents/Datasets/Mouse Brain/hdf5/classifier.classifier";

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
		try (ParallelizationParadigm paradigm = new TestParadigm( new InProcessImageJServerRunner( context ), context ))
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
		Map<String, Object> map = new HashMap<>();
		map.put("input", inputXml );
		map.put("classifier", classifier );
		map.put("interval", JsonIntervals.toJson( output ));
		map.put( "output", wrapAsDataset( output ) );
		paradigm.runAll( SegmentCommand.class, Collections.singletonList( map ) );
	}

	private Dataset wrapAsDataset( RandomAccessibleInterval< UnsignedByteType > output )
	{
		final ImgPlus< UnsignedByteType > imgPlus = new ImgPlus<>( ImgView.wrap( Views.zeroMin( output ), null ) );
		Dataset example = new DefaultDataset( context, imgPlus );
		example.setName( "dummy.png" );
		return example;
	}

	private RandomAccessibleInterval< UnsignedByteType > createEmptyImage( Interval interval )
	{
		return Views.translate( ArrayImgs.unsignedBytes( Intervals.dimensionsAsLongArray( interval ) ), Intervals.minAsLongArray( interval ));
	}

}
