package json.summary;

import java.util.ArrayList;
import java.util.List;

import json.aggregated.AggregatedObservationProfile;
import json.features.FeatureObservationProfile;

public class SummaryProfile {
	
	int id;
	int observations_count;
	List<SummarySymmetryGroup> symmetry_groups;
	
	public SummaryProfile(int id, int observations_count, 
			List<SummarySymmetryGroup> symmetry_groups) {
		this.id = id;
		this.symmetry_groups = symmetry_groups;
		this.observations_count = observations_count;
	}

	public static SummaryProfile create(AggregatedObservationProfile profile) {
		return new SummaryProfile(profile.getID(), profile.getObservations().size(),
				new ArrayList<SummarySymmetryGroup>());
	}
	
	public static SummaryProfile create(FeatureObservationProfile profile) {
		return new SummaryProfile(profile.getID(), profile.getObservations().size(),
				new ArrayList<SummarySymmetryGroup>());
	}

	public static SummaryProfile create(SummaryProfile profile) {
		return new SummaryProfile(profile.id, profile.observations_count,
				new ArrayList<SummarySymmetryGroup>());
	}
	
	public void addGroup(SummarySymmetryGroup group) {
		symmetry_groups.add(group);
	}
	
	public int getID() {
		return id;
	}
	
	public List<SummarySymmetryGroup> getGroups() {
		return symmetry_groups;
	}
	
	public SummarySymmetryGroup findGroupByID(int id) {
		for (SummarySymmetryGroup group : symmetry_groups) {
			if (group.getID() == id) {
				return group;
			}
		}
		return null;
	}
	
	/**
	 * Determines if two profiles have the same background symm groups.
	 * 
	 * @param profile1
	 * @param profile2
	 * @return
	 */
	public static boolean compareSummaryProfileBackground(SummaryProfile profile1, SummaryProfile profile2) {
		if (profile1.symmetry_groups.size() != profile2.symmetry_groups.size()) return false;
		
		for (SummarySymmetryGroup group1 : profile1.symmetry_groups) {
			for (SummarySymmetryGroup group2 : profile2.symmetry_groups) {
				if (checkEquivalentBackground(group1, group2))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * checks if group2 contains same background role symmetry groups
	 * in terms of count & strategy as group1
	 * if so, returns true
	 * 
	 * @param group1
	 * @param group2
	 * @return
	 */
	public static boolean checkEquivalentBackground(SummarySymmetryGroup group1,
			SummarySymmetryGroup group2) {
		boolean equivalent = true;
		equivalent &= group1.getCount() == group2.getCount();
		equivalent &= group1.getStrategy().equals(group2.getStrategy());
		equivalent &= group1.getRole().equals(group2.getRole());
		return equivalent;
	}
}
