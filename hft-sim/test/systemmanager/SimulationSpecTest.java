package systemmanager;

import static org.junit.Assert.assertEquals;
import static systemmanager.Consts.Presets.CENTRALCALL;
import static systemmanager.Consts.Presets.CENTRALCDA;
import static systemmanager.Consts.Presets.TWOMARKET;
import static systemmanager.Consts.Presets.TWOMARKETLA;
import static systemmanager.Consts.Presets.MAXEFF;

import java.io.StringReader;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import data.AgentProperties;
import data.MarketProperties;

public class SimulationSpecTest {

	@Test
	public void centralCDAPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, CENTRALCDA.toString());
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(1, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case LA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
	}
	
	@Test
	public void centralCallPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, CENTRALCALL.toString());
		config.addProperty(Keys.NBBO_LATENCY, 1337);
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(1, mp.getAsInt(Keys.NUM));
				assertEquals(1337, mp.getAsInt(Keys.CLEAR_INTERVAL));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case LA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
	}
	
	@Test
	public void twoMarketPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, TWOMARKET.toString());
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(2, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case LA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
	}
	
	@Test
	public void twoMarketLAPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, TWOMARKETLA.toString());
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(2, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case LA:
				assertEquals(1, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
	}

	@Test
	public void maxEffPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, MAXEFF.toString());
		config.addProperty(Keys.MAX_POSITION, Defaults.get(Keys.MAX_POSITION));
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		assertEquals(12, spec.simulationProperties.getAsInt(Keys.SIMULATION_LENGTH));
		
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(1, mp.getAsInt(Keys.NUM));
				assertEquals(10, mp.getAsInt(Keys.CLEAR_INTERVAL));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case MAXEFFICIENCY:
				assertEquals(66, mp.getAsInt(Keys.NUM));
				assertEquals(Integer.parseInt(Defaults.get(Keys.MAX_POSITION)), mp.getAsInt(Keys.MAX_POSITION));
				break;
			default:
			}
		}
	}
	
	
	/**
	 * Tests different reentry rates for background & MM, also tests creation of
	 * players via simulation spec. Can only test via if the read in spec file
	 * is correct...
	 */
	@Test
	public void reentryTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		JsonObject players = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.BACKGROUND_REENTRY_RATE, "0.0005");
		config.addProperty(Keys.MARKETMAKER_REENTRY_RATE, "0.05");
		
		JsonArray agents = new JsonArray();
		JsonElement agent1 = new JsonPrimitive("ZIR:" + Keys.BID_RANGE_MAX + "_100");
		JsonElement agent2 = new JsonPrimitive("ZIRP:" + Keys.BID_RANGE_MIN + "_10");
		agents.add(agent1);
		agents.add(agent2);
		JsonArray marketmaker = new JsonArray();
		JsonElement mm = new JsonPrimitive("FUNDAMENTALMM:" + Keys.SPREAD + "_256");
		marketmaker.add(mm);
		players.add("BACKGROUND", agents);
		players.add("MARKETMAKER", marketmaker);
		
		json.add(Keys.ASSIGN, players);
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		
		for (String role : spec.getPlayerProps().keySet()) {
			if (role.equals("BACKGROUND")) {
				for (AgentProperties ap : spec.getPlayerProps().get(role).elementSet()) {
					switch (ap.getAgentType()) {
					case ZIR:
						assertEquals(100, ap.getAsInt(Keys.BID_RANGE_MAX));
						assertEquals(0.0005, ap.getAsDouble(Keys.BACKGROUND_REENTRY_RATE), 1E-6);
						assertEquals(0.0005, ap.getAsDouble(Keys.BACKGROUND_REENTRY_RATE, Keys.REENTRY_RATE), 1E-6);
						break;
					case ZIRP:
						assertEquals(10, ap.getAsInt(Keys.BID_RANGE_MIN));
						assertEquals(0.0005, ap.getAsDouble(Keys.BACKGROUND_REENTRY_RATE), 1E-6);
						assertEquals(0.0005, ap.getAsDouble(Keys.BACKGROUND_REENTRY_RATE, Keys.REENTRY_RATE), 1E-6);
						break;
					default:
					}
				}
			} else if (role.equals("MARKETMAKER")) {
				for (AgentProperties ap : spec.getPlayerProps().get(role).elementSet()) {
					switch (ap.getAgentType()) {
					case FUNDAMENTALMM:
						assertEquals(256, ap.getAsInt(Keys.SPREAD));
						assertEquals(0.05, ap.getAsDouble(Keys.MARKETMAKER_REENTRY_RATE), 1E-6);
						assertEquals(0.05, ap.getAsDouble(Keys.MARKETMAKER_REENTRY_RATE, Keys.REENTRY_RATE), 1E-6);
						break;
					default:
					}
				}
			}
		}
	}
}
