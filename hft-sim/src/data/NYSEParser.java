package data;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;
//import java.nio.file.Paths;
//import java.util.Scanner;

public class NYSEParser extends MarketDataParser {

	public NYSEParser(String pathName) throws IOException {
		super(pathName);
	}

	@Override
	public PeekingIterator<OrderDatum> getIterator() {

		// XXX - can optimize for big data sets by buffering
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			List<String> elements = Lists.newArrayList(line.split(","));
		
			char messageType = elements.get(0).charAt(0);
			switch (messageType) {
			case 'A':
				this.orderDatumList.add(parseAddOrder(elements));
				 break;
			case 'D':
				parseDeleteOrder(elements);
				break;
			default:
				break;
			}
		}
		scanner.close();
		
		return Iterators.peekingIterator(orderDatumList.iterator());
	}

	private static OrderDatum parseAddOrder(List<String> elements) {
		// Need to check number of columns
		long orderReferenceNumber = new Long(elements.get(2));
		OrderType type = (elements.get(4).charAt(0) == 'B') ? BUY : SELL;
		int quantity = new Integer(elements.get(5));
		Price price = new Price(new Double(elements.get(7)));
		long seconds = new Long(elements.get(8));
		long milliseconds = new Long(elements.get(9));
		TimeStamp timestamp = TimeStamp.create(seconds*1000 + milliseconds);
		
		return new OrderDatum(orderReferenceNumber, timestamp, price, quantity, 
				type, TimeStamp.IMMEDIATE);
	}

	private void parseDeleteOrder(List<String> elements) {
		long orderRefNum = new Long(elements.get(2));
		int seconds = new Integer(elements.get(3));
		int milliseconds = new Integer(elements.get(4));
		
		// So we can reverse search the orders
		Iterator<OrderDatum> itr = Lists.reverse(orderDatumList).iterator();
		
		while (itr.hasNext()) {
			OrderDatum orderDatum = itr.next();
			
			// Check if the order reference numbers match
			if (orderDatum.getOrderRefNum() == orderRefNum) {
				TimeStamp deleteTime = TimeStamp.create(seconds * 1000 + milliseconds);
				TimeStamp duration = deleteTime.minus(orderDatum.getTimeStamp());
				orderDatum.setDuration(duration);
				break;
			}
		}
	}
	
}
