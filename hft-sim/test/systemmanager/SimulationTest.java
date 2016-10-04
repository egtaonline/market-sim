package systemmanager;

import static org.junit.Assert.assertEquals;
import static systemmanager.Consts.AgentType.ZIR;

import java.io.StringReader;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import data.AgentProperties;

public class SimulationTest {
	
	private static final Gson gson = new Gson();
	// Don't modify!
	private static final JsonObject baseSpec = gson.toJsonTree(ImmutableMap.of(
			"assignment", ImmutableMap.of(
					"role", ImmutableList.of()
					),
			"configuration", ImmutableMap.of(
					Keys.PRIVATE_VALUE_VAR, 5e6,
					Keys.FUNDAMENTAL_SHOCK_VAR, 1e6,
					Consts.AgentType.ZI.toString(), Keys.NUM + "_" + 0,
					Keys.TICK_SIZE, 1
					)
			)).getAsJsonObject();
	
	static JsonObject getBaseSpec() {
		// XXX Ugly deep copy. Can be made more efficient, but probably not necessary
		return gson.fromJson(gson.toJson(baseSpec), JsonObject.class);
	}
	
	/*
	 * This is the result of a major bug that caused default spec properties to
	 * not propagate to players.
	 */
	@Test
	public void defaultPropertiesTest() {
		JsonObject rawSpec = getBaseSpec();
		rawSpec.get("assignment").getAsJsonObject().get("role").getAsJsonArray().add(new JsonPrimitive(ZIR.toString()));
		SimulationSpec spec = new SimulationSpec(new StringReader(rawSpec.toString()));
		// Test correct spec, since it'd be too hard to test the actual created
		// agents, since knowledge is sealed away... Could use reflection...
		Map<String, Multiset<AgentProperties>> players = spec.playerProps;
		assertEquals(1, players.size());
		Multiset<AgentProperties> strats = players.get("role");
		assertEquals(1, strats.size());
		AgentProperties props = Iterables.getOnlyElement(strats);
		
		assertEquals(ZIR, props.getAgentType());
		assertEquals(5e6, props.getAsDouble(Keys.PRIVATE_VALUE_VAR), 0);
		assertEquals(1, props.getAsInt(Keys.TICK_SIZE));
	}
	
	@Test
	public void defaultPropertiesModificationsTest() {
		JsonObject rawSpec = getBaseSpec();
		rawSpec.get("configuration").getAsJsonObject().add(Keys.PRIVATE_VALUE_VAR, new JsonPrimitive(7e8));
		rawSpec.get("assignment").getAsJsonObject().get("role").getAsJsonArray().add(new JsonPrimitive(ZIR + ""));
		SimulationSpec spec = new SimulationSpec(new StringReader(rawSpec.toString()));
		// Test correct spec, since it'd be too hard to test the actual created
		// agents, since knowledge is sealed away... Could use reflection...
		Map<String, Multiset<AgentProperties>> players = spec.playerProps;
		assertEquals(1, players.size());
		Multiset<AgentProperties> strats = players.get("role");
		assertEquals(1, strats.size());
		AgentProperties props = Iterables.getOnlyElement(strats);
		
		assertEquals(ZIR, props.getAgentType());
		assertEquals(7e8, props.getAsDouble(Keys.PRIVATE_VALUE_VAR), 0);
		assertEquals(1, props.getAsInt(Keys.TICK_SIZE));
	}
	
}
