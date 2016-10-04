package parser;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import controlvariates.ControlVariates;
import json.summary.SummaryProfile;
import json.summary.SummarySymmetryGroup;

/**
 * Note that it has to compute the total background trader surplus (which is
 * based on multiplying the count with the mean payoff)
 * 
 * @author ewah
 *
 */
public class SurplusComparisonProfile {

	SummaryProfile noOpProfile;		// stores the base no-op profile
	List<Integer> mmIDs;			// profile IDs for the cases with market makers
	List<SummarySymmetryGroup> backgroundGroups;

	int id;							// no op profile ID
	Map<String, Integer> strategyIDMap;	// hashed by strategy
	List<String> mmStrategies; 		// so know which row to put it in, or the index
	double backgroundSurplus;

	Map<String, Double> surplusWithMM;
	Map<String, Double> mmProfits;			// hashed by strat

	public int getID() {
		return id;
	}
	
	public double getBackgroundSurplus() {
		return backgroundSurplus;
	}
	
	public double getSurplusByStrategy(String mm) {
		return surplusWithMM.get(mm);
	}

	public double getProfitByStrategy(String mm) {
		return mmProfits.get(mm);
	}
	
	public SurplusComparisonProfile(SummaryProfile profile, List<String> marketmakerStrats) {
		this.noOpProfile = profile;
		this.mmStrategies = marketmakerStrats;
		mmIDs = Lists.newArrayList();
		strategyIDMap = Maps.newHashMap();
		id = noOpProfile.getID();

		backgroundGroups = Lists.newArrayList();
		for (SummarySymmetryGroup group : noOpProfile.getGroups()) {
			if (group.getRole().equals(ControlVariates.BACKGROUND));
			backgroundGroups.add(group);
		}
		backgroundSurplus = getBackgroundTotalSurplus(backgroundGroups);
 
		surplusWithMM = Maps.newHashMap();
		for (String key : mmStrategies) {
			surplusWithMM.put(key, 0.0);
		}
		mmProfits = Maps.newHashMap();
	}

	/**
	 * After getting this list of background symm groups what I need to do is to
	 * then verify that these match the input profile (with MM) 
	 * 
	 * @return
	 */
	private double getBackgroundTotalSurplus(List<SummarySymmetryGroup> groups) {
		double surplus = 0;
		for (SummarySymmetryGroup backgroundGroup : groups) {
			surplus += backgroundGroup.computeTotalPayoff(); 
		}
		return surplus;
	}


	/**
	 * Given a profile, if it's the same background trader profile, sticks it in the map
	 * for background surplus with MM
	 * 
	 * Assumes that the input is already checked to be equivalent to the noOpProfile
	 * 
	 * Then also stores the MM profit
	 * @param mmProfile
	 */
	public void processMarketMakerProfile(SummaryProfile mmProfile) {
		mmIDs.add(mmProfile.getID());
		double surplus = 0;
		String mmStrategy = null;
		for (SummarySymmetryGroup mmGroup : mmProfile.getGroups()) {
			if (mmGroup.getRole().equals(ControlVariates.BACKGROUND)) {
				surplus += mmGroup.computeTotalPayoff();
			}
			if (mmGroup.getRole().equals(ControlVariates.MARKETMAKER)) {
				mmStrategy = mmGroup.getStrategy();
				mmProfits.put(mmStrategy, mmGroup.getPayoff());
			}
		}
		surplusWithMM.put(mmStrategy, surplus);
		strategyIDMap.put(mmStrategy, mmProfile.getID());
	}
	
}
