package labkit_cluster.interactive;

import labkit_cluster.headless.JsonIntervals;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.labkit.inputimage.InputImage;
import net.imglib2.labkit.labeling.Labeling;
import net.imglib2.labkit.segmentation.weka.TrainableSegmentationSegmenter;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SciJavaParallelSegmenter extends TrainableSegmentationSegmenter
{
	private final String filename;

	private final ParallelizationParadigm paradigm;

	private final Context context;

	private File model;

	public SciJavaParallelSegmenter( Context context, InputImage inputImage, String filename, ParallelizationParadigm paradigm )
	{
		super( context, inputImage );
		this.context = context;
		this.filename = filename;
		this.paradigm = paradigm;
	}

	@Override
	public void train( List< Pair< ? extends RandomAccessibleInterval< ? >, ? extends Labeling > > trainingData )
	{
		super.train( trainingData );
		if( super.isTrained() ) {
			try
			{
				File model = File.createTempFile( "labkit-", ".classifier" );
				saveModel( model.getAbsolutePath() );
				this.model = model;
			}
			catch ( IOException e )
			{
				throw new RuntimeException( e );
			}
		}
	}

	@Override
	public void segment( RandomAccessibleInterval< ? > image, RandomAccessibleInterval< ? extends IntegerType< ? > > labels )
	{
		segment( paradigm, filename, model.getAbsolutePath(), labels );
	}

	private void segment( ParallelizationParadigm paradigm, String inputXml, String classifier, RandomAccessibleInterval< ? extends IntegerType<?> > output )
	{
		Map<String, Object> map = new HashMap<>();
		map.put("input", inputXml );
		map.put("classifier", classifier );
		map.put("interval", JsonIntervals.toJson( output ));
		map.put( "output", wrapAsDataset( output ) );
		paradigm.runAll( SegmentCommand.class, Collections.singletonList( map ) );
	}

	private Dataset wrapAsDataset( RandomAccessibleInterval< ? extends RealType<?> > output )
	{
		final ImgPlus imgPlus = new ImgPlus( ImgView.wrap( Views.zeroMin( (RandomAccessibleInterval) output ), null ) );
		Dataset example = new DefaultDataset( context, imgPlus );
		example.setName( "dummy.png" );
		return example;
	}
}
