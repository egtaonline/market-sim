package systemmanager;

import static systemmanager.Consts.AgentType.LA;
import static systemmanager.Consts.AgentType.MAXEFFICIENCY;
import static systemmanager.Consts.MarketType.CALL;
import static systemmanager.Consts.MarketType.CDA;
import static systemmanager.Keys.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Consts.Presets;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import data.AgentProperties;
import data.EntityProperties;
import data.MarketProperties;

/**
 * Stores list of web parameters used in EGTAOnline.
 * 
 * NOTE: All MarketModel types in the spec file must match the corresponding
 * class name.
 * 
 * @author ewah
 */
public class SimulationSpec implements Serializable {

	private static final long serialVersionUID = 5646083286397841102L;
	protected static final Splitter split = Splitter.on(';');
	protected static final Gson gson = new Gson();
	
	protected static final String[] simulationKeys = { 
			SIMULATION_LENGTH,
			FUNDAMENTAL_MEAN, 
			FUNDAMENTAL_KAPPA, 
			FUNDAMENTAL_SHOCK_VAR,
			FUNDAMENTAL_SHOCK_PROB,
			RAND_SEED, 
			NBBO_LATENCY, 
			NUM_SIMULATIONS, 
			MAX_POSITION
		};
	protected static final String[] marketKeys = { 
			MARKET_LATENCY,
			MARKET_TICK_SIZE,
			TICK_SIZE };
	protected static final String[] agentKeys = {
			AGENT_TICK_SIZE,
			TICK_SIZE, 
			REENTRY_RATE,
			BACKGROUND_REENTRY_RATE,
			MARKETMAKER_REENTRY_RATE,
			PRIVATE_VALUE_VAR, 
			SIMULATION_LENGTH, 
			FUNDAMENTAL_KAPPA, 
			FUNDAMENTAL_MEAN,
			FUNDAMENTAL_SHOCK_VAR,
			MAX_POSITION,
			BMM_SHADE_TICKS,
			BMM_INVENTORY_FACTOR,
			PROB_FUND_AGENT,
			NOISE_STDEV,
			WITHDRAW_ORDERS,
			BID_RANGE_MIN,
			BID_RANGE_MAX,
			RUNG_SIZE,
			TICK_IMPROVEMENT,
			TICK_OUTSIDE,
			INITIAL_LADDER_MEAN,
			INITIAL_LADDER_RANGE
		};

	protected transient final JsonObject rawSpec;
	protected final EntityProperties simulationProperties;

	protected final Collection<MarketProperties> marketProps;
	protected final Collection<AgentProperties> agentProps;
	
	// This probably makes more sense as a Multimap, but I couldn't make it as efficient
	protected final Map<String, Multiset<AgentProperties>> playerProps; 

	public SimulationSpec() {
		this.rawSpec = new JsonObject();
		this.simulationProperties = EntityProperties.empty();

		this.marketProps = ImmutableList.of();
		this.agentProps = ImmutableList.of();
		this.playerProps = ImmutableMap.of();
	}
	
	public SimulationSpec(File specFile) throws FileNotFoundException {
		this(new FileReader(specFile));
	}
	
	public SimulationSpec(Reader reader) {
		rawSpec = gson.fromJson(reader, JsonObject.class);
		JsonObject config = rawSpec.getAsJsonObject(Keys.CONFIG);
		JsonObject players = rawSpec.getAsJsonObject(Keys.ASSIGN);

		handlePresets(config);
		
		simulationProperties = readProperties(config, simulationKeys);
		
		EntityProperties defaultMarketProperties = readProperties(config, marketKeys);
		marketProps = markets(config, defaultMarketProperties);
		
		EntityProperties defaultAgentProperties = readProperties(config, agentKeys);
		agentProps = agents(config, defaultAgentProperties);
		playerProps = players(players == null ? new JsonObject() : players, defaultAgentProperties);
	}

