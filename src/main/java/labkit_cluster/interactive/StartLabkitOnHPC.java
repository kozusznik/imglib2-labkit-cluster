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
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import cz.it4i.parallel.RunningRemoteServer;
import cz.it4i.parallel.fst.runners.HPCFSTRPCServerRunnerUI;
import cz.it4i.parallel.fst.utils.TestFSTRPCParadigm;
import cz.it4i.parallel.runners.HPCImageJServerRunner;
import cz.it4i.parallel.runners.HPCSettings;
import cz.it4i.parallel.ui.HPCSettingsGui;
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
	
	static ParallelizationParadigm initHpcParadigm( Context context )
	{
		
		
		final HPCSettings settings = HPCSettingsGui.showDialog(context);

		final HPCImageJServerRunner runner = new HPCFSTRPCServerRunnerUI(settings);
		return TestFSTRPCParadigm.runner(runner, context);
	}

	@Parameter
	private Context context;
	
	@Parameter
	private File file;
	
	@Override
	public void run() {
		String filename = file.toString();
		fileExists(filename);
		final ParallelizationParadigm paradigm = initHpcParadigm(context);
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
