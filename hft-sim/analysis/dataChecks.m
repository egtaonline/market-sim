function dataChecks(expName)
% dataChecks(expName)
%
% This function takes an experiment name, runs data checks on it and returns a results file. It checks
% for matching header and column numbers, latency range, matching surplus, matching transaction 
% numbers, matching execution speed, matching median NBBO spreads, and matching volatility.
%
% It generates a .mat file with 2 variables, data and headers.
%
% The function performs the following data checks. Most of the checks in only apply when latency is 0. 
% 
% 1) The first thing we check is that the number of headers matches the numbers of columns in the data. 
%    If these don't match, that implies that something went wrong when parsing the file. 
% 2) The second thing we check is the range of the latency, which should be between 0 and 1000. If 
%    latency is not the variable being varied it will always be 0. 
% 3) Next, we check that various things match at latency zero. Undiscounted surplus should match, as 
%    well as transaction number, execution speeds, and med NBBO spreads. We check that discounted 
%    surplus matches at every market model except CENTRALCALL. 
% 4) Last we check that various volatilities match. For average (price or return) volatility, we 
%    check that central call and central cda match and TWOMARKET-DUMMY and TWOMARKET-LA match. When 
%    markets are involved we check that 1 and 6, 2 and 4, and 3 and 5 match (assuming running with
%    four market models)

outputName = [expName, '_output.log'];
file = [expName, '.csv'];
[data,headers] = readCSV(file);
filename = [expName, '.mat'];
save(filename, 'data', 'headers');
load(filename);

fileID = fopen(outputName, 'a');

[datar, datac] = size(data);
headerNum = length(headers);
latencyIndex = getIndex(headers, 'latency');
logIndex = getIndex(headers, 'obs');
epsilon = 1e-3;

%Header variables
noDiscSurpHeader = 'surplus_sum_total_nodisc';
transNumHeader = 'trans_zi_num';
discSurpHeader = 'surplus_sum_total_disc';
execSpeedHeader = 'exectime';
spreadMedNbboHeader = 'spreads_med_nbbo';
volMeanStdPriceHeader = 'mean_stdprice';
volStdPriceHeader = 'std_price_mkt';
volMeanLogPriceHeader = 'mean_logprice';
volStdLogReturnHeader = 'std_logreturn_mkt';
volMeanLogReturnHeader = 'mean_logreturn';


fprintf(fileID,'\n\n');
fprintf(fileID,'DATA CHECKS ON EXPERIMENT: %s\n', expName);


% Check for latency column
assert(~isempty(latencyIndex), 'Latency column does not exist!');

% checks that number of col headers equals max number of cols in data
fprintf(fileID,'Testing number of column headers = max columns in data:                        ');
if headerNum == datac
   fprintf(fileID,'PASSED\n');
else
   fprintf(fileID,'FAILED\n');
   fprintf(fileID,'\t Number of column headers do not match number of columns in data \n');
end

% test range of latency values
fprintf(fileID,'Testing range of latency values:                                               ');
test = true;
wrong = 0;
for row = 1:datar
   if data(row, latencyIndex) < 0 || data(row, latencyIndex) > 1000
      test = false;
      wrong = wrong + 1;
      if wrong == 1
         fprintf(fileID,'FAILED\n');
         fprintf(fileID,'\t Latency out of range at observation %d \n', data(row, logIndex));
      elseif wrong > 1
         fprintf(fileID,'\t Latency out of range at observation %d \n', data(row, logIndex));
      end
   end
end

if test
   fprintf(fileID,'PASSED\n');
end

% test for matching undiscounted surplus at zero latency
noDiscSurpIndex = getIndex(headers, noDiscSurpHeader); 
fprintf(fileID,'Testing total undiscounted surplus at latency 0:                               ');
testSome(noDiscSurpIndex, 'Undiscounted surplus sum total for latency 0 does not match at observation');

% test for transaction number for latency 0
transNumIndex = getIndex(headers, transNumHeader); 
fprintf(fileID,'Testing transaction num at latency 0:                                          ');
testSome(transNumIndex, 'Transaction number for latency 0 does not match at observation');

% test for discount surplus for all models except central call at latency 0
discSurpIndex = getIndex(headers, discSurpHeader);
noCallDiscSurpIndex = discSurpIndex(2:end,1);
fprintf(fileID,'Testing total discounted surplus at latency 0 except central call:             ');
testSome(noCallDiscSurpIndex, 'Discount surplus does not match at observation');

% test for execution speed at 0 latency
execTimeIndex = getIndex(headers, execSpeedHeader); 
fprintf(fileID,'Testing execution speed at latency 0:                                          ');
testSome(execTimeIndex, 'Execution speed for latency 0 does not match at observation');

