
package labkit_cluster;

import bdv.util.BdvFunctions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import java.io.IOException;

public class ShowResult {

	public static void main(String... args) throws IOException {
		N5FSReader reader = new N5FSReader(
			LabkitClusterCommandTest.OUTPUT_N5_DIRECTORY);
		RandomAccessibleInterval<UnsignedByteType> result = N5Utils
			.openWithDiskCache(reader, "segmentation", new UnsignedByteType());
		BdvFunctions.show(result, "N5!!").setDisplayRange(0, 1);
	}
}
