package json.aggregated;

import json.SymmetryGroup;

public class ObservationSymmetryGroup extends SymmetryGroup {
	
	int id;
	double payoff;
	double payoff_sd;
	
	public ObservationSymmetryGroup(int id, double payoff_sd, double payoff) {
		this.id = id;
		this.payoff = payoff;
		this.payoff_sd = payoff_sd;
	}
	
	@Override
	public double getPayoff() {
		return payoff;
	}
	
	@Override
	public double getPayoffSD() {
		return payoff_sd;
	}
	
	@Override
	public int getID() {
		return id;
	}
}
