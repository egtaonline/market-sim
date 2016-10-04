package entity.market;

import event.TimeStamp;

public class DummyMarketTime extends MarketTime {

	private static final long serialVersionUID = 1L;
	
	public DummyMarketTime(TimeStamp time, long marketTime) {
		super(time, marketTime);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof MarketTime)) return false;
		MarketTime other = (MarketTime) obj;
		return ticks == other.getInTicks() && marketTime == other.marketTime;
	}
}
