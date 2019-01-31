
package labkit_cluter;

import cz.it4i.parallel.ImageJServerParadigm;
import labkit_cluster.JsonIntervals;
import labkit_cluster.LabkitClusterCommand;
import labkit_cluster.MyN5;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.util.Intervals;
import org.scijava.Context;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigmProfile;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class LabkitClusterCommandTest {

	public final static String OUTPUT_N5_DIRECTORY =
		"/home/training/dd-18-37-29/output";
	final String CLASSIFIER_PATH =
		"/home/training/dd-18-37-29/input/classifier.classifier";
	private final String inputXml = "/home/training/dd-18-37-29/input/export.xml";
	private static List<String> hosts = Arrays.asList("localhost:10001",
		"localhost:10002", "localhost:10003", "localhost:10004");

	private Context context = new Context();
	private ParallelService parallelService = context.service(
		ParallelService.class);

	public static void main(String... args) throws ExecutionException,
		InterruptedException
	{
		new LabkitClusterCommandTest().startServerAndRun();
	}

	public void startServerAndRun() throws ExecutionException,
		InterruptedException
	{
		// Process process = StartImageJServer.startImageJServerIfNecessary(
		// "/home/arzt/Applications/Fiji.app/");
		try (ParallelizationParadigm paradigm = getTestParadigm(parallelService)) {
			run(paradigm);
		}
		finally {
			// if (process != null) process.destroy();
		}
	}

	public static ParallelizationParadigm getTestParadigm(
		ParallelService parallelService)
	{
		parallelService.deleteProfiles();
		parallelService.addProfile(new ParallelizationParadigmProfile(
			ImageJServerParadigm.class, "lonelyBiologist01"));
		parallelService.selectProfile("lonelyBiologist01");
		ParallelizationParadigm paradigm = parallelService.getParadigm();
		((ImageJServerParadigm) paradigm).setHosts(hosts);
		paradigm.init();
		return paradigm;
	}

	private void run(ParallelizationParadigm paradigm) {
		final long[] dimensions = Intervals.dimensionsAsLongArray(
			new SpimDataInputImage("/home/arzt/salomon/input/export.xml", 0)
				.interval());
		CellGrid grid = new CellGrid(dimensions, new int[] { 100, 100, 100 });
		MyN5.createDataset("/home/arzt/salomon/output", grid);
		List<Map<String, ?>> parameters = initializeParameters(OUTPUT_N5_DIRECTORY,
			inputXml, CLASSIFIER_PATH, grid);
		paradigm.runAll(nCopies(LabkitClusterCommand.class, parameters.size()),
			parameters);
		System.out.println("Results written to: " + OUTPUT_N5_DIRECTORY);
	}

	private <T> List<T> nCopies(T value, int size) {
		return new AbstractList<T>() {

			@Override
			public T get(int index) {
				return value;
			}

			@Override
			public int size() {
				return size;
			}
		};
	}

	private List<Map<String, ?>> initializeParameters(String outputPath,
		String inputXml, String value, CellGrid grid)
	{
		List<Interval> cells = listIntervals(grid);
		return cells.stream().map(interval -> {
			Map<String, Object> map = new HashMap<>();
			map.put("input", inputXml);
			map.put("output", outputPath);
			map.put("classifier", value);
			map.put("interval", JsonIntervals.toJson(interval));
			return map;
		}).collect(Collectors.toList());
	}

	private static List<Interval> listIntervals(CellGrid grid) {
		int numCells = (int) Intervals.numElements(grid.getGridDimensions());
		int n = grid.numDimensions();
		return new AbstractList<Interval>() {

			@Override
			public Interval get(int index) {
				long[] min = new long[n];
				long[] max = new long[n];
				int[] size = new int[n];
				grid.getCellDimensions(index, min, size);
				for (int i = 0; i < min.length; i++)
					max[i] = min[i] + size[i] - 1;
				return new FinalInterval(min, max);
			}

			@Override
			public int size() {
				return numCells;
			}
		};
	}
}
