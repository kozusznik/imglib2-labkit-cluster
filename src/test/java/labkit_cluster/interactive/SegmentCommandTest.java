
package labkit_cluster.interactive;

import cz.it4i.parallel.TestParadigm;
import io.scif.services.DatasetIOService;
import labkit_cluster.headless.JsonIntervals;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.FinalInterval;
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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SegmentCommandTest
{

	@Parameter
	private Context context;

	@Parameter
	private DatasetIOService ioService;

	@Parameter
	private UIService uiService;

	public static void main(String... args)
	{
		final SegmentCommandTest segmentCommandTest = new SegmentCommandTest();
		new Context().inject( segmentCommandTest );
		segmentCommandTest.run();
	}

	public void run()
	{
		try (ParallelizationParadigm paradigm = new TestParadigm( new InProcessImageJServerRunner( context ), context ))
		{
			run( paradigm );
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	private void run( ParallelizationParadigm paradigm ) throws IOException
	{
		final String inputXml = "/home/arzt/Documents/Datasets/Mouse Brain/hdf5/export.xml";
		final String value = "/home/arzt/Documents/Datasets/Mouse Brain/hdf5/classifier.classifier";
		final Interval interval = Intervals.createMinSize( 50, 0, 0, 50, 50, 50 );
		final RandomAccessibleInterval< UnsignedByteType > output = createEmptyImage( interval );
		segment( paradigm, inputXml, value, output );
		uiService.show(Views.zeroMin( output ));
	}

	private void segment( ParallelizationParadigm paradigm, String inputXml, String value, RandomAccessibleInterval< UnsignedByteType > output )
	{
		Map<String, Object> map = new HashMap<>();
		map.put("input", inputXml );
		map.put("classifier", value );
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
