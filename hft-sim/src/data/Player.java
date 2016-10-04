package data;

import java.io.Serializable;

import entity.agent.Agent;

/**
 * A class that represents a player. It contains a role, strategy, and a
 * reference to the player.
 * 
 * It is accessed by getting a player observation. This calls the
 * player.getPayoff function which can be overridden to return the correct thing
 * for different agent types.
 * 
 * @author erik
 * 
 */
public class Player implements Serializable {
	
	private static final long serialVersionUID = 7233996258432288503L;
	
	protected final String role;
	protected final String strategy;
	protected final Agent agent;

	public Player(String role, String strategy, Agent agent) {
		this.role = role;
		this.strategy = strategy;
		this.agent = agent;
	}

	public Agent getAgent() {
		return agent;
	}
	
	public PlayerObservation getObservation() {
		return new PlayerObservation(role, strategy, agent.getPayoff(), agent.getFeatures());
	}
}
