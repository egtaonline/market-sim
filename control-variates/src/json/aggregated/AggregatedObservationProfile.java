package json.aggregated;

import java.util.List;

import com.google.common.collect.Lists;

import json.ObservationProfile;
import json.ProfileSymmetryGroup;
import json.SymmetryGroup;

/**
 * Profiles in the aggregated observations.
 * 
 * @author ewah
 *
 */
public class AggregatedObservationProfile extends ObservationProfile {

	int id;
	List<ProfileSymmetryGroup> symmetry_groups;
	List<AggregatedObservation> observations;

	public AggregatedObservationProfile(int id, List<ProfileSymmetryGroup> symmetry_groups,
			List<AggregatedObservation> observations) {
		this.id = id;
		this.symmetry_groups = symmetry_groups;
		this.observations = observations;
	}

	public List<AggregatedObservation> getObservations() {
		return observations;
	}
	
	public AggregatedObservation getObservation(int index) {
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
		AggregatedObservation obs = getObservation(index);
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
	
//	/**
//	 * Generates control variable matrix (EQ A6) for variance reduction.
//	 * Do not add the column of 1's here.
//	 * 
//	 * @param group
//	 * @param controlVariables
//	 * @return
//	 */
//	public double[][] getControlVariableMatrix(String[] controlVariables) {
//
//		int length = controlVariables.length;
//		double[] controlVarMeans = new double[length];
//		for (int j = 0; j < controlVarMeans.length; j++) {
//			for (int i = 0; i < getNumObs(); i++) {
//				controlVarMeans[j] += getObservation(i).getFeature(controlVariables[j]);
//			}
//			controlVarMeans[j] /= getNumObs();
//		}
//		
//		double[][] obsMatrix = new double[getNumObs()][controlVarMeans.length];
//		for (int i = 0; i < getNumObs(); i++) {
//			for (int j = 0; j < controlVarMeans.length; j++) {
//				obsMatrix[i][j] = getObservation(i).getFeature(controlVariables[j]) -
//						controlVarMeans[j];
//			}
//		}
//		return obsMatrix;
//	}
//
//
//	/**
//	 * Does it on per-profile, per-symmetry group basis (i.e. computes the 
//	 * payoff mean for each group, within each profile).
//	 * 
//	 * Averages the payoffs for each observation but does it by symmetry group.
//	 * 
//	 * @return
//	 */
//	public double[] getObservationAveragePayoffs(ProfileSymmetryGroup group) {
//		double[] payoffVector = new double[getNumObs()];
//		for (int i = 0; i < payoffVector.length; i++) {
//			List<ObservationSymmetryGroup> osg = getObservation(i).getSymmetryGroups();
//			// to get index to check, must verify the id with the group's id
//			for (ObservationSymmetryGroup obsGroup : osg) {
//				if (obsGroup.getID() == group.getID())
//					payoffVector[i] = obsGroup.getPayoff();
//			}
//		}
//		return payoffVector;
//	}
	

}
