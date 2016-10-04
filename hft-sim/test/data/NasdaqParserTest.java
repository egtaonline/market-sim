package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.PeekingIterator;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class NasdaqParserTest {

	@Test
	public void parseAddOrderTest() {
		MarketDataParser parser = null;
		try {
			parser = new NasdaqParser("dataTests/nasdaqAddTest.csv");
		} catch (IOException e) {
			fail("error opening file");
			e.printStackTrace();
		}
		
		if (parser == null) {
		    throw new IllegalStateException();
		}
		
		PeekingIterator<OrderDatum> itr = parser.getIterator();
		
		assertTrue("No orders", itr.hasNext());
		
		OrderDatum orderDatum = itr.next();
		
		assertEquals("incorrect orderRefNum", 1, orderDatum.getOrderRefNum());
		assertEquals("incorrect timeStamp", 
				TimeStamp.create(16), orderDatum.getTimeStamp());
		assertEquals("incorrect duration", 
				TimeStamp.IMMEDIATE, orderDatum.getDuration());
		assertEquals("incorrect price", new Price(5630815), orderDatum.getPrice());
		assertEquals("incorrect quantity", 3748742, orderDatum.getQuantity());
		assertEquals("incorrect ordertype",
				OrderType.BUY, orderDatum.getOrderType());
		
		assertFalse("too many orders", itr.hasNext());
	}
	
	@Test
	public void parseDeleteOrderTest() {
		MarketDataParser parser = null;
		try {
			parser = new NasdaqParser("dataTests/nasdaqDeleteTest.csv");
		} catch (IOException e) {
			fail("error opening file");
			e.printStackTrace();
		}
	      
        if (parser == null) {
            throw new IllegalStateException();
        }
        
		PeekingIterator<OrderDatum> itr = parser.getIterator();
		
		assertTrue("No orders", itr.hasNext());
		
		OrderDatum orderDatum = itr.next();
		
		assertEquals("incorrect orderRefNum", 1, orderDatum.getOrderRefNum());
		assertEquals("incorrect timeStamp", 
				TimeStamp.create(16), orderDatum.getTimeStamp());
		assertEquals("incorrect duration", 
				TimeStamp.create(4), orderDatum.getDuration());
		assertEquals("incorrect price", new Price(5630815), orderDatum.getPrice());
		assertEquals("incorrect quantity", 3748742, orderDatum.getQuantity());
		assertEquals("incorrect ordertype",
				OrderType.BUY, orderDatum.getOrderType());
		
		assertFalse("too many orders", itr.hasNext());
	}

}
