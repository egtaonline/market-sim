function [data headers] = readCSV(filename)
% [data, headers] = readCSV(filename)
%
% Example: [d, h] = readCSV('test.csv');

fid = fopen(filename);
% tmp = textscan(fid,'%f');
lineArray = cell(50,1); % longer than needed
lineIndex = 1;
nextLine = fgetl(fid);
while ~isequal(nextLine,-1)
    lineArray{lineIndex} = nextLine;
    lineIndex = lineIndex + 1;
    nextLine = fgetl(fid);
end
fclose(fid);
headers = textscan(lineArray{1},'%s','Delimiter',',');
lineArray = lineArray(2:lineIndex-1);
for i = 1:lineIndex-2
    lineData = textscan(lineArray{i},'%s','Delimiter',',');
    lineData = lineData{1};
    lineArray(i,1:numel(lineData)) = lineData;
end
headers = headers{:};
data = cellfun(@(s) str2double(s), lineArray);

end
