package labkit_cluster.interactive;

import java.io.File;

import net.imagej.ImageJ;
import net.imglib2.labkit.LabkitFrame;
import net.imglib2.labkit.inputimage.InputImage;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.labkit.models.DefaultSegmentationModel;

import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

import cz.it4i.parallel.HPCBigDataServerRunTS;
import cz.it4i.parallel.HPCImageJServerRunner;
import cz.it4i.parallel.TestParadigm;
import cz.it4i.parallel.ui.HPCImageJServerRunnerWithUI;

/**
 * Starts Labkit, with a special segmentation algorithm, that
 * connects to an ImageJ-Server to perform the segmentation.
 */
public class StartLabkitOnHPC
{
	public static void main(String... args) {
		final String filename = fileExists("/please/specify/path/to/big-data-viewer/dataset.xml");
		// To get a big data viewer dataset:
		// 1. Open and 3d gray scale image in FIJI. For example: File > Open Samples > T1 Head.
		// 2. Run: Plugins > BigDataViewer > Export Current Image as XML/HDF5.

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		final Context context = ij.context();
		final HPCImageJServerRunner runner = HPCImageJServerRunnerWithUI.gui(
			context);
		final ParallelizationParadigm paradigm = new TestParadigm(runner, context);
		HPCBigDataServerRunTS runBGS = new HPCBigDataServerRunTS(runner, "~/bigdataserver/bigdataserver.sh", "/scratch/work/project/open-15-12/apps/bigdataserver/HisYFP-SPIM.xml");
		runBGS.run();
		final InputImage inputImage = new SpimDataInputImage( filename, 0 );
		DefaultSegmentationModel segmentationModel = new DefaultSegmentationModel( inputImage, context,
				( c, i ) -> new SciJavaParallelSegmenter( c, i, filename, paradigm ) );
		LabkitFrame.show(segmentationModel, "Demonstrate SciJava-Parallel used for Segmentation");

		Runtime.getRuntime().addShutdownHook( new Thread( paradigm::close ) );
	}

	private static String fileExists( String path )
	{
		if (!new File(path).exists()) {
			throw new RuntimeException( "File doesn't exist: " + path );
		}
		return path;
	}
}