	public SimulationSpec(String specFileName) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		this(new File(specFileName));
	}

	protected static EntityProperties readProperties(JsonObject config,
			String... keys) {
		EntityProperties props = EntityProperties.empty();
		for (String key : keys) {
			JsonPrimitive value = config.getAsJsonPrimitive(key);
			if (value == null) continue;
			props.put(key, value.getAsString());
		}
		return props;
	}

	protected Collection<MarketProperties> markets(
			JsonObject config, EntityProperties def) {
		Builder<MarketProperties> markets = ImmutableList.builder();

		for (MarketType marketType : MarketType.values()) {
			JsonPrimitive configJson = config.getAsJsonPrimitive(marketType.toString());
			if (configJson == null) continue;
			for (String marketConfig : split.split(configJson.getAsString()))
				markets.add(MarketProperties.create(marketType, def, marketConfig));
		}
		return markets.build();
	}

	protected Collection<AgentProperties> agents(JsonObject config,
			EntityProperties def) {
		Builder<AgentProperties> environmentAgents = ImmutableList.builder();

		for (AgentType agentType : Consts.AgentType.values()) {
			JsonPrimitive configJson = config.getAsJsonPrimitive(agentType.toString());
			if (configJson == null) continue;
			for (String agentConfig : split.split(configJson.getAsString()))
				environmentAgents.add(AgentProperties.create(agentType, def, agentConfig));
		}
		return environmentAgents.build();
	}
	
	protected Map<String, Multiset<AgentProperties>> players(JsonObject config, EntityProperties defaults) {
		ImmutableMap.Builder<String, Multiset<AgentProperties>> mapBuilder = ImmutableMap.builder();
		for (Entry<String, JsonElement> e : config.entrySet()) {
			ImmutableMultiset.Builder<AgentProperties> multisetBuilder = ImmutableMultiset.builder();
			for (JsonElement stratString : e.getValue().getAsJsonArray())
				multisetBuilder.add(AgentProperties.fromConfigString(stratString.getAsString(), defaults));
			mapBuilder.put(e.getKey(), multisetBuilder.build());
		}
		return mapBuilder.build();
	}
	
	/**
	 * Set preset for standard simulations
	 */
	// Just add a new case to add your own!
	protected void handlePresets(JsonObject config) {
		JsonPrimitive preset = config.getAsJsonPrimitive(Keys.PRESETS);
		if (preset == null) return;
		if (preset.getAsString().isEmpty()) return;
		switch(Presets.valueOf(preset.getAsString())) {
		case NONE:
			break;
		case TWOMARKET:
			config.addProperty(CDA.toString(), NUM + "_2");
			config.addProperty(CALL.toString(), NUM + "_0");
			config.addProperty(LA.toString(), NUM + "_0");
			break;
		case TWOMARKETLA:
			config.addProperty(CDA.toString(), NUM + "_2");
			config.addProperty(CALL.toString(), NUM + "_0");
			config.addProperty(LA.toString(), NUM + "_1");
			break;
		case CENTRALCDA:
			config.addProperty(CDA.toString(), NUM + "_1");
			config.addProperty(CALL.toString(), NUM + "_0");
			config.addProperty(LA.toString(), NUM + "_0");
			break;
		case CENTRALCALL:
			int nbboLatency = config.getAsJsonPrimitive(NBBO_LATENCY).getAsInt();
			config.addProperty(CDA.toString(), NUM + "_0");
			config.addProperty(CALL.toString(), NUM + "_1_" + CLEAR_INTERVAL + "_" + nbboLatency);
			config.addProperty(LA.toString(), NUM + "_0");
			break;
		case MAXEFF:
			int maxPosition = config.getAsJsonPrimitive(MAX_POSITION).getAsInt();
			config.addProperty(SIMULATION_LENGTH, "12");
			config.addProperty(CDA.toString(), NUM + "_0");
			config.addProperty(CALL.toString(), NUM + "_1_" + CLEAR_INTERVAL + "_10");
			config.addProperty(MAXEFFICIENCY.toString(), NUM + "_66_" + Keys.MAX_POSITION + "_" + maxPosition);
			break;
		default:
			// Should be impossible to reach here
			throw new IllegalArgumentException("Unknown Preset");
		}
		
	}

	public EntityProperties getSimulationProps() {
		return simulationProperties;
	}

	public Collection<MarketProperties> getMarketProps() {
		return ImmutableList.copyOf(marketProps);
	}

	public Collection<AgentProperties> getAgentProps() {
		return ImmutableList.copyOf(agentProps);
	}

	public Map<String, Multiset<AgentProperties>> getPlayerProps() {
		return playerProps;
	}
	
	public JsonObject getRawSpec() {
		return rawSpec;
	}
	
	@Override
	public String toString() {
		return rawSpec.toString();
	}
}
