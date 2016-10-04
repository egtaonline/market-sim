package data;

import java.util.Map;

/**
 * The observation of a single player. It's generated in the player class.
 * 
 * @author erik
 * 
 */
public class PlayerObservation {

	public final String role;
	public final String strategy;
	public final double payoff;
	public final Map<String, Double> features;
	
	public PlayerObservation(String role, String strategy, double utility,
			Map<String, Double> features) {
		this.role = role;
		this.strategy = strategy;
		this.payoff = utility;
		this.features = features;
	}
}
