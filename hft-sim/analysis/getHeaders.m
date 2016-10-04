function y = getHeaders(headers, colName)
% x = getHeaders(headers, colName);
% x = getHeaders(headers, colNames);

if (~iscellstr(colName))
    tmp = strfind(headers, colName);
    x = find(~cellfun('isempty', tmp));
else
    x = [];
    for i = colName
        x = [x; find(strcmp(headers, i))];
    end
end
y = headers(x);
