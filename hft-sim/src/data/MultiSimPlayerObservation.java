package data;

import java.util.Map;

import sumstats.SumStats;

import com.google.common.collect.Maps;

/**
 * This class represents the observation of a player over several simulations.
 * 
 * @author erik
 * 
 */
public class MultiSimPlayerObservation {

	public final String role;
	public final String strategy;
	public final SumStats payoff;
	public final Map<String, SumStats> features;
	
	public MultiSimPlayerObservation(String role, String strategy) {
		this.role = role;
		this.strategy = strategy;
		this.payoff = SumStats.create();
		this.features = Maps.newHashMap();
	}
	
	public MultiSimPlayerObservation(String role, String strategy, double firstValue,
			Map<String, Double> features) {
		this(role, strategy);
		payoff.add(firstValue);
		for (String key : features.keySet()) {
			SumStats summ = SumStats.create();
			summ.add(features.get(key).doubleValue());
			this.features.put(key, summ);
		}
	}
	
}
