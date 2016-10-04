package data;

import entity.market.Price;
import event.TimeStamp;

public class OrderData {
	private TimeStamp timestamp;
	private Price price;
	private int quantity;
	boolean isBuy;
	
	public TimeStamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(TimeStamp timestamp) {
		this.timestamp = timestamp;
	}

	public Price getPrice() {
		return price;
	}

	public void setPrice(Price price) {
		this.price = price;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public boolean isBuy() {
		return isBuy;
	}

	public void setBuy(boolean isBuy) {
		this.isBuy = isBuy;
	}

	public OrderData(TimeStamp timestamp, Price price, int quantity, boolean isBuy) {
		this.timestamp = timestamp;
		this.price = price;
		this.quantity = quantity;
		this.isBuy = isBuy;
	}
}
