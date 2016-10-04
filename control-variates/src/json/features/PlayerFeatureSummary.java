package json.features;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.google.common.collect.Maps;

public class PlayerFeatureSummary {

	int sid;												// symmetry group id
	SummaryStatistics p = new SummaryStatistics();			// payoff
	Map<String, SummaryStatistics> f = Maps.newHashMap();	// features
	
	public Map<String, Double> getFeatures() {
		Map<String, Double> features = Maps.newHashMap();
		features.put("payoff", this.getPayoff());
		features.put("payoff_sd", this.getPayoffSD());
		if (!f.isEmpty())
			for (String key : f.keySet())
				features.put(key, f.get(key).getMean());
		
		return features;
	}
	
	public double getFeature(String key) {
		if (!f.containsKey(key)) return 0;
		return f.get(key).getMean();
	}
	
	public double getPayoff() {
		return p.getMean();
	}
	
	public double getPayoffSD() {
		return p.getStandardDeviation();
	}
	
	public int getSID() {
		return sid;
	}
	
	public void addFeature(String key, double value) {
		if (!f.containsKey(key)) 
			f.put(key, new SummaryStatistics());
		f.get(key).addValue(value);
	}
	
	public void addPayoff(double payoff) {
		p.addValue(payoff);
	}
}
