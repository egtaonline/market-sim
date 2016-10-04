package utils;

import com.google.common.base.Objects;

/**
 * Simple pair implementation
 * 
 * @author drhurd
 * 
 * @param <A>
 * @param <B>
 */
public abstract class Pair<A, B> {

	protected A left;
	protected B right;

	protected Pair(A left, B right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(left, right);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Pair)) return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		return Objects.equal(left, other.left) && Objects.equal(right, other.right);
	}

	@Override
	public String toString() {
		return "<" + left + ", " + right + ">";
	}

}
