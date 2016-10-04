package fourheap;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Random;

import org.junit.Test;

import com.google.common.collect.Iterables;

import fourheap.Order.OrderType;

public class FourHeapTest {
	
	protected final static Random rand = new Random();
	
	@Test
	public void heapOrderTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		Order<Integer, Integer> b1, b2, b3, s1, s2, s3;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		
		b1 = Order.create(BUY, 5, 3, 5);
		b2 = Order.create(BUY, 10, 3, 5);
		b3 = Order.create(BUY, 5, 3, 4);
		
		fh.buyUnmatched.offer(b1);
		fh.buyUnmatched.offer(b2);
		assertEquals(b2, fh.buyUnmatched.poll());
		fh.buyUnmatched.offer(b3);
		assertEquals(b3, fh.buyUnmatched.poll());
		
		fh.buyMatched.offer(b1);
		fh.buyMatched.offer(b2);
		assertEquals(fh.buyMatched.peek(), b1);
		fh.buyMatched.offer(b3);
		assertEquals(fh.buyMatched.poll(), b3);
		
		s1 = Order.create(SELL, 5, 3, 5);
		s2 = Order.create(SELL, 10, 3, 5);
		s3 = Order.create(SELL, 5, 3, 4);
		
		fh.sellUnmatched.offer(s1);
		fh.sellUnmatched.offer(s2);
		assertEquals(fh.sellUnmatched.peek(), s1);
		fh.sellUnmatched.offer(s3);
		assertEquals(fh.sellUnmatched.poll(), s3);
		
		fh.sellMatched.offer(s1);
		fh.sellMatched.offer(s2);
		assertEquals(fh.sellMatched.poll(), s2);
		fh.sellMatched.offer(s3);
		assertEquals(fh.sellMatched.poll(), s3);
	}
	
	@Test
	public void insertOneBuyTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, BUY, 5, 3, 0);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(3, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void insertOneSellTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 5, 3, 0);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(3, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void matchTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 5, 3, 0);
		insertOrder(fh, BUY, 7, 3, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(6, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 5, 5, 0);
		insertOrder(fh, BUY, 7, 3, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(8, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 5, 3, 0);
		insertOrder(fh, BUY, 7, 5, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(8, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void insertMatchedTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 5, 3, 0);
		insertOrder(fh, BUY, 7, 3, 1);
		insertOrder(fh, BUY, 4, 1, 2);
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 5, 3, 0);
		insertOrder(fh, BUY, 7, 3, 1);
		insertOrder(fh, BUY, 6, 1, 2);
		assertEquals((Integer) 6, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertInvariants(fh);

		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 5, 3, 0);
		insertOrder(fh, BUY, 7, 3, 1);
		insertOrder(fh, BUY, 8, 1, 2);
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void withdrawOneBuyTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		Order<Integer, Integer> o = insertOrder(fh, BUY, 5, 3, 0);
		fh.withdrawOrder(o, 2);
		
		assertEquals(1, o.unmatchedQuantity);
		assertTrue(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(1, fh.size());
		assertInvariants(fh);
		
		fh.withdrawOrder(o);
		
		assertFalse(fh.contains(o));
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(0, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void withdrawOneSellTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		Order<Integer, Integer> o = insertOrder(fh, SELL, 5, 3, 0);
		fh.withdrawOrder(o, 2);
		
		assertEquals(1, o.unmatchedQuantity);
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(1, fh.size());
		assertInvariants(fh);
		
		fh.withdrawOrder(o);
		
		assertFalse(fh.contains(o));
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(0, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void withdrawMatchTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		Order<Integer, Integer> os, ob;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		os = insertOrder(fh, SELL, 5, 3, 0);
		ob = insertOrder(fh, BUY, 7, 3, 1);
		fh.withdrawOrder(ob, 2);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(4, fh.size());
		assertInvariants(fh);
		
		fh.withdrawOrder(os);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(1, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		ob = insertOrder(fh, BUY, 7, 3, 1);
		os = insertOrder(fh, SELL, 5, 5, 0);
		fh.withdrawOrder(os, 3);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(5, fh.size());
		assertInvariants(fh);
		
		fh.withdrawOrder(ob);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(2, fh.size());
		assertInvariants(fh);

		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		os = insertOrder(fh, SELL, 5, 3, 0);
		ob = insertOrder(fh, BUY, 7, 5, 1);
		fh.withdrawOrder(ob, 4);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(4, fh.size());
		assertInvariants(fh);
	}
	
	/**
	 * Test that withdrawing with orders waiting to get matched actually works
	 * appropriately
	 */
	@Test
	public void withdrawWithWaitingOrders() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		Order<Integer, Integer> o;

		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		o = insertOrder(fh, BUY, 4, 3, 0);
		insertOrder(fh, SELL, 1, 3, 1);
		insertOrder(fh, SELL, 2, 2, 2);
		insertOrder(fh, BUY, 3, 4, 3);
		assertInvariants(fh);
		fh.withdrawOrder(o);
		assertInvariants(fh);
		fh.clear();
		assertInvariants(fh);

		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		o = insertOrder(fh, SELL, 1, 3, 0);
		insertOrder(fh, BUY, 4, 3, 1);
		insertOrder(fh, BUY, 3, 2, 2);
		insertOrder(fh, SELL, 2, 4, 3);
		assertInvariants(fh);
		fh.withdrawOrder(o);
		assertInvariants(fh);
		fh.clear();
		assertInvariants(fh);
	}
	
	/**
	 * Test a strange edge case with withdrawing orders, where quantity may get
	 * misinterpreted.
	 */
	@Test
	public void strangeWithdrawEdgeCase() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		Order<Integer, Integer> o;

		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, BUY, 4, 3, 0);
		o = insertOrder(fh, SELL, 1, 3, 1);
		insertOrder(fh, SELL, 2, 2, 2);
		insertOrder(fh, BUY, 3, 4, 3);
		assertInvariants(fh);
		fh.withdrawOrder(o);
		assertInvariants(fh);
		fh.clear();
		assertInvariants(fh);

		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 1, 3, 0);
		o = insertOrder(fh, BUY, 4, 3, 1);
		insertOrder(fh, BUY, 3, 2, 2);
		insertOrder(fh, SELL, 2, 4, 3);
		assertInvariants(fh);
		fh.withdrawOrder(o);
		assertInvariants(fh);
		fh.clear();
		assertInvariants(fh);
	}
	
	@Test
	public void emptyClearTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 7, 3, 0);
		insertOrder(fh, BUY, 5, 3, 1);
		assertTrue(fh.clear().isEmpty());
	}
	
	@Test
	public void clearTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		Order<Integer, Integer> os, ob;
		Collection<MatchedOrders<Integer, Integer, Order<Integer, Integer>>> transactions;
		MatchedOrders<Integer, Integer, Order<Integer, Integer>> trans;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		os = insertOrder(fh, SELL, 5, 2, 0);
		ob = insertOrder(fh, BUY, 7, 3, 1);
		transactions = fh.clear();
		
		assertEquals(1, transactions.size());
		trans = Iterables.getOnlyElement(transactions);
		assertEquals(os, trans.getSell());
		assertEquals(ob, trans.getBuy());
		assertEquals(2, trans.getQuantity());
		assertEquals(1, ob.unmatchedQuantity);
		assertEquals(1, fh.size());
		assertFalse(fh.contains(os));
		assertTrue(fh.contains(ob));
		assertInvariants(fh);
	}
	
	@Test
	public void multiOrderClearTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		Order<Integer, Integer> os, ob;
		Collection<MatchedOrders<Integer, Integer, Order<Integer, Integer>>> transactions;
