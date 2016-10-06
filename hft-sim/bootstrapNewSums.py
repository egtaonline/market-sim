
import sys
import os.path
import json
import math
import numpy as np
import random

spread = []
time = []
trade = []
mmTrade = []
welfare = []
bgSurplus = []
mmProfit = []
spreadIfDefined = []
rms = []
minShade = []
frac = []
inventory = []
spreadsEarned = []
spreadProfit = []
positioningProfit = []

spreadString = "spreads_mean_markets"
timeString = "exectime_mean"
tradeString = "trans_num"
mmTradeString = "trans_fundamentalmarketmaker_num"
bgSurplusString = "surplus_sum_no_disc"
mmProfitString = "profit_sum_marketmaker"
spreadIfDefinedString = "mean_median_spread_not_inf_nan"
rmsString = "mean_rms_midquote_error_vs_estim_rt"
minShadeString = "mean_min_shade_vs_estim_rt"
fracString = "mean_fraction_estim_rt_in_spread"
inventoryString = "sum_absv_inventory_mm"
spreadsEarnedString = "total_spreads_earned_mm"
spreadProfitString = "sum_spread_profit_mm"
positioningProfitString = "sum_positioning_profit_mm"

myOutputStrings = ["spread","spread_L","spread_H","executionTime","executionTime_L","executionTime_H","numTrades","numTrades_L","numTrades_H","mmTrades","mmTrades_L","mmTrades_H","welfare","welfare_L","welfare_H","bgSurplus","bgSurplus_L","bgSurplus_H","mmProfit","mmProfit_L","mmProfit_H","spreadIfDef","spreadIfDef_L","spreadIfDef_H","midquoteRmsVsEst","midquoteRmsVsEst_L","midquoteRmsVsEst_H","minShade","minShade_L","minShade_H","fracEstInSpread","fracEstInSpread_L","fracEstInSpread_H","mmInventory","mmInventory_L","mmInventory_H","mmSpreadsEarned","mmSpreadsEarned_L","mmSpreadsEarned_H","mmSpreadProfit","mmSpreadProfit_L","mmSpreadProfit_H","mmPositioningProfit","mmPositioningProfit_L","mmPositioningProfit_H"]

myOutput = []

featureString = "features"

bootstrapSamples = 10000

observationFileString = "observation"

def getValuesFromFolder(folderName):
    for path, subdirs, files in os.walk(folderName):
        for fileName in files:
            if not observationFileString in fileName:
                continue
            fileName = os.path.join(path, fileName)
            jsonObs = ''
            with open(fileName) as obsFile:
                jsonObs = json.load(obsFile)

            spread.append(jsonObs[featureString][spreadString])
            tempTime = jsonObs[featureString][timeString]
            if not math.isnan(float(tempTime)):
                time.append(tempTime)
                pass

            trade.append(jsonObs[featureString][tradeString])
            if mmTradeString in jsonObs[featureString]:
                mmTrade.append(jsonObs[featureString][mmTradeString])
            else:
                mmTrade.append(0.0)
            welfare.append(jsonObs[featureString][bgSurplusString] + jsonObs[featureString][mmProfitString])
            bgSurplus.append(jsonObs[featureString][bgSurplusString])
            mmProfit.append(jsonObs[featureString][mmProfitString])
            spreadIfDefined.append(jsonObs[featureString][spreadIfDefinedString])
            rms.append(jsonObs[featureString][rmsString])
            minShade.append(jsonObs[featureString][minShadeString])
            frac.append(jsonObs[featureString][fracString])
            inventory.append(jsonObs[featureString][inventoryString])
            spreadsEarned.append(jsonObs[featureString][spreadsEarnedString])
            spreadProfit.append(jsonObs[featureString][spreadProfitString])
            positioningProfit.append(jsonObs[featureString][positioningProfitString])
            pass
        pass
    pass

