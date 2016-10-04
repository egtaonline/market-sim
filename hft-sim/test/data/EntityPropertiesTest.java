package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import systemmanager.Keys;

public class EntityPropertiesTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void emptyProps() {
		EntityProperties props = EntityProperties.empty();
		
		assertEquals("", props.configString);
		assertEquals(0, props.properties.size());
	}
	
	@Test
	public void basicProps() {
		EntityProperties props = EntityProperties.fromPairs(
				Keys.GAMMA, 0.005,
				Keys.ETA, 10,
				Keys.THETA, Long.MIN_VALUE,
				Keys.WITHDRAW_ORDERS, false,
				Keys.ASSIGN, "assign",
				Keys.BETA_R, Float.MAX_VALUE,
				Keys.STRATS, "1-2-5");
		
		assertEquals(0.005, props.getAsDouble(Keys.GAMMA), 1E-6);
		assertEquals(10, props.getAsInt(Keys.ETA));
		assertEquals("assign", props.getAsString(Keys.ASSIGN));
		assertEquals(Long.MIN_VALUE, props.getAsLong(Keys.THETA));
		assertEquals(false, props.getAsBoolean(Keys.WITHDRAW_ORDERS));
		assertEquals(Float.MAX_VALUE, props.getAsFloat(Keys.BETA_R), 1E-6);
		int[] spreads = new int[]{1,2,5};
		int[] propSpreads = props.getAsIntArray(Keys.STRATS);
		for (int i = 0; i < 2; i++)
			assertEquals(spreads[i], propSpreads[i]);
		
		exception.expect(IllegalStateException.class);
		props.getAsDouble("nonexistentKey");
	}
	
	@Test
	public void booleanTest() {
		EntityProperties props = EntityProperties.fromPairs(
				Keys.GAMMA, true,
				Keys.ETA, false,
				Keys.THETA, "T",
				Keys.ALPHA, "f");
		
		assertTrue(props.getAsBoolean(Keys.GAMMA));
		assertTrue(props.getAsBoolean(Keys.THETA));
		assertFalse(props.getAsBoolean(Keys.ALPHA));
		assertFalse(props.getAsBoolean(Keys.ETA));
		
		assertTrue(props.getAsBoolean(Keys.GAMMA, Keys.ETA));
		assertTrue(props.getAsBoolean("nonexistentKey", Keys.GAMMA));
		assertTrue(props.getAsBoolean("nonexistentKey", Keys.THETA));
		assertFalse(props.getAsBoolean("nonexistentKey", Keys.ALPHA));
		assertFalse(props.getAsBoolean("nonexistentKey", Keys.ETA));
		
		String config = "gamma_T_eta_f";
		EntityProperties props2 = EntityProperties.fromConfigString(config);
		assertTrue(props2.getAsBoolean(Keys.GAMMA));
		assertFalse(props2.getAsBoolean(Keys.ETA));
		
	}
	
	@Test
	public void copyProps() {
		EntityProperties props = EntityProperties.fromPairs(
				Keys.GAMMA, 0.005,
				Keys.ETA, 10);
		
		EntityProperties copy = EntityProperties.copy(props);
		assertEquals(props.getAsDouble(Keys.GAMMA), copy.getAsDouble(Keys.GAMMA), 1E-6);
		assertEquals(props.getAsInt(Keys.ETA), copy.getAsInt(Keys.ETA));
	}
	
	@Test
	public void copyPropsFromPairs() {
		EntityProperties props = EntityProperties.fromPairs(
				Keys.GAMMA, 0.005,
				Keys.ETA, 10);
		
		EntityProperties copy = EntityProperties.copyFromPairs(props, Keys.THETA, -1, Keys.ETA, 20);
		assertEquals(props.getAsDouble(Keys.GAMMA), copy.getAsDouble(Keys.GAMMA), 1E-6);
		assertNotEquals(props.getAsInt(Keys.ETA), copy.getAsInt(Keys.ETA));
		assertEquals(20, copy.getAsInt(Keys.ETA));
		assertEquals(-1, copy.getAsInt(Keys.THETA));
	}
	
	@Test
	public void propsFromConfigString() {
		String config = "gamma_0.005_eta_10";
		
		EntityProperties props = EntityProperties.fromConfigString(config);
		assertEquals(0.005, props.getAsDouble(Keys.GAMMA), 1E-6);
		assertEquals(10, props.getAsInt(Keys.ETA));
	}
	
	@Test
	public void nullProps() {
		// test with using a default key
		EntityProperties props = EntityProperties.fromPairs(
				Keys.NUM, 12);
		
		exception.expect(IllegalStateException.class);
		props.getAsString(Keys.AGENT_TICK_SIZE, "nonExistentKey");
	}
	
	@Test
	public void defaultStringProps() {
		// test with using a default key
		EntityProperties props = EntityProperties.fromPairs(
				Keys.NUM, 12);
		
		assertEquals(new String("12"), props.getAsString(Keys.NUM));
		assertEquals(new String("12"), props.getAsString(Keys.GAMMA, Keys.NUM));
		assertEquals(new String("12"), props.getAsString(Keys.THETA, Keys.NUM));
		
		exception.expect(IllegalStateException.class);
		props.getAsInt("nonexistentKey", "nonExistentKey");
	}
	
	@Test
	public void defaultIntProps() {
		// test with using a default key
		EntityProperties props = EntityProperties.fromPairs(
				Keys.THETA, 1,
				Keys.THETA_MAX, 10);
		
		assertEquals(1, props.getAsInt(Keys.THETA, Keys.THETA_MAX));
		assertEquals(1, props.getAsInt(Keys.THETA, "nonExistentKey"));
		assertEquals(10, props.getAsInt("nonExistentKey", Keys.THETA_MAX));
		
		exception.expect(IllegalStateException.class);
		props.getAsInt("nonexistentKey", "nonExistentKey");
	}
	
	@Test
	public void defaultDoubleProps() {
		// test with using a default key
		EntityProperties props = EntityProperties.fromPairs(
				Keys.THETA, 1.0,
				Keys.THETA_MAX, 10.0);
		
		assertEquals(1.0, props.getAsDouble(Keys.THETA, Keys.THETA_MAX), 1E-6);
		assertEquals(1.0, props.getAsDouble(Keys.THETA, "nonExistentKey"), 1E-6);
		assertEquals(10.0, props.getAsDouble("nonExistentKey", Keys.THETA_MAX), 1E-6);
		
		exception.expect(IllegalStateException.class);
		props.getAsDouble("nonexistentKey", "nonExistentKey");
	}
	
	@Test
	public void defaultLongProps() {
		// test with using a default key
		EntityProperties props = EntityProperties.fromPairs(
				Keys.THETA, 1,
				Keys.THETA_MAX, 10);
		
		assertEquals(1, props.getAsLong(Keys.THETA, Keys.THETA_MAX));
		assertEquals(1, props.getAsLong(Keys.THETA, "nonExistentKey"));
		assertEquals(10, props.getAsLong("nonExistentKey", Keys.THETA_MAX));
		
		exception.expect(IllegalStateException.class);
		props.getAsLong("nonexistentKey", "nonExistentKey");
	}
	
	@Test
	public void defaultFloatProps() {
		// test with using a default key
		EntityProperties props = EntityProperties.fromPairs(
				Keys.THETA, 1.0,
				Keys.THETA_MAX, 10.0);
		
		assertEquals(1.0, props.getAsFloat(Keys.THETA, Keys.THETA_MAX), 1E-6);
		assertEquals(1.0, props.getAsFloat(Keys.THETA, "nonExistentKey"), 1E-6);
		assertEquals(10.0, props.getAsFloat("nonExistentKey", Keys.THETA_MAX), 1E-6);
		
		exception.expect(IllegalStateException.class);
		props.getAsFloat("nonexistentKey", "nonExistentKey");
	}
	
	@Test
	public void defaultBooleanProps() {
		// test with using a default key
		EntityProperties props = EntityProperties.fromPairs(
				Keys.THETA, true,
				Keys.THETA_MAX, false);
		
		assertTrue(props.getAsBoolean(Keys.THETA, Keys.THETA_MAX));
		assertTrue(props.getAsBoolean(Keys.THETA, "nonExistentKey"));
		assertFalse(props.getAsBoolean("nonExistentKey", Keys.THETA_MAX));
		
		exception.expect(IllegalStateException.class);
		props.getAsBoolean("nonexistentKey", "nonExistentKey");
	}
}
