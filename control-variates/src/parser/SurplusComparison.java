package parser;

import java.util.List;

import json.summary.SummaryObservations;
import json.summary.SummaryProfile;

import com.google.common.collect.Lists;

/**
 * 
 * Holds surplus comparison metrics so it's easier to convert this eventually
 * to a csv.
 * 
 * It goes through all the profiles and stores it for each no MM profile.
 * It records the payoff and the std dev (in a column next to it)
 * It then goes through for each MM strat included here to give the background
 * surplus (TOTAL) with each profile with that MM (playing that strategy)
 * present.
 * 
 * profile (defined by symmetry groups of just background players), stored
 * in SurplusComparisonProfile
 * 
 * 	strats	profile1	(surplus)		profile1 (stddev)	profile1 (MM profit)
 * -------------------------------------------------------------
 *   [na]		[only background]		[only background]	[0]
 *   [MM strat]	[surplus with MM]		[std dev with MM]	[MM profit]
 *		...						...					...
 *	 (for all MM strats)
 * 
 * It gathers the available MM strats via the roles group at the very top.
 * 
 * @author ewah
 *
 */
public class SurplusComparison {

	// one of the first things to do is the parse the roles
	// and determine the order of MM strategies to use
	
	List<SurplusComparisonProfile> profiles = Lists.newArrayList();
	final List<String> mmStrategies;
	
	
	public SurplusComparison(List<String> strats) {
		mmStrategies = strats;
	}
	
	public void addProfile(SurplusComparisonProfile profile) {
		profiles.add(profile);
	}
	
	public List<String> getMMStrategies() {
		return mmStrategies;
	}
	
	public void testPrint() {
		String strat = mmStrategies.get(1);
		for (SurplusComparisonProfile p : profiles) {
			if (p.id == 423289) { // 423288
				System.out.println(p.id + ": " + strat + " " + p.strategyIDMap.get(strat));
				System.out.println(p.id + ": " + p.backgroundSurplus + " " + p.surplusWithMM.get(strat));
				
				System.out.println(p.id + ": " + 0 + " " + p.mmProfits.get(strat));
			}
		}
	}
	
	public void processNoMMProfiles(SummaryObservations noMMObs, SummaryObservations mmObs) {
		for (SummaryProfile profile : noMMObs.getProfiles()) {
			SurplusComparisonProfile comp = new SurplusComparisonProfile(profile, this.mmStrategies);
			
			for (SummaryProfile mmProfile : mmObs.getProfiles()) {
				if (SummaryProfile.compareSummaryProfileBackground(profile, mmProfile)) {
					comp.processMarketMakerProfile(mmProfile);
					mmProfile.getID();
				}
			}
			addProfile(comp);
		}
//		testPrint();
	}
}
