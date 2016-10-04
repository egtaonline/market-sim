package json.features;

import java.util.List;
import java.util.Map;

import json.SymmetryGroup;

import com.google.common.collect.Lists;

import controlvariates.ControlVariates;

public class FeatureSymmetryGroup extends SymmetryGroup {

	int id;
	double payoff;
	double payoff_sd;
	double pv_buy1;
	double pv_sell1;
	double pv_position_max_abs1;

	public FeatureSymmetryGroup(int id, double buy, double sell, double position,
			double payoff, double payoff_sd) {
		this.id = id;
		this.pv_buy1 = buy;
		this.pv_sell1 = sell;
		this.pv_position_max_abs1 = position;
		this.payoff = payoff;
		this.payoff_sd = payoff_sd;
	}

	public FeatureSymmetryGroup(int id, Map<String, Double> summaryStats) {
		this.id = id;
		if (!summaryStats.isEmpty()) {
			if (summaryStats.containsKey(ControlVariates.PV_BUY1))
				this.pv_buy1 = summaryStats.get(ControlVariates.PV_BUY1);
			if (summaryStats.containsKey(ControlVariates.PV_SELL1))
				this.pv_sell1 = summaryStats.get(ControlVariates.PV_SELL1);
			if (summaryStats.containsKey(ControlVariates.PV_POSITION_MAX_ABS1))
				this.pv_position_max_abs1 = summaryStats.get(ControlVariates.PV_POSITION_MAX_ABS1);
			
			this.payoff = summaryStats.get("payoff");
			this.payoff_sd = summaryStats.get("payoff_sd");
		}
	}

	public static List<FeatureSymmetryGroup> createGroups(Map<Integer, PlayerFeatureSummary> map) {
		List<FeatureSymmetryGroup> symmetry_groups = Lists.newArrayList();
		for (int id : map.keySet()) {
			FeatureSymmetryGroup fsg = new FeatureSymmetryGroup(id, map.get(id).getFeatures());
			symmetry_groups.add(fsg);
		}
		return symmetry_groups;
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public double getPayoff() {
		return payoff;
	}

	@Override
	public double getPayoffSD() {
		return payoff_sd;
	}
	
	public double getBuyPV() {
		return pv_buy1;
	}
	
	public double getSellPV() {
		return pv_sell1;
	}
	
	public double getMaxPositionPV() {
		return pv_position_max_abs1;
	}
	
	public double getFeature(String key) {
		if (key.equals(ControlVariates.PV_BUY1))
			return getBuyPV();
		else if (key.equals(ControlVariates.PV_SELL1))
			return getSellPV();
		else if (key.equals(ControlVariates.PV_POSITION_MAX_ABS1))
			return getMaxPositionPV();
		return 0;
	}
}
