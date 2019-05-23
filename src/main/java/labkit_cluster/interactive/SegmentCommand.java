package labkit_cluster.interactive;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

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

import labkit_cluster.headless.JsonIntervals;

@Plugin( type = Command.class )
public class SegmentCommand implements Command
{
	@Parameter
	private String input;

	@Parameter
	private String classifier;

	@Parameter
	private String interval;

	@SuppressWarnings("rawtypes")
	@Parameter(type = ItemIO.BOTH)
	private Dataset output;

	@Parameter
	private Context context;

	@Override
	public void run()
	{
		SpimDataInputImage inputImage = null;
		if (System.getProperty("SegmentCommand.no_first_run") == null) {
			 synchronized(SegmentCommand.class) {
				 inputImage = new SpimDataInputImage( input, 0 );
				 System.setProperty("SegmentCommand.no_first_run","true"); 
			 }
		} else {
			 inputImage = new SpimDataInputImage( input, 0 );
		}
		TrainableSegmentationSegmenter segmenter = new TrainableSegmentationSegmenter(context, inputImage);
		segmenter.openModel( storeToFile( classifier ) );
		Interval interval = JsonIntervals.fromJson( this.interval );
		final RandomAccessibleInterval translated = Views.translate( this.output, Intervals.minAsLongArray( interval ) );
		segmenter.segment( inputImage.imageForSegmentation(), translated );
	}

	static private String storeToFile(String storedClassifier) {
		try {
			File model = File.createTempFile("labkit-", ".classifier");
			com.google.common.io.Files.write(storedClassifier, model, Charset.defaultCharset());
			return model.toString();
		}
		catch (IOException exc) {
			throw new RuntimeException(exc);
		}
		

	}
}
