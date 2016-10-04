package json.features;

import java.util.List;

public class FeatureObservation {

	List<FeatureSymmetryGroup> symmetry_groups;
	Feature features;
	
	public double getFeature(String featureKey) {
		return features.get(featureKey);
	}
	
	public FeatureObservation(Feature features, List<FeatureSymmetryGroup> symmetry_groups) {
		this.features = features;
		this.symmetry_groups = symmetry_groups;
	}
	
	public List<FeatureSymmetryGroup> getSymmetryGroups() {
		return symmetry_groups;
	}
}
