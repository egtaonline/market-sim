package entity.market;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import com.google.common.base.Objects;

import event.TimeStamp;

/**
 * Container for Quote data.
 * 
 * @author ewah
 */
public class Quote implements Serializable {

	private static final long serialVersionUID = 3842989596948215994L;
	
	protected final Price ask, bid;
	protected final int askQuantity, bidQuantity;
	protected final Market market;
	protected final TimeStamp quoteTime;

	public Quote(Market market, Price bid, int bidQuantity, Price ask,
			int askQuantity, TimeStamp currentTime) {
		this.market = market;
		this.ask = ask;
		this.askQuantity = askQuantity;
		this.bid = bid;
		this.bidQuantity = bidQuantity;
		this.quoteTime = currentTime;
	}

	public Price getAskPrice() {
		return ask;
	}

	public Price getBidPrice() {
		return bid;
	}
	
	public int getBidQuantity() {
		return bidQuantity;
	}
	
	public int getAskQuantity() {
		return askQuantity;
	}

	public Market getMarket() {
		return market;
	}
	
	public TimeStamp getQuoteTime() {
		return quoteTime;
	}
	
	/**
	 * @return true if the quote is defined (has an ask and a bid price)
	 */
	public boolean isDefined() {
		return ask != null && bid != null;
	}

	/**
	 * @return bid-ask spread of the quote
	 */
	public double getSpread() {
		// XXX Are these the best way to handle these cases?
		if (ask == null || bid == null)
			return Double.POSITIVE_INFINITY;
		checkArgument(ask.greaterThanEqual(bid), "%s::quote: ERROR bid > ask", market);
		return ask.doubleValue() - bid.doubleValue();
	}
	
	public double getMidquote() {
		// XXX Are these the best way to handle these cases?
		if (ask == null || bid == null)
			return Double.NaN;
		checkArgument(ask.greaterThanEqual(bid), "%s::quote: ERROR bid > ask", market);
		return (ask.doubleValue() + bid.doubleValue())/ 2;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(market, ask, askQuantity, bid, bidQuantity, quoteTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Quote))
			return false;
		Quote that = (Quote) obj;
		return Objects.equal(market, that.market)
				&& Objects.equal(ask, that.ask)
				&& Objects.equal(bid, that.bid)
				&& Objects.equal(quoteTime, that.quoteTime)
				&& askQuantity == that.askQuantity
				&& bidQuantity == that.bidQuantity;
	}

	@Override
    public String toString() {
		StringBuilder sb = new StringBuilder("(Bid: ");
		
		if (bid == null) sb.append("- ");
		else sb.append(bidQuantity).append(" @ ").append(bid);
		
		sb.append(", Ask: ");
		
		if (ask == null) sb.append("- ");
		else sb.append(askQuantity).append(" @ ").append(ask);
		
		return sb.append(')').toString();
	}

}
