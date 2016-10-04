package controlvariates;

public class EmpiricalValue {

	double value;
	double stddev;
	int num;
	
	public EmpiricalValue(double payoff, double stddev, int num) {
		this.value = payoff;
		if (Double.isNaN(stddev)) stddev = Double.MAX_VALUE;
		this.stddev = stddev;
		this.num = num;
	}
	
	public double getValue() {
		return value;
	}
	
	public double getStandardDeviation() {
		return stddev;
	}
	
	public double getNum() {
		return num;
	}
}
