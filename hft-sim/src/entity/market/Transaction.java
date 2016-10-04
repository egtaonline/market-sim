package entity.market;

import java.io.Serializable;

import entity.agent.Agent;
import event.TimeStamp;

/**
 * Base class for Transactions. Contains information on buyer/seller, market, quantity, price, and
 * time.
 * 
 * @author ewah
 */
public class Transaction implements Serializable {

	private static final long serialVersionUID = 8420827805792281642L;
	
	protected final Agent buyer;
	protected final Agent seller;
	protected final Market market;
	protected final Order buyOrder;
	protected final Order sellOrder;

	// Transaction Info
	protected final int quantity;
	protected final Price price;
	protected final TimeStamp execTime;

	public Transaction(Agent buyer, Agent seller, Market market,
			Order buyOrder, Order sellOrder, int quantity, Price price,
			TimeStamp execTime) {
		this.buyer = buyer;
		this.seller = seller;
		this.market = market;
		this.buyOrder = buyOrder;
		this.sellOrder = sellOrder;
		this.quantity = quantity;
		this.price = price;
		this.execTime = execTime;
	}

	public final Agent getBuyer() {
		return buyer;
	}

	public final Agent getSeller() {
		return seller;
	}

	public final Market getMarket() {
		return market;
	}

	public final Order getBuyOrder() {
		return buyOrder;
	}

	public final Order getSellOrder() {
		return sellOrder;
	}

	public final int getQuantity() {
		return quantity;
	}

	public final Price getPrice() {
		return price;
	}

	public final TimeStamp getExecTime() {
		return execTime;
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		// XXX this is only an approximation, doesn't handle case when both 
		// are at the same TimeStamp...
		if (buyOrder.getSubmitTime().before(sellOrder.getSubmitTime())) {
			return buyer + " bought " + quantity + " from " + seller + " @ " + price + " in " + market;
		}
		return seller + " sold " + quantity + " to " + buyer + " @ " + price + " in " + market;
	}
}
