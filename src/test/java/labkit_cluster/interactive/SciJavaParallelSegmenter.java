package labkit_cluster.interactive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.labkit.inputimage.InputImage;
import net.imglib2.labkit.labeling.Labeling;
import net.imglib2.labkit.segmentation.weka.TrainableSegmentationSegmenter;
import net.imglib2.labkit.utils.CheckedExceptionUtils;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Pair;

import org.scijava.Context;

import labkit_cluster.headless.JsonIntervals;

public class SciJavaParallelSegmenter extends TrainableSegmentationSegmenter
{
	private final String filename;

	private final CombineSegmentCommandCalls calls;


	private File model;

	public SciJavaParallelSegmenter( Context context, InputImage inputImage, String filename, CombineSegmentCommandCalls calls)
	{
		super( context, inputImage );
		this.filename = filename;
		this.calls = calls;
	}

	@Override
	public void train( List< Pair< ? extends RandomAccessibleInterval< ? >, ? extends Labeling > > trainingData )
	{
		super.train( trainingData );
		if( super.isTrained() ) {
			try
			{
				File model = File.createTempFile( "labkit-", ".classifier" );
				model.deleteOnExit();
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
	public void openModel( String path )
	{
		this.model = new File( path );
		super.openModel( path );
	}

	@Override
	public void segment( RandomAccessibleInterval< ? > image, RandomAccessibleInterval< ? extends IntegerType< ? > > labels )
	{
		segment( filename, model.getAbsolutePath(), labels );
	}


	private void segment( String inputXml, String classifier, RandomAccessibleInterval<? extends IntegerType<?>> output ) {
		Map<String, Object> map = new HashMap<>();
		map.put("input", inputXml );
		map.put("classifier", readTextFile( classifier ) );
		map.put("interval", JsonIntervals.toJson( output ));
		map.put( "output", output );
		calls.run( map );
	}

	private static String readTextFile( String classifier )
	{
		return CheckedExceptionUtils.run( () -> new String( Files.readAllBytes( Paths.get( classifier ) ) ) );
	}

}
