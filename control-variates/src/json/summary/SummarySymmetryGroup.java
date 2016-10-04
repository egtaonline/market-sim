package json.summary;

import json.ProfileSymmetryGroup;
import json.aggregated.ObservationSymmetryGroup;
import controlvariates.EmpiricalValue;

public class SummarySymmetryGroup {
	
	int count;
	String role;
	int id;
	String strategy;
	double payoff;
	double payoff_sd;
	
	public SummarySymmetryGroup(int count, String role, int id, String strategy,
			double payoff, double payoff_sd) {
		this.count = count;
		this.role = role;
		this.id = id;
		this.strategy = strategy;
		this.payoff = payoff;
		this.payoff_sd = payoff_sd;
	}
	
	public int getCount() {
		return count;
	}
	
	public String getRole() {
		return role;
	}
	
	public String getStrategy() {
		return strategy;
	}
	
	public int getID() {
		return id;
	}
	
	public double getPayoff() {
		return payoff;
	}
	
	public double getPayoffSD() {
		return payoff_sd;
	}
	
	public static SummarySymmetryGroup create(SummarySymmetryGroup group, 
			double payoff, double payoff_sd) {
		return new SummarySymmetryGroup(group.count, group.role, group.id,
				group.strategy, payoff, payoff_sd);
	}
	
	/**
	 * Given ObsSymGroup with the new payoff data (merged), then create the 
	 * SumSymGroup.
	 * 
	 * @param psg
	 * @param osg
	 * @return
	 */
	public static SummarySymmetryGroup create(ProfileSymmetryGroup psg, ObservationSymmetryGroup osg) {
		if (psg.getID() == osg.getID())
			return new SummarySymmetryGroup(psg.getCount(), psg.getRole(), psg.getID(), 
					psg.getStrategy(), osg.getPayoff(), osg.getPayoffSD());
		return null;
	}
	
	/**
	 * Creates new SummarySymmetryGroup with the params from the profile symmetry
	 * group and the new estimated value/stddev from value.
	 * 
	 * @param psg
	 * @param value
	 * @return
	 */
	public static SummarySymmetryGroup create(ProfileSymmetryGroup psg, 
			EmpiricalValue value) {
		return new SummarySymmetryGroup(psg.getCount(), psg.getRole(), psg.getID(), 
				psg.getStrategy(), value.getValue(), value.getStandardDeviation());
	}
	
	/**
	 * Computes count * payoff.
	 * @return
	 */
	public double computeTotalPayoff() {
		return count * payoff;
	}
}
