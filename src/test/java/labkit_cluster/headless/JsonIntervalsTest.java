
package labkit_cluster.headless;

import static org.junit.Assert.assertTrue;

import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;

import org.junit.Assert;
import org.junit.Test;

public class JsonIntervalsTest {

	FinalInterval interval = FinalInterval.createMinSize(10, 10, 5, 5);
	String json = "{\"min\":[10,10],\"max\":[14,14],\"n\":2}";

	@Test
	public void testToJson() {
		Assert.assertEquals(json, JsonIntervals.toJson(interval));
	}

	@Test
	public void testFromJson() {
		assertTrue(Intervals.equals(interval, JsonIntervals.fromJson(json)));
	}
}
