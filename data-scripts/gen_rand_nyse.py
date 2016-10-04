# to run:
#	python gen_rand_nyse.py [number_of_securities] [number_of_orders_to_print]
# only supports up to 100 securities

# NYSE supports the following orders
	# Add Order 
	# Modify Order 
	# Delete Order
	# Imbalance 
	# System Event

# examples:
	# A,1,12884402522,B,B,4900,AAIR,0.1046,28800,390,B,AARCA,
	# D,73,12884404105,30112,692,GRZG,B,B,AARCA,B,
	# M,85,12884403642,760,0.892,28800,531,GNIN,B,B,AARCA,B,

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

msg_type = ['A','M','D']
seq_num = 0
ord_ref_num = 0 
exchange_code = ['N','P','B']
buy_or_sell = ['B','S']
shares = 3000
stock_sym = 'SRG'
seconds=0
milliseconds=0
sys_code = ['L','O','E','B']
#total_imbalance = 1
#market_imbalance = 1
auction_type = ['O','M','H','C']
auction_time = 1

for x in range(0,numOrders):
	
	#incrementing the time
	next_msec = random.choice([True,False])
	if (next_msec):
		#reset seq_num
		seq_num = 0
		#incrementing milliseconds
		inc = random.randint(0, 100)
		milliseconds += inc 
		#incrementing seconds, if necessary
		if (milliseconds > 1000):
			seconds += 1
			milliseconds -= 1000
	else:
		seq_num += 1

  #incrementing reference number
	ord_ref_num += 1
	
	#generate random values for variables

	m = str(random.choice(msg_type))
	seq_num_str = str(seq_num)
	ref_num_str = str(ord_ref_num)
	exchange_code_str = random.choice(exchange_code)
	buy_str = random.choice(buy_or_sell)
	shares_str = str(random.randint(0,9999999))
	symbol_str = 'SRG' + str(random.randint(1,numSecurities))
	price_str = str(random.randint(1,9999999))
	sec_str = str(seconds)
	msec_str = str(milliseconds)
	sys_code_str = random.choice(sys_code)
	quote_id = "AARCA"
	filler = " "*8
	
	if (m == 'A'):
		order_str = m + ','
		order_str += seq_num_str + ','
		order_str += ref_num_str + ','
		order_str += exchange_code_str + ','
		order_str += buy_str + ','
		order_str += shares_str + ','
		order_str += symbol_str + ','
		order_str += price_str + ','
		order_str += sec_str + ','
		order_str += msec_str + ','
		order_str += sys_code_str + ','
		order_str += quote_id + ',' + filler
		print order_str

	if (m == 'M'):
		order_str = m + ','
		order_str += seq_num_str + ','
		order_str += ref_num_str + ','
		order_str += shares_str + ','
		order_str += price_str + ','
		order_str += sec_str + ','
		order_str += msec_str + ','
		order_str += symbol_str + ','
		order_str += exchange_code_str + ','
		order_str += sys_code_str + ','
		order_str += quote_id + ',' 
		order_str += buy_str + ',' + filler
		print order_str
	
	if (m == 'D'):
		order_str = m + ','
		order_str += seq_num_str + ','
		order_str += ref_num_str + ','
		order_str += sec_str + ','
		order_str += msec_str + ','
		order_str += symbol_str + ','
		order_str += exchange_code_str + ','
		order_str += sys_code_str + ','
		order_str += quote_id + ',' 
		order_str += buy_str + ',' + filler
		print order_str

#endfor