def printMeans():
    myPlaces = 3
    print ""
    print "mean spread: " + str(round(np.mean(spread), myPlaces))
    print "mean execution time: " + str(round(np.mean(time), myPlaces))
    print "mean num trades: " + str(round(np.mean(trade), myPlaces))
    print "mean num MM trades: " + str(round(np.mean(mmTrade), myPlaces))
    print "mean social welfare: " + str(round(np.mean(welfare), myPlaces))
    print "mean bg trader surplus: " + str(round(np.mean(bgSurplus), myPlaces))
    print "mean MM profit: " + str(round(np.mean(mmProfit), myPlaces))
    print "mean spread where defined: " + str(round(np.mean(spreadIfDefined), myPlaces))
    print "mean rms midquote vs estimate: " + str(round(np.mean(rms), myPlaces))
    print "mean min shade from estimate: " + str(round(np.mean(minShade), myPlaces))
    print "mean fraction estimate in spread: " + str(round(np.mean(frac), myPlaces))
    print "mean sum absval MM inventory: " + str(round(np.mean(inventory), myPlaces))
    print "mean total MM spreads earned: " + str(round(np.mean(spreadsEarned), myPlaces))
    print "mean sum MM spread profit: " + str(round(np.mean(spreadProfit), myPlaces))
    print "mean sum MM positioning profit: " + str(round(np.mean(positioningProfit), myPlaces))
    pass

def printSds():
    myPlaces = 3
    print ""
    print "stdev spread: " + str(round(np.std(spread), myPlaces))
    print "stdev execution time: " + str(round(np.std(time), myPlaces))
    print "stdev num trades: " + str(round(np.std(trade), myPlaces))
    print "stdev num MM trades: " + str(round(np.std(mmTrade), myPlaces))
    print "stdev social welfare: " + str(round(np.std(welfare), myPlaces))
    print "stdev bg trader surplus: " + str(round(np.std(bgSurplus), myPlaces))
    print "stdev MM profit: " + str(round(np.std(mmProfit), myPlaces))
    print "stdev spread where defined: " + str(round(np.std(spreadIfDefined), myPlaces))
    print "stdev rms midquote vs estimate: " + str(round(np.std(rms), myPlaces))
    print "stdev min shade from estimate: " + str(round(np.std(minShade), myPlaces))
    print "stdev fraction estimate in spread: " + str(round(np.std(frac), myPlaces))
    print "stdev sum absval MM inventory: " + str(round(np.std(inventory), myPlaces))
    print "stdev total MM spreads earned: " + str(round(np.std(spreadsEarned), myPlaces))
    print "stdev sum MM spread profit: " + str(round(np.std(spreadProfit), myPlaces))
    print "stdev sum MM positioning profit: " + str(round(np.std(positioningProfit), myPlaces))
    pass

# print 95% confidence interval for each mean:
# that is, 2.5th percentile and 97.5th percentile for each mean
def printBootstrapMeanIntervals(mmCount):
    myPlaces = 3
    print ""
    spreadCI = printBootstrapMeanInterval(spread, "mean spread")
    myOutput.append(round(np.mean(spread), myPlaces))
    myOutput.append(spreadCI[0])
    myOutput.append(spreadCI[1])

    timeCI = printBootstrapMeanInterval(time, "mean execution time")
    myOutput.append(round(np.mean(time), myPlaces))
    myOutput.append(timeCI[0])
    myOutput.append(timeCI[1])

    tradesCI = printBootstrapMeanInterval(trade, "mean num trades")
    myOutput.append(round(np.mean(trade), myPlaces))
    myOutput.append(tradesCI[0])
    myOutput.append(tradesCI[1])

    mmTradesCI = printBootstrapMeanInterval(mmTrade, "mean num MM trades")
    myOutput.append(round(np.mean(mmTrade), myPlaces))
    myOutput.append(mmTradesCI[0])
    myOutput.append(mmTradesCI[1])

    welfareCI = printBootstrapMeanInterval(welfare, "mean social welfare")
    myOutput.append(round(np.mean(welfare), myPlaces))
    myOutput.append(welfareCI[0])
    myOutput.append(welfareCI[1])

    bgSurplusCI = printBootstrapMeanInterval(bgSurplus, "mean bg trader surplus")
    myOutput.append(round(np.mean(bgSurplus), myPlaces))
    myOutput.append(bgSurplusCI[0])
    myOutput.append(bgSurplusCI[1])

    mmProfitCI = printBootstrapMeanInterval(mmProfit, "mean MM profit")
    myOutput.append(round(np.mean(mmProfit), myPlaces))
    myOutput.append(mmProfitCI[0])
    myOutput.append(mmProfitCI[1])

    spreadCondCI = printBootstrapMeanInterval(spreadIfDefined, "mean spread if defined")
    myOutput.append(round(np.mean(spreadIfDefined), myPlaces))
    myOutput.append(spreadCondCI[0])
    myOutput.append(spreadCondCI[1])

    rmsCI = printBootstrapMeanInterval(rms, "mean rms midquote vs estimate")
    myOutput.append(round(np.mean(rms), myPlaces))
    myOutput.append(rmsCI[0])
    myOutput.append(rmsCI[1])

    minShadeCI = printBootstrapMeanInterval(minShade, "mean min shade from estimate")
    myOutput.append(round(np.mean(minShade), myPlaces))
    myOutput.append(minShadeCI[0])
    myOutput.append(minShadeCI[1])

    fracCI = printBootstrapMeanInterval(frac, "mean fraction estimate in spread")
    myOutput.append(round(np.mean(frac), myPlaces))
    myOutput.append(fracCI[0])
    myOutput.append(fracCI[1])

    inventoryCI = printBootstrapMeanInterval(inventory, "mean absval MM inventory")
    myOutput.append(round(np.mean(inventory) / mmCount, myPlaces))
    myOutput.append(round(inventoryCI[0] / mmCount, myPlaces))
    myOutput.append(round(inventoryCI[1] / mmCount, myPlaces))

    spreadsEarnedCI = printBootstrapMeanInterval(spreadsEarned, "mean MM spreads earned")
    myOutput.append(round(np.mean(spreadsEarned) / mmCount, myPlaces))
    myOutput.append(round(spreadsEarnedCI[0] / mmCount, myPlaces))
    myOutput.append(round(spreadsEarnedCI[1] / mmCount, myPlaces))

    spreadProfitCI = printBootstrapMeanInterval(spreadProfit, "mean MM spread profit")
    myOutput.append(round(np.mean(spreadProfit) / mmCount, myPlaces))
    myOutput.append(round(spreadProfitCI[0] / mmCount, myPlaces))
    myOutput.append(round(spreadProfitCI[1] / mmCount, myPlaces))

    positioningProfitCI = printBootstrapMeanInterval(positioningProfit, "mean MM positioning profit")
    myOutput.append(round(np.mean(positioningProfit) / mmCount, myPlaces))
    myOutput.append(round(positioningProfitCI[0] / mmCount, myPlaces))
    myOutput.append(round(positioningProfitCI[1] / mmCount, myPlaces))

    printForCsv()
    pass