//		MatchedOrders<Integer, Integer, Order<Integer, Integer>> trans;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		os = insertOrder(fh, SELL, 5, 3, 0);
		insertOrder(fh, SELL, 6, 2, 0);
		ob = insertOrder(fh, BUY, 7, 4, 1);
		transactions = fh.clear();
		
		assertEquals(2, transactions.size());
		assertInvariants(fh);
		assertEquals(1, fh.size());
		assertFalse(fh.contains(os));
		assertFalse(fh.contains(ob));
		
		boolean one = false, three = false;
		for (MatchedOrders<Integer, Integer, Order<Integer, Integer>> trans : transactions) {
			switch (trans.getQuantity()) {
			case 1:
				assertEquals(ob, trans.getBuy());
				assertNotEquals(os, trans.getSell());
				one = true;
				break;
			case 3:
				assertEquals(os, trans.getSell());
				assertEquals(ob, trans.getBuy());
				three = true;
				break;
			default:
				fail("Incorrect transaction quantities");
			}
		}
		assertTrue(one);
		assertTrue(three);
	}
	
	@Test
	public void containsTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		
		Order<Integer, Integer> os, ob;
		ob = insertOrder(fh, BUY, 5, 1, 0);
		assertTrue(fh.contains(ob));
		os = insertOrder(fh, SELL, 6, 1, 0);
		assertTrue(fh.contains(os));
		os = insertOrder(fh, SELL, 4, 1, 1);
		// Verify that sell order @ 4 which has matched is still in FH
		assertTrue(fh.contains(os)); 
		assertTrue(fh.contains(ob));		
	}
	
	@Test
	public void matchedQuoteTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		
		// Test when only matched orders in order book
		insertOrder(fh, BUY, 10, 1, 0);
		insertOrder(fh, SELL, 5, 1, 0);
		// BID=max{max(matched sells), max(unmatched buys)}
		// ASK=min{min(matched buys), min(unmatched sells)}
		assertEquals(new Integer(5), fh.bidQuote());
		assertEquals(new Integer(10), fh.askQuote());
	}
	
	@Test
	public void askQuoteTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		
		assertEquals(null, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		
		// Test when no matched orders
		insertOrder(fh, SELL, 10, 1, 0);
		assertEquals(new Integer(10), fh.askQuote());
		assertEquals(null, fh.bidQuote());
		insertOrder(fh, BUY, 5, 1, 0);
		assertEquals(new Integer(5), fh.bidQuote());
		
		// Test when some orders matched
		// BID=max{max(matched sells), max(unmatched buys)}	-> max(10, 5)
		// ASK=min{min(matched buys), min(unmatched sells)} -> min(15, -)
		insertOrder(fh, BUY, 15, 1, 0);
		assertEquals(new Integer(15), fh.askQuote());	// the matched buy at 15
		assertEquals(new Integer(10), fh.bidQuote());
		
		// Now orders in each container in FH
		insertOrder(fh, SELL, 20, 1, 0);
		assertEquals(new Integer(10), fh.bidQuote());	// max(10, 5)
		assertEquals(new Integer(15), fh.askQuote());	// min(15, 20)
		
	}

	
	@Test
	public void bidQuoteTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		
		assertEquals(null, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		
		// Test when no matched orders
		insertOrder(fh, BUY, 15, 1, 0);
		assertEquals(new Integer(15), fh.bidQuote());
		assertEquals(null, fh.askQuote());
		insertOrder(fh, SELL, 20, 1, 0);
		assertEquals(new Integer(20), fh.askQuote());
		
		// Test when some orders matched
		// BID=max{max(matched sells), max(unmatched buys)} -> max(10, -)
		// ASK=min{min(matched buys), min(unmatched sells)} -> min(15, 20)
		insertOrder(fh, SELL, 10, 1, 0);
		assertEquals(new Integer(10), fh.bidQuote()); // the matched sell at 10
		assertEquals(new Integer(15), fh.askQuote());
		
		// Now orders in each container in FH
		insertOrder(fh, BUY, 5, 1, 0);
		assertEquals(new Integer(10), fh.bidQuote()); 	// max(10, 5)
		assertEquals(new Integer(15), fh.askQuote());	// min(15, 20)
	}
	
	@Test
	public void specificInvariantTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh;
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, BUY, 2, 1, 0);
		insertOrder(fh, SELL, 1, 1, 1);
		insertOrder(fh, SELL, 4, 1, 2);
		insertOrder(fh, BUY, 3, 1, 3);
		insertOrder(fh, BUY, 5, 1, 4);
		
		assertInvariants(fh);
		
		fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		insertOrder(fh, SELL, 4, 1, 0);
		insertOrder(fh, BUY, 5, 1, 1);
		insertOrder(fh, BUY, 2, 1, 2);
		insertOrder(fh, SELL, 3, 1, 3);
		insertOrder(fh, SELL, 1, 1, 4);
		
		assertInvariants(fh);
	}
	
	@Test
	public void quoteInvariantTest() {
		FourHeap<Integer, Integer, Order<Integer, Integer>> fh = FourHeap.<Integer, Integer, Order<Integer, Integer>> create();
		for (int i = 0; i < 1000; i++) {
			insertOrder(fh, rand.nextBoolean() ? BUY : SELL,
					rand.nextInt(900000) + 100000, 1, i);
			assertInvariants(fh);
		}
	}
	
	@Test
	public void repeatedInvarianceTest() {
		for (int i = 0; i < 100; i++)
			quoteInvariantTest();
	}
	
	protected static Order<Integer, Integer> insertOrder(
			FourHeap<Integer, Integer, Order<Integer, Integer>> fh, OrderType type, int price, int quantity, int time) {
		Order<Integer, Integer> order = Order.create(type, price, quantity, time);
		fh.insertOrder(order);
		return order;
	}
	
	protected static int matchedSize(PriorityQueue<Order<Integer, Integer>> bh) {
		int size = 0;
		for (Order<Integer, Integer> so : bh)
			size += so.matchedQuantity;
		return size;
	}
	
	protected static void assertInvariants(FourHeap<Integer, Integer, Order<Integer, Integer>> fh) {
		Order<Integer, Integer> bi, bo, si, so;
		Integer bid, ask;
		
		bi = fh.buyMatched.peek();
		bo = fh.buyUnmatched.peek();
		si = fh.sellMatched.peek();
		so = fh.sellUnmatched.peek();
		bid = fh.bidQuote();
		ask = fh.askQuote();
		
		assertTrue(bi == null || bo == null || bi.price >= bo.price);
		assertTrue(so == null || si == null || so.price >= si.price);
		assertTrue(so == null || bo == null || so.price >= bo.price);
		assertTrue(bi == null || si == null || bi.price >= si.price);
		assertTrue(bid == null || ask == null || bid <= ask);
		assertEquals(matchedSize(fh.sellMatched), matchedSize(fh.buyMatched));
	}

}
