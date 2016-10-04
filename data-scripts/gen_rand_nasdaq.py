# to run:
#	python gen_rand_nasdaq.py [number_of_orders_to_print]



# NASDAQ supports the following orders
	# Add Order 
	# Modify Order 
	# Delete Order
	# Imbalance 
	# System Event


import random
import sys
import pprint

# Checking for input
if (len(sys.argv) != 3):
	print "Incorrect # of arguments"
	print "Usage: python gen_rand_nyse.py [number_of_securities] [number_of_orders_to_print]"
	sys.exit(1)

numSecurities = int(sys.argv[1])
numOrders = int(sys.argv[2])

msg_type = ['A','F','E','C','X','D','U'];
nanoseconds = 0;
ord_ref_num = 0; 				# can be upped from 1 (not really important)
buy_or_sell = ['B','S'];
stock_sym = 'SRG';
price =1;
seconds=0;
printable = ['N', 'Y'];


for x in range(0,numOrders):

	#initial time message
	if x == 0:
		time_msg = "T," + str(seconds)
		print time_msg

	#incrementing the time
	nanoseconds += random.randint(0, (10**8));
	if (nanoseconds > 10**9):
		nanoseconds -= 10**9
		seconds += 1
		#Printing the time message
		time_msg = "T," + str(seconds)
		print time_msg


	ord_ref_num += 1

	#generate random values for variables
	m = str(random.choice(msg_type))
	ref_num_str = str(ord_ref_num)
	buy_str = random.choice(buy_or_sell)
	shares_str = str(random.randint(0,9999999))
	symbol_str = 'SRG' + str(random.randint(1,numSecurities))
	price_str = str(random.randint(1,9999999))
	sec_str = str(seconds)
	quote_id = "AARCA"
	filler = " "*8
	pick_printable = random.choice(printable);
	time = str(nanoseconds)

	#makes sequential data

	if (m == 'A' or m == 'F'): #F needs a market identifier
		order_str = m + ','
		order_str += time + ','
		order_str += ref_num_str + ','
		order_str += buy_str + ','
		order_str += shares_str + ','
		order_str += symbol_str + ','
		order_str += price_str
		print order_str

	if (m == 'E' or m == 'X'): #E needs match number
		order_str = m + ','
		order_str += time + ','
		order_str += ref_num_str + ','
		order_str += shares_str
		print order_str
	
	if (m == 'U'): #new order reference number
		order_str = m + ','
		order_str += time + ','
		order_str += ref_num_str + ','
		order_str += shares_str + ','
		order_str += price_str
		print order_str


	if (m == 'C'):
		order_str = m + ','
		order_str += time + ','
		order_str += ref_num_str + ','
		order_str += shares_str + ','
		order_str += pick_printable + ','
		order_str += price_str

	if(m == 'D'):
		order_str = m + ','
		order_str += time + ','
		order_str += ref_num_str
		print order_str

#endfor
