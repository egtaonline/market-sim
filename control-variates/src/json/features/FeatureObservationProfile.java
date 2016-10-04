package json.features;

import java.util.List;

import com.google.common.collect.Lists;

import json.ObservationProfile;
import json.ProfileSymmetryGroup;
import json.SymmetryGroup;

/**
 * My own profiles that I'm creating with aggregated features (player-level).
 * 
 * @author ewah
 *
 */
public class FeatureObservationProfile extends ObservationProfile {

	int id;
	List<ProfileSymmetryGroup> symmetry_groups;
	List<FeatureObservation> observations;

	public FeatureObservationProfile(int id, List<ProfileSymmetryGroup> symmetry_groups,
			List<FeatureObservation> observations) {
		this.id = id;
		this.symmetry_groups = symmetry_groups;
		this.observations = observations;
	}

	public List<FeatureObservation> getObservations() {
		return observations;
	}

	public FeatureObservation getObservation(int index) {
		return observations.get(index);
	}

	@Override
	public int getNumObs() {
		return this.observations.size();
	}

	@Override
	public List<ProfileSymmetryGroup> getSymmetryGroups() {
		return symmetry_groups;
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public List<SymmetryGroup> getObservationSymmetryGroups(int index) {
		FeatureObservation obs = getObservation(index);
		List<SymmetryGroup> list = Lists.newArrayList();
		for (SymmetryGroup group : obs.symmetry_groups) {
			list.add(group);
		}
		return list;
	}

	@Override
	public double getObservationFeature(int index, String feature) {
		return getObservation(index).getFeature(feature);
	}

	/**
	 * Returns a K x N matrix of player-feature control variables (less the sample means)
	 * 
	 * K = # observations
	 * N = # player-level features (for only the specified symmetry group)
	 * 
	 * @param controlVariables		Feature variables (player-level)
	 * @param group
	 * @return
	 */
	public double[][] getFeatureControlVariables(String[] controlVariables,
			ProfileSymmetryGroup group) {
		int length = controlVariables.length;

		double[][] cvMatrix = new double[getNumObs()][length];
		for (int i = 0; i < getNumObs(); i++) {
			List<SymmetryGroup> groups = getObservationSymmetryGroups(i);
			for (int j = 0; j < length; j++) {
				for (SymmetryGroup symGroup : groups) {
					if (symGroup.getID() == group.getID()) {
						cvMatrix[i][j] = ((FeatureSymmetryGroup) symGroup).getFeature(controlVariables[j]);
					}
				}
			}
		}
		return cvMatrix;
	}

}
