package data;

import java.io.Serializable;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class OrderDatum implements Serializable {

	private static final long serialVersionUID = 6379805806017545016L;

	private long orderRefNum;
	private long sequenceNum;
	private TimeStamp timestamp;
	private TimeStamp duration;
	private Price price; // 10 bytes in nyse
	private int quantity;//9 bytes in nyse
	private OrderType orderType;

	
	public OrderDatum(long orderRefNumber, TimeStamp timeStamp, Price price,
			int quantity, OrderType orderType) {
		this.orderRefNum = orderRefNumber;
		this.timestamp = timeStamp;
		this.price = price;
		this.quantity = quantity;
		this.orderType = orderType;
		this.duration = TimeStamp.IMMEDIATE;
	}
	
	public OrderDatum(long orderRefNumber, TimeStamp timeStamp, Price price,
			int quantity, OrderType orderType, TimeStamp duration) {
		this(orderRefNumber, timeStamp, price, quantity, orderType);
		this.duration = duration;
	}
	
	public OrderDatum(long orderRefNumber, TimeStamp timeStamp, Price price,
			int quantity, OrderType orderType, long sequenceNum) {
		this(orderRefNumber, timeStamp, price, quantity, orderType);
		this.sequenceNum = sequenceNum;
	}

	public long getSequenceNum() {
		return sequenceNum;
	}

	public long getOrderRefNum() {
		return orderRefNum;
	}

	public TimeStamp getTimeStamp() {
		return timestamp;
	}

	public Price getPrice() {
		return price;
	}

	public int getQuantity() {
		return quantity;
	}

	public OrderType getOrderType() {
		return orderType;
	}
	
	public TimeStamp getDuration() {
		return duration;
	}
	
	public void setDuration(TimeStamp duration) {
		this.duration = duration;
	}
}
