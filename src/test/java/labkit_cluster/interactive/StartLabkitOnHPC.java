package labkit_cluster.interactive;

import java.io.File;
import java.util.List;

import net.imagej.ImageJ;
import net.imglib2.labkit.LabkitFrame;
import net.imglib2.labkit.inputimage.InputImage;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.labkit.models.DefaultSegmentationModel;

import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

import cz.it4i.parallel.RunningRemoteServer;

/**
 * Starts Labkit, with a special segmentation algorithm, that
 * connects to an ImageJ-Server to perform the segmentation.
 */
public class StartLabkitOnHPC
{
	public static void main(String... args) {
		final String filename = fileExists("/home/koz01/Documents/Datasets/Mouse Brain/hdf5/export.xml");
		// To get a big data viewer dataset:
		// 1. Open and 3d gray scale image in FIJI. For example: File > Open Samples > T1 Head.
		// 2. Run: Plugins > BigDataViewer > Export Current Image as XML/HDF5.

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		final Context context = ij.context();
		final ParallelizationParadigm paradigm = InteractiveSegmentationDemo.initHpcParadigm(context);
		final InputImage inputImage = new SpimDataInputImage( filename, 0 );
		final CombineSegmentCommandCalls calls = new CombineSegmentCommandCalls(paradigm, getSumOfCores(paradigm));
		DefaultSegmentationModel segmentationModel = new DefaultSegmentationModel( inputImage, context,
				( c, i ) -> new SciJavaParallelSegmenter( c, i, filename, calls ) );
		LabkitFrame.show(segmentationModel, "Demonstrate SciJava-Parallel used for Segmentation");
		
		Runtime.getRuntime().addShutdownHook( new Thread( paradigm::close ) );
	}

	private static int getSumOfCores(ParallelizationParadigm paradigm) {
		List<Integer> coreList = ((RunningRemoteServer) paradigm).getNCores();
		return coreList.stream().reduce(0, (a, b) -> a + b);
	}

	private static String fileExists( String path )
	{
		if (!new File(path).exists()) {
			throw new RuntimeException( "File doesn't exist: " + path );
		}
		return path;
	}
}
