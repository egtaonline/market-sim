package entity.market;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.MarketProperties;
import entity.infoproc.SIP;

public class MarketFactory {

	protected final Scheduler scheduler;
	protected final SIP sip;
	protected final Random rand;

	public MarketFactory(Scheduler scheduler, SIP sip, Random rand) {
		this.scheduler = scheduler;
		this.sip = sip;
		this.rand = rand;
	}

	public Market createMarket(MarketProperties props) {
		switch (props.getMarketType()) {
		case CDA:
			return new CDAMarket(scheduler, sip, new Random(rand.nextLong()), props);
		case CALL:
			if (props.getAsInt(Keys.CLEAR_INTERVAL) <= 0) {
				return new CDAMarket(scheduler, sip, new Random(rand.nextLong()), props);
			}

			return new CallMarket(scheduler, sip, new Random(rand.nextLong()), props);
		default:
			throw new IllegalArgumentException("Can't create MarketType: " + props.getMarketType());
		}
	}

}
