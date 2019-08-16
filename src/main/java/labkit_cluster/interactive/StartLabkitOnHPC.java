package labkit_cluster.interactive;

import java.io.File;
import java.util.List;

import net.imagej.ImageJ;
import net.imglib2.labkit.LabkitFrame;
import net.imglib2.labkit.inputimage.InputImage;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.labkit.models.DefaultSegmentationModel;
import net.imglib2.labkit.segmentation.PredictionLayer;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.Status;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.UIService;

import cz.it4i.parallel.MultipleHostParadigm;
import labkit_cluster.headless.SciJavaParallelSegmenter;
import labkit_cluster.utils.CombineSegmentCommandCalls;

// Select 'xml' file with data for BDV in first open dialog. Local path to the file must same as a path on cluster.
/**
 * Starts Labkit, with a special segmentation algorithm, that
 * connects to an ImageJ-Server to perform the segmentation.
 */
@Plugin(type = Command.class, menuPath = "Plugins > Segmentation > Labkit on HPC")
public class StartLabkitOnHPC implements Command {

	public static void main(String... args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		final Context context = ij.context();
		CommandService commandService = context.getService(CommandService.class);
		commandService.run(StartLabkitOnHPC.class, true);
	}

	private static int getSumOfCores(ParallelizationParadigm paradigm) {
		List<Integer> coreList = ((MultipleHostParadigm) paradigm).getNCores();
		return coreList.stream().reduce(0, (a, b) -> a + b);
	}

	private static String fileExists( String path )
	{
		if (!new File(path).exists()) {
			throw new RuntimeException( "File doesn't exist: " + path );
		}
		return path;
	}
	
	@Parameter
	private Context context;
	
	@Parameter
	private File file;
	
	@Parameter
	private ParallelService parallelService; 
	
	@Override
	public void run() {
		String filename = file.toString();
		fileExists(filename);
		final ParallelizationParadigm paradigm = parallelService.getParadigm();
		if (paradigm == null) {
			context.getService(UIService.class).showDialog(
				"There is no Parrallel Paradigm selected." +
					" You can manage in Plugins>Scijava parallel>Paradigm Profiles Manager",
				MessageType.WARNING_MESSAGE);
			return;
		}
		if (paradigm.getStatus() == Status.NON_ACTIVE) {
			paradigm.init();
		}
		final InputImage inputImage = new SpimDataInputImage( filename, 0 );
		int nCores = getSumOfCores(paradigm);
		final CombineSegmentCommandCalls calls = new CombineSegmentCommandCalls(context, paradigm, nCores);
		System.setProperty(PredictionLayer.SIZE_OF_QUEUE, nCores + "");
		DefaultSegmentationModel segmentationModel = new DefaultSegmentationModel( inputImage, context,
				( c, i ) -> new SciJavaParallelSegmenter( c, i, filename, calls ) );
		LabkitFrame.show(segmentationModel, "Demonstrate SciJava-Parallel used for Segmentation");
		
		Runtime.getRuntime().addShutdownHook( new Thread( paradigm::close ) );
		
	}
}
