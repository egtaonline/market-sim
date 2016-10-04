package fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

/**
 * An order meant for use in a fourheap
 * 
 * @author ebrink
 * 
 * @param <BS>
 * 			  OrderType
 * @param <P>
 *            Price
 * @param <T>
 *            Time
 */
public class Order<P extends Comparable<? super P>, T extends Comparable<? super T>> implements Serializable {

	private static final long serialVersionUID = -3460176014871040729L;
	
	public enum OrderType { BUY, SELL };
	
	protected final OrderType type;
	protected final P price;
	protected int unmatchedQuantity, matchedQuantity; // Always positive
	protected final T submitTime;

	protected Order(OrderType type, P price, int initialQuantity, T submitTime) {
		checkArgument(initialQuantity > 0, "Initial quantity must be positive");
		this.price = checkNotNull(price, "Price");
		this.unmatchedQuantity = initialQuantity;
		this.matchedQuantity = 0;
		this.type = checkNotNull(type);
		this.submitTime = checkNotNull(submitTime, "Submit Time");
	}
	
	/**
	 * Factory constructor
	 */
	public static <P extends Comparable<? super P>, T extends Comparable<? super T>> Order<P, T> create(
			OrderType type, P price, int initialQuantity, T submitTime) {
		return new Order<P, T>(type, price, initialQuantity, submitTime);
	}

	public OrderType getOrderType() {
		return type;
	}
	
	/**
	 * Get the Price
	 * 
	 * @return The price
	 */
	public P getPrice() {
		return price;
	}

	/**
	 * Get the quantity. Always positive.
	 * 
	 * @return The quantity
	 */
	public int getQuantity() {
		return unmatchedQuantity + matchedQuantity;
	}

	/**
	 * Get the submission Time of the order
	 * 
	 * @return The submission Time
	 */
	public T getSubmitTime() {
		return submitTime;
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	/**
	 * All Orders are unique
	 */
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "<" + submitTime + "| " + type + " " + getQuantity() + " @ " + price + ">";
	}
	
}
