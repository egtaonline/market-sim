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

public class NYSEParserTest {

	@Test
	public void parseAddOrderTest() {
		NYSEParser parser = null;
		try {
			parser = new NYSEParser("dataTests/nyseSimpleTest.csv");
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
				TimeStamp.create(88), orderDatum.getTimeStamp());
		assertEquals("incorrect duration", 
				TimeStamp.IMMEDIATE, orderDatum.getDuration());
		assertEquals("incorrect price", new Price(3249052), orderDatum.getPrice());
		assertEquals("incorrect quantity", 5742346, orderDatum.getQuantity());
		assertEquals("incorrect ordertype",
				OrderType.BUY, orderDatum.getOrderType());
		
		assertFalse("too many orders", itr.hasNext());
	}
	
	@Test
	public void parseDeleteOrderTest() {
		NYSEParser parser = null;
		try {
			parser = new NYSEParser("dataTests/nyseDeleteTest.csv");
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
				TimeStamp.create(0), orderDatum.getTimeStamp());
		assertEquals("incorrect duration", 
				TimeStamp.create(2000), orderDatum.getDuration());
		assertEquals("incorrect price", new Price(5516081), orderDatum.getPrice());
		assertEquals("incorrect quantity", 981477, orderDatum.getQuantity());
		assertEquals("incorrect ordertype",
				OrderType.BUY, orderDatum.getOrderType());
		
		assertFalse("too many orders", itr.hasNext());
	}

}
