
package labkit_cluter;

import cz.it4i.parallel.TestParadigm;
import labkit_cluster.JsonIntervals;
import labkit_cluster.LabkitClusterCommand;
import labkit_cluster.MyN5;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.labkit.inputimage.SpimDataInputImage;
import net.imglib2.util.Intervals;
import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class LabkitClusterCommandTest {

	public final static String OUTPUT_N5_DIRECTORY =
		"/home/arzt/tmp/output/result.xml";

	private Context context = new Context();

	public static void main(String... args) throws ExecutionException,
		InterruptedException
	{
		new LabkitClusterCommandTest().startServerAndRun();
	}

	public void startServerAndRun() throws ExecutionException,
		InterruptedException
	{
		try (ParallelizationParadigm paradigm = TestParadigm.localImageJServer( "/home/arzt/Applications/Fiji.app/ImageJ-linux64", context ))
		{
			run(paradigm);
		}
	}

	private void run(ParallelizationParadigm paradigm) {
		final String inputXml =
			"/home/arzt/Documents/Datasets/Mouse Brain/hdf5/export.xml";
		final String value =
			"/home/arzt/Documents/Datasets/Mouse Brain/hdf5/classifier.classifier";
		final long[] dimensions = Intervals.dimensionsAsLongArray(
			new SpimDataInputImage(inputXml, 0).interval());
		CellGrid grid = new CellGrid(dimensions, new int[] { 100, 100, 100 });
		MyN5.createDataset(OUTPUT_N5_DIRECTORY, grid);
		List<Map<String, Object>> parameters = initializeParameters(OUTPUT_N5_DIRECTORY,
			inputXml, value, grid);
		paradigm.runAll(LabkitClusterCommand.class, parameters);
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

	private List<Map<String, Object>> initializeParameters(String outputPath,
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
