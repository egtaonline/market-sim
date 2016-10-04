package json;

import java.util.List;

public abstract class ObservationProfile {

	public abstract List<ProfileSymmetryGroup> getSymmetryGroups();
	
	public abstract int getNumObs();
	
	public abstract int getID();
	
	public abstract List<SymmetryGroup> getObservationSymmetryGroups(int index);
	
	public abstract double getObservationFeature(int index, String feature);
	
	/**
	 * Generates control variable matrix (EQ A6) for variance reduction.
	 * Do not add the column of 1's here.
	 * 
	 * Note this is for general features, not player-level features.
	 * The matrix is the SAME for each symmetry group in an observation.
	 * 
	 * @param group
	 * @param controlVariables
	 * @return
	 */
	public double[][] getControlVariableMatrix(String[] controlVariables) {

		int length = controlVariables.length;
		double[] controlVarMeans = new double[length];
		for (int j = 0; j < controlVarMeans.length; j++) {
			for (int i = 0; i < getNumObs(); i++) {
				controlVarMeans[j] += getObservationFeature(i, controlVariables[j]);
			}
			controlVarMeans[j] /= getNumObs();
		}
		
		double[][] cvMatrix = new double[getNumObs()][controlVarMeans.length];
		for (int i = 0; i < getNumObs(); i++) {
			for (int j = 0; j < controlVarMeans.length; j++) {
				cvMatrix[i][j] = getObservationFeature(i, controlVariables[j]) -
						controlVarMeans[j];
			}
		}
		return cvMatrix;
	}
	
	/**
	 * Does not subtract the means here.
	 * 
	 * @param controlVariables
	 * @return
	 */
	public double[][] getControlVariables(String[] controlVariables) {
		int length = controlVariables.length;
		double[][] cvMatrix = new double[getNumObs()][length];
		for (int i = 0; i < getNumObs(); i++) {
			for (int j = 0; j < length; j++) {
				cvMatrix[i][j] = getObservationFeature(i, controlVariables[j]);
			}
		}
		return cvMatrix;
	}
	
	/**
	 * Does it on per-profile, per-symmetry group basis.
	 * @return
	 */
	public double[] getObservationPayoffs(ProfileSymmetryGroup group) {
		double[] payoffVector = new double[getNumObs()];
		for (int i = 0; i < payoffVector.length; i++) {
			List<SymmetryGroup> groups = getObservationSymmetryGroups(i);
			// to get index to check, must verify the id with the group's id
			for (SymmetryGroup symGroup : groups) {
				if (symGroup.getID() == group.getID())
					payoffVector[i] = symGroup.getPayoff();
			}
		}
		return payoffVector;
	}
	
}
