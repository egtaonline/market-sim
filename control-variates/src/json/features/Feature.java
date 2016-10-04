package json.features;

import controlvariates.ControlVariates;

public class Feature {

	double control_mean_fund;
	double control_var_fund;
	double control_mean_private;
	
	public Feature(double mean_fund, double var_fund, double mean_private) {
		this.control_mean_fund = mean_fund;
		this.control_var_fund = var_fund;
		this.control_mean_private = mean_private;
	}
	
	public double get(String key) {
		if (key.equals(ControlVariates.CONTROL_MEAN_FUND))
			return control_mean_fund;
		else if (key.equals(ControlVariates.CONTROL_VAR_FUND))
			return control_var_fund;
		else if (key.equals(ControlVariates.CONTROL_MEAN_PRIVATE))
			return control_mean_private;
		
		return 0;
	}
}
