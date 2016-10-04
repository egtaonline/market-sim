package entity.infoproc;

import java.io.Serializable;

import com.google.common.base.Objects;

import entity.market.Market;
import entity.market.Price;


/**
 * Data structure for holding best bid/ask quote (for updating NBBO)
 * 
 * @author ewah
 */
public class BestBidAsk implements Serializable {
	
	private static final long serialVersionUID = -7312167969610706296L;
	
	protected final Market bestBidMarket, bestAskMarket;
	protected final Price bestBid, bestAsk;
	protected final int bestBidQuantity, bestAskQuantity;

	public BestBidAsk(Market bestBidMarket, Price bestBid, int bestBidQuantity,
			Market bestAskMarket, Price bestAsk, int bestAskQuantity) {
		this.bestBidMarket = bestBidMarket;
		this.bestBid = bestBid;
		this.bestBidQuantity = bestBidQuantity;
		this.bestAskMarket = bestAskMarket;
		this.bestAsk = bestAsk;
		this.bestAskQuantity = bestAskQuantity;
	}

	/**
	 * @return bid-ask spread of the quote (double)
	 */
	public double getSpread() {
		if (bestAsk == null || bestBid == null || bestAsk.lessThan(bestBid))
			return Double.POSITIVE_INFINITY;
		return bestAsk.doubleValue() - bestBid.doubleValue();
	}

	public Market getBestBidMarket() {
		return bestBidMarket;
	}

	public Market getBestAskMarket() {
		return bestAskMarket;
	}

	public Price getBestBid() {
		return bestBid;
	}

	public Price getBestAsk() {
		return bestAsk;
	}
	
	public int getBestBidQuantity() {
		return bestBidQuantity;
	}

	public int getBestAskQuantity() {
		return bestAskQuantity;
	}
	

	@Override
	public int hashCode() {
		return Objects.hashCode(bestBidMarket, bestBid, bestBidQuantity, 
				bestAskMarket, bestAsk, bestAskQuantity);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof BestBidAsk))
			return false;
		BestBidAsk that = (BestBidAsk) obj;
		return Objects.equal(bestBidMarket, that.bestBidMarket)
				&& Objects.equal(bestBid, that.bestBid)
				&& Objects.equal(bestBidQuantity, that.bestBidQuantity)
				&& Objects.equal(bestAskMarket, that.bestAskMarket)
				&& Objects.equal(bestAsk, that.bestAsk)
				&& Objects.equal(bestAskQuantity, that.bestAskQuantity);
	}

	@Override
    public String toString() {
		StringBuilder sb = new StringBuilder("(BestBid: ");
		
		if (bestBid == null) sb.append("- ");
		else { 
			sb.append(bestBidQuantity).append(" @ ").append(bestBid);
			sb.append(" from ").append(bestBidMarket);
		}
		
		sb.append(", BestAsk: ");
		
		if (bestAsk == null) sb.append("- ");
		else {
			sb.append(bestAskQuantity).append(" @ ").append(bestAsk);
			sb.append(" from ").append(bestAskMarket);
		}
		
		return sb.append(')').toString();
	}

}