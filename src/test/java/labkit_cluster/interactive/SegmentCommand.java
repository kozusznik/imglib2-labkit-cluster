package labkit_cluster.interactive;

import labkit_cluster.headless.JsonIntervals;
import net.imagej.Dataset;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.labkit.segmentation.weka.TrainableSegmentationSegmenter;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class )
public class SegmentCommand implements Command
{
	@Parameter
	private String input;

	@Parameter
	private String classifier;

	@Parameter
	private String interval;

	@Parameter(type = ItemIO.BOTH)
	private Dataset output;

	@Parameter
	private Context context;

	@Override
	public void run()
	{
		SpimDataInputImage inputImage = new SpimDataInputImage( input, 0 );
		TrainableSegmentationSegmenter segmenter = new TrainableSegmentationSegmenter(context, inputImage);
		segmenter.openModel( classifier );
		Interval interval = JsonIntervals.fromJson( this.interval );
		final RandomAccessibleInterval translated = Views.translate( this.output, Intervals.minAsLongArray( interval ) );
		segmenter.segment( inputImage.imageForSegmentation(), translated );
	}
}
