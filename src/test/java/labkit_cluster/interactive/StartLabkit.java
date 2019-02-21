package labkit_cluster.interactive;

import cz.it4i.parallel.TestParadigm;
import net.imglib2.labkit.LabkitFrame;
import net.imglib2.labkit.inputimage.InputImage;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.labkit.models.DefaultSegmentationModel;
import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

public class StartLabkit
{
	public static void main(String... args) {
		Context context = new Context();
		try (ParallelizationParadigm paradigm = TestParadigm.localImageJServer( "/home/arzt/Applications/Fiji.app/ImageJ-linux64", context ))
		{
			final String filename = "/home/arzt/Documents/Datasets/Mouse Brain/hdf5/export.xml";
			final InputImage inputImage = new SpimDataInputImage( filename, 0 );
			DefaultSegmentationModel segmentationModel = new DefaultSegmentationModel( inputImage, context,
					( c, i ) -> new SciJavaParallelSegmenter( c, i, filename, paradigm ) );
			LabkitFrame.show(segmentationModel, "Demonstrate other Segmenter");
		}
	}
}
