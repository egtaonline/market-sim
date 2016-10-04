function newHeaders = removeHeaderType(headers, headerToRemove)
% newHeaders = removeHeader(oldHeaders, strToRemove)
% 
% Find & remove headers matching a certain pattern
% Example: newHeaders = removeHeader(allHeaders, 'sum')

x = find(~cellfun('isempty', strfind(headers, headerToRemove)));
if (~isempty(x))
    headers(x) = [];
end

newHeaders = headers;
