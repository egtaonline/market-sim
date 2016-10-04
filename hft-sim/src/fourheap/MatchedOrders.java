package fourheap;

import java.io.Serializable;

public class MatchedOrders<P extends Comparable<? super P>, T extends Comparable<? super T>, O extends Order<? extends P, ? extends T>> implements Serializable {

	private static final long serialVersionUID = -6073835626927361670L;
	
	protected final O buy;
	protected final O sell;
	protected final int quantity;
	
	protected MatchedOrders(O buy, O sell, int quantity) {
		this.buy = buy;
		this.sell = sell;
		this.quantity = quantity;
	}

	public static <P extends Comparable<? super P>, T extends Comparable<? super T>, O extends Order<? extends P, ? extends T>> MatchedOrders<P, T, O> create(
			O buy, O sell, int quantity) {
		return new MatchedOrders<P, T, O>(buy, sell, quantity);
	}

	public O getBuy() {
		return buy;
	}

	public O getSell() {
		return sell;
	}

	public int getQuantity() {
		return quantity;
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
		return "<buy=" + buy + ", sell=" + sell + ", quantity="
				+ quantity + ">";
	}

}
