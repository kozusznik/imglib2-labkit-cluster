
package labkit_cluter;

import bdv.util.BdvFunctions;
import bdv.util.volatiles.VolatileViews;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import java.io.IOException;

public class ShowResult {

	public static void main(String... args) throws IOException {
		N5FSReader reader = new N5FSReader("/home/arzt/salomon/output");
		RandomAccessibleInterval<UnsignedByteType> result = N5Utils
			.openWithDiskCache(reader, "segmentation", new UnsignedByteType());
		BdvFunctions.show(VolatileViews.wrapAsVolatile(result), "N5!!")
			.setDisplayRange(0, 1);
	}
}