def printForCsv():
    print ','.join(map(str, myOutputStrings))
    print ','.join(map(str, myOutput))
    pass

def printBootstrapStdevInterval(myList, myName):
    myPlaces = 3
    twoPointFivePerc = round(bootstrapStdevPercentile(myList, 2.5), myPlaces)
    ninetySevenPointFivePerc = round(bootstrapStdevPercentile(myList, 97.5), myPlaces)
    print myName + " 95% confidence range for stdev: [" + str(twoPointFivePerc) + ", " + str(ninetySevenPointFivePerc) + "]" 
    pass

def bootstrapStdevPercentile(myList, perc):
    sampleResults = []
    while len(sampleResults) < bootstrapSamples:
        sample = getSample(myList)
        sampleResult = np.std(sample)
        sampleResults.append(sampleResult)
        pass
    return np.percentile(sampleResults, perc)

def printBootstrapMeanInterval(myList, myName):
    myPlaces = 3
    twoPointFivePerc = round(bootstrapMeanPercentile(myList, 2.5), myPlaces)
    ninetySevenPointFivePerc = round(bootstrapMeanPercentile(myList, 97.5), myPlaces)
    print myName + " 95% confidence range for mean: [" + str(twoPointFivePerc) + ", " + str(ninetySevenPointFivePerc) + "]" 
    return [twoPointFivePerc, ninetySevenPointFivePerc]

def bootstrapMeanPercentile(myList, perc):
    sampleResults = []
    while len(sampleResults) < bootstrapSamples:
        sample = getSample(myList)
        sampleResult = np.mean(sample)
        sampleResults.append(sampleResult)
        pass
    return np.percentile(sampleResults, perc)

def getSample(myList):
    result = []
    while len(result) < len(myList):
        result.append(random.choice(myList))
    return result

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print "must specify folder"
        print "must specify MM count"
        sys.exit(1)
    folderName = sys.argv[1]
    mmCount = int(float(sys.argv[2]))
    if not os.path.isdir(folderName):
        print "not a directory"
        sys.exit(1)        
    getValuesFromFolder(folderName)
    print "from folder: " + folderName
    print "bootstrap samples: " + str(bootstrapSamples)
    print "files count: " + str(len(spread))
    printMeans()
    printSds()
    printBootstrapMeanIntervals(mmCount)
    pass
