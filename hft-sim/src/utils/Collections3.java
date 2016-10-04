package utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.Lists;

public class Collections3 {
	
	/**
	 * Returns a LIFO Queue backed by an ArrayList
	 * 
	 * @return
	 */
	public static <E> Queue<E> newArrayStack() {
		return asLifoQueue(Lists.<E> newArrayList());
	}

	/**
	 * Wrap any List in a LIFO Queue interface, such that items are appended and
	 * removed from the end of the list. This returns a view of the original
	 * list, so modifying the list will effect the queue.
	 * 
	 * @param backingList
	 * @return
	 */
	public static <E> Queue<E> asLifoQueue(List<E> backingList) {
		return new ListQueue<E>(backingList);
	}
	
	private static class ListQueue<E> extends AbstractQueue<E> {

		private List<E> backingList;
		
		private ListQueue(List<E> backingList) {
			this.backingList = checkNotNull(backingList);
		}
		
		@Override
		public boolean offer(E e) {
			return backingList.add(e);
		}

		@Override
		public E peek() {
			if (isEmpty()) return null;
			return backingList.get(size() - 1);
		}

		@Override
		public E poll() {
			if (isEmpty()) return null;
			return backingList.remove(size() - 1);
		}

		@Override
		public Iterator<E> iterator() {
			return Lists.reverse(backingList).iterator();
		}

		@Override
		public int size() {
			return backingList.size();
		}
		
	}
	
}
