package data;

import java.util.List;
import java.util.Map;

import systemmanager.SimulationSpec;

import com.google.common.collect.ImmutableList;

import entity.agent.Agent;
import entity.market.Market;

public class MockObservations extends Observations {

	private final List<PlayerObservation> playerObservations;
	private final Map<String, Double> features;
	
	public MockObservations(List<PlayerObservation> playerObservations, Map<String, Double> features) {
		super(new SimulationSpec(),
				ImmutableList.<Market> of(),
				ImmutableList.<Agent> of(),
				ImmutableList.<Player> of(),
				new MockFundamental(10000));
		this.playerObservations = playerObservations;
		this.features = features;
	}

	@Override
	public List<PlayerObservation> getPlayerObservations() {
		return playerObservations;
	}

	@Override
	public Map<String, Double> getFeatures() {
		return features;
	}

}
