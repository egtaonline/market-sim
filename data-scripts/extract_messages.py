# arguments: (in csv file: string) (out csv file: string) (ticker: string) (all or a/m/d: bool) (all times: bool)
# example: infile.csv outfile.csv AAA 1
import sys

if __name__ == '__main__':
	ticker = sys.argv[3]
	allTypes = bool(int(sys.argv[4]))
	allTimes = bool(int(sys.argv[5]))
	if (allTimes == 0):
		startTime = int(raw_input("Enter Start Time as seconds after midnight: "))
		endTime = int(raw_input("Enter End Time as seconds after midnight: "))
	numRows = 0
	allTimeStamps = []
	numTies = 0

with open(sys.argv[1]) as ifile, open(sys.argv[2], mode = 'w') as ofile:
	for row in ifile: 
		terms = [term for term in row.split(',') if term.strip() != '']
		# if row corresponds to designated ticker (multiple checks because haven't filtered on message type yet)
		if (terms[2] == ticker or terms[5] == ticker or terms[6] == ticker or terms[7] == ticker):
			if (allTypes == 1):
				# create current timestamp based on message type
				if (terms[0] == "A"):
					currentTimeStamp = float(terms[8]) + (float(terms[9]) / 1000.0)
				elif (terms[0] == "M"):
					currentTimeStamp = float(terms[5]) + (float(terms[6]) / 1000.0)
				elif (terms[0] == "D"):
					currentTimeStamp = float(terms[3]) + (float(terms[4]) / 1000.0)
				elif (terms[0] == "I"):
					currentTimeStamp = float(terms[6]) + (float(terms[7]) / 1000.0)
				else: # SE has same message type as imbalance (????)
					pass
				# no designated time range
				if (allTimes == 1):
					# check for both unordered messages and ties
					if (numRows > 0):
						if (currentTimeStamp < allTimeStamps[-1]):
							sys.exit("Error: Messages Not Ordered Chronologically")
						elif (currentTimeStamp == allTimeStamps[-1]):
							numTies += 1
					numRows += 1
					allTimeStamps.append(currentTimeStamp)
					ofile.write(row)
				# designated time range
				else:
					# before time range
					if (currentTimeStamp < startTime):
						continue
					# after time range
					elif (currentTimeStamp > endTime):
						break
					# in time range
					else: 
						# check for both unordered messages and ties
						if (numRows > 0):
							if (currentTimeStamp < allTimeStamps[-1]):
								sys.exit("Error: Messages Not Ordered Chronologically")
							elif (currentTimeStamp == allTimeStamps[-1]):
								numTies += 1
						numRows += 1
						allTimeStamps.append(currentTimeStamp)
						ofile.write(row)
			else:
				# create current timestamp based on message type
				if (terms[0] == "A"):
					currentTimeStamp = float(terms[8]) + (float(terms[9]) / 1000.0)
				elif (terms[0] == "M"):
					currentTimeStamp = float(terms[5]) + (float(terms[6]) / 1000.0)
				elif (terms[0] == "D"):
					currentTimeStamp = float(terms[3]) + (float(terms[4]) / 1000.0)
				else:
					continue
				# no designated time range
				if (allTimes == 1):
					# check for both unordered messages and ties
					if (numRows > 0):
						if (currentTimeStamp < allTimeStamps[-1]):
							sys.exit("Error: Messages Not Ordered Chronologically")
						elif (currentTimeStamp == allTimeStamps[-1]):
							numTies += 1
					numRows += 1
					allTimeStamps.append(currentTimeStamp)
					ofile.write(row)
				# designated time range
				else:
					# before time range
					if (currentTimeStamp < startTime):
						continue
					# after time range
					elif (currentTimeStamp > endTime):
						break
					# in time range
					else: 
						# check for both unordered messages and ties
						if (numRows > 0):
							if (currentTimeStamp < allTimeStamps[-1]):
								sys.exit("Error: Messages Not Ordered Chronologically")
							elif (currentTimeStamp == allTimeStamps[-1]):
								numTies += 1
						numRows += 1
						allTimeStamps.append(currentTimeStamp)
						ofile.write(row)

		else: 
			continue
print "Number of rows: ", numRows
print "Number of ties: ", numTies