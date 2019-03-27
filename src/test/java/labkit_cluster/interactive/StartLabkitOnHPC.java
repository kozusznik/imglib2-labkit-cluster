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
		final String bdsLocationOnCluster =
			"/scratch/work/project/open-15-12/apps/bigdataserver/bigdataserver.sh";

		final String filenameOnCluster = "/scratch/work/project/open-15-12/apps/bigdataserver/export.xml";
		// To get a big data viewer dataset:
		// 1. Open and 3d gray scale image in FIJI. For example: File > Open Samples > T1 Head.
		// 2. Run: Plugins > BigDataViewer > Export Current Image as XML/HDF5.

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		final Context context = ij.context();
		final HPCImageJServerRunnerWithUI runner = HPCImageJServerRunnerWithUI.gui(
			context);
		final ParallelizationParadigm paradigm = new TestParadigm(runner, context);
		final HPCBigDataServerRunTS runBGS = new HPCBigDataServerRunTS(runner,
			bdsLocationOnCluster, filenameOnCluster);
		final String remoteURL = runBGS.run();
		final InputImage inputImage = new SpimDataInputImage( remoteURL, 0 );
		DefaultSegmentationModel segmentationModel = new DefaultSegmentationModel(
			inputImage, context, (c, i) -> new SciJavaParallelSegmenter(c, i,
				filenameOnCluster, paradigm));
		LabkitFrame.show(segmentationModel, "Demonstrate SciJava-Parallel used for Segmentation");
		if (runner.isShutdownJob()) {
			Runtime.getRuntime().addShutdownHook( new Thread( paradigm::close ) );
		}
	}

	@SuppressWarnings("unused")
	private static String fileExists( String path )
	{
		if (!new File(path).exists()) {
			throw new RuntimeException( "File doesn't exist: " + path );
		}
		return path;
	}
}
