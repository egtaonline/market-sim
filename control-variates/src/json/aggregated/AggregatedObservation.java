package json.aggregated;

import java.util.List;

import com.google.gson.JsonObject;

public class AggregatedObservation {

	List<ObservationSymmetryGroup> symmetry_groups;
	JsonObject features;

	public double getFeature(String featureKey) {
		return features.get(featureKey).getAsDouble();
	}
	
	public List<ObservationSymmetryGroup> getSymmetryGroups() {
		return symmetry_groups;
	}
}
