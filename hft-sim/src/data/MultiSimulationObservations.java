package data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sumstats.SumStats;
import systemmanager.Keys;
import systemmanager.SimulationSpec;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Class that represents the combined observations of several simulations.
 * Contains a map from strings to summary statistics for random features, and a
 * list of MutiSimPlayerObservations for the player observations.
 * 
 * TODO Integrate this into Json's Default object parsers so that I don't have
 * to write manual parsing to json.
 * 
 * TODO: Clean up the dangling book keeping variables like numSims and
 * outputConfig
 * 
 * @author erik
 * 
 */
public class MultiSimulationObservations {

	protected static final Gson gson = new Gson();
	
	protected final List<MultiSimPlayerObservation> playerObservations;
	protected final Map<String, SumStats> features;
	protected SimulationSpec spec;
	protected final boolean outputConfig;
	protected final int totalSimulations;
	
	public MultiSimulationObservations(boolean outputConfig, int totalSimulations) {
		this.features = Maps.newHashMap();
		this.playerObservations = Lists.newArrayList();
		this.spec = null;
		this.outputConfig = outputConfig;
		this.totalSimulations = totalSimulations;
	}
	
	public void addObservation(Observations obs) {
		if (spec == null) { // First observation
			for (PlayerObservation po : obs.getPlayerObservations()) {
				playerObservations.add(new MultiSimPlayerObservation(po.role, 
						po.strategy, po.payoff, po.features));
			}
			for (Entry<String, Double> e : obs.getFeatures().entrySet()) {
				if (Double.isNaN(e.getValue())) {
					 continue;
				}
				SumStats sum = SumStats.create();
				sum.add(e.getValue());
				features.put(e.getKey(), sum);
			}
			spec = obs.spec;
		} else {
			/*
			 * XXX This is not super safe. It assumes that you will see the same
			 * features every time, and it assumes that the players are in the
			 * same order. Given the way the code is written, this should be the
			 * case, but it's worth noting.
			 */
			Iterator<PlayerObservation> it = obs.getPlayerObservations().iterator();
			for (MultiSimPlayerObservation mpo : playerObservations) {
				PlayerObservation playerObs = it.next();
				mpo.payoff.add(playerObs.payoff);
				// player-specific PV control variables
//				mpo.features.get(Keys.PV_BUY1).add(playerObs.features.get(Keys.PV_BUY1).doubleValue());
//				mpo.features.get(Keys.PV_SELL1).add(playerObs.features.get(Keys.PV_SELL1).doubleValue());
//				mpo.features.get(Keys.PV_POSITION1_MAX_ABS).add(playerObs.features.get(Keys.PV_POSITION1_MAX_ABS).doubleValue());
			}
			for (Entry<String, Double> e : obs.getFeatures().entrySet()) {
				if (Double.isNaN(e.getValue())) {
					 continue;
				}
			    // needed in case the new feature, "mean_median_spread_not_inf_nan",
			    // has been added by Observations.java.
			    if (!features.containsKey(e.getKey())) {
		             SumStats sum = SumStats.create();
	                 sum.add(e.getValue());
	                 features.put(e.getKey(), sum);
			    } else {
		             features.get(e.getKey()).add(e.getValue());
			    }
			}
		}
	}
	
	protected JsonElement toJson() {
		JsonObject root = new JsonObject();
		
		// Write out players
		JsonArray players = new JsonArray();
		root.add("players", players);
		for (MultiSimPlayerObservation mpo : playerObservations) {
			JsonObject obs = new JsonObject();
			players.add(obs);
			obs.addProperty("role", mpo.role);
			obs.addProperty("strategy", mpo.strategy);
			obs.addProperty("payoff", mpo.payoff.mean());
			
			// Record standard deviation for multi simulation
			JsonObject playerFeatures = new JsonObject();
			obs.add("features", playerFeatures);
			// Note NaNs are not handled in EGTAOnline
			if (Observations.VERBOSE_OUTPUT) {
    	         if (!Double.isNaN(mpo.payoff.stddev())) {
	                playerFeatures.addProperty("payoff_stddev", mpo.payoff.stddev());
	            }
	            // Record player-specific PV control variables
	            for (String property : mpo.features.keySet()) {
	                playerFeatures.addProperty(property, mpo.features.get(property).mean());
	            }
			}
		}
		
		// Write out features
		JsonObject feats = new JsonObject();
		root.add("features", feats);
		for (Entry<String, SumStats> e : features.entrySet()) {
			// TODO Ben's JsonParser doesn't handle nans or inf. This does.
			// Either make Ben's handle it, or make this handling better
			double mean = e.getValue().mean();
			if (Double.isInfinite(mean) || Double.isNaN(mean))
				feats.addProperty(e.getKey(), Double.toString(mean)); // FIXME EGTA won't handle strings, must be numeric
			else
				feats.addProperty(e.getKey(), mean);
		}
		
		// Add spec to config if necessary
		if (outputConfig)
			feats.add("config", spec.getRawSpec().get("configuration"));
		else // Else just carry over numSims
			feats.addProperty(Keys.NUM_SIMULATIONS, totalSimulations);
		
		return root;
	}
	
	public void writeToFile(File observationsFile) throws IOException {
		Writer writer = null;
		try {
			writer = new FileWriter(observationsFile);
			gson.toJson(toJson(), writer);
		} finally {
			if (writer != null) writer.close();
		}
	}

}
