function x = getIndex(headers, colName)
% x = getIndex(headers, colName);
% x = getIndex(headers, colNames);

if (~iscellstr(colName))
    tmp = strfind(headers, colName);
    x = find(~cellfun('isempty', tmp));
else
    x = [];
    if iscolumn(colName)
        colName = colName';
    end
    for i = colName
        x = [x; find(strcmp(headers, i))];
    end
end