% test for spreads of med nbbo at 0 latency
medNbboIndex = getIndex(headers, spreadMedNbboHeader); 
fprintf(fileID,'Testing median NBBO spread at latency 0:                                       ');
testSome(medNbboIndex, 'Median NBBO spread for latency 0 does not match at observation');

% test for avg vol (std dev of log of midquote prices) at 0 latency
volMeanStdPriceIndex = getIndex(headers, volMeanStdPriceHeader);
fprintf(fileID,'Testing price vol (std dev of log of midquote prices) at latency 0:            ');
testVol(volMeanStdPriceIndex, 'Price vol (std dev of log of midquote prices) for latency 0 does not match at observation');

% test for avg vol (log of std dev of midquote prices) at 0 latency
volMeanLogPriceIndex = getIndex(headers, volMeanLogPriceHeader);
fprintf(fileID,'Testing log price vol (log of std dev of midquote prices) at latency 0:        ');
testVol(volMeanLogPriceIndex, 'Log price vol (log of std dev of midquote prices) for latency 0 does not match at observation');

% test for avg vol (std dev of log returns) at 0 latency
volMeanLogReturnIndex = getIndex(headers, volMeanLogReturnHeader);
fprintf(fileID,'Testing log return vol (std dev of log returns) at latency 0:                  ');
testVol(volMeanLogReturnIndex, 'Log return vol (std dev of log returns) for latency 0 does not match at observation');

% test vol in markets (log of std dev of midquote prices) at 0 latency
volStdPriceIndex = getIndex(headers, volStdPriceHeader);
fprintf(fileID,'Testing price vol in markets (log of std dev of midquote prices) at latency 0: ');
testVol2(volStdPriceIndex, 'Price vol (log of std dev of midquote prices) for latency 0 does not match at observation', 1, 2, 3, 5, 4, 6);

% test vol in markets (std dev of log returns) at 0 latency
volStdLogReturnIndex = getIndex(headers, volStdLogReturnHeader);
fprintf(fileID,'Testing log return vol in markets (std dev of log returns) at latency 0:       ');
testVol2(volStdLogReturnIndex, 'Log return vol (std dev of log returns) for latency 0 does not match at observation', 1, 2, 3, 6, 4, 5);



% Function that iterates through one index and checks for equality
function testSome(dataIndex, errMessage)
  test = true;
  wrong = 0;
  [r, c] = size(dataIndex);
  for row = 1: datar
     if (data(row,latencyIndex) == 0)
        index = data(row, dataIndex);
        for j = 2:r - 2
           if ~(le(abs(index(1, j)- index(1, 1)),epsilon))
              test = false;
              wrong = wrong + 1;
              if wrong == 1
                 fprintf(fileID,'FAILED\n');
                 fprintf(fileID,'\t %s %d \n', errMessage, data(row, logIndex));
              elseif wrong > 1
                 fprintf(fileID,'\t %s %d \n', errMessage, data(row, logIndex));
              end
           end
        end
     end
  end

  if test
     fprintf(fileID,'PASSED\n');
  end
end

% Volatility function 1
function testVol(dataIndex, errMessage)
  test = true;
  wrong = 0;
  for row = 1: datar
     if (data(row, latencyIndex) == 0)
        if (~(le(abs(data(row, dataIndex(1))- data(row, dataIndex(2))),epsilon))) || ...
           (~(le(abs(data(row, dataIndex(3))- data(row, dataIndex(4))),epsilon)))
           test = false;
           wrong = wrong + 1;
           if wrong == 1
              fprintf(fileID,'FAILED\n');
              fprintf(fileID,'\t %s %d \n', errMessage, data(row, logIndex));
           elseif wrong > 1
              fprintf(fileID,'\t %s %d \n', errMessage, data(row, logIndex));
           end
        end
     end
  end

  if test
     fprintf(fileID,'PASSED\n');
  end
end

% Volatility function 2
function testVol2(dataIndex, errMessage, a, b, c, d, e, f)
  test = true;
  wrong = 0;
  for row = 1: datar
     if (data(row, latencyIndex) == 0)
        if (~(le(abs(data(row, dataIndex(a))- data(row, dataIndex(b))),epsilon))) || (~(le(abs(data(row, dataIndex(c))- ...
           data(row, dataIndex(d))),epsilon))) || (~(le(abs(data(row, dataIndex(e))- data(row, dataIndex(f))),epsilon)))
           test = false;
           wrong = wrong + 1;
           if wrong == 1
              fprintf(fileID,'FAILED\n');
              fprintf(fileID,'\t %s %d \n', errMessage, data(row, logIndex));
           elseif wrong > 1
              fprintf(fileID,'\t %s %d \n', errMessage, data(row, logIndex));
           end
        end
     end
  end

  if test
     fprintf(fileID,'PASSED\n');
  end
end   

end
