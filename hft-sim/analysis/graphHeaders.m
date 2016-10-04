function graphHeaders(trialName, toPlotHeaders, toPlotTitle, errorbarOn)
% graphHeaders(trialName, toPlotHeaders, toPlotTitle, errorbarOn)
%
% The x-axis is latency. The y-axis is auto-scaled, based on the values pulled
% from the dataset that have headers in the toPlotHeaders list.
%
% Example: graphHeaders('test', headers, 'my plot', false);
%          (where the data is saved in test.mat)

eval(['load ',trialName])

latency = unique(data(:,strcmp('latency',headers)));
n = size(data,1)/length(latency);  % number of observations

% average everything in the data matrix
tmp = arrayfun(@(s) nanmean(data(data(:,getIndex(headers,'latency'))==s, :)), ...
    latency, 'UniformOutput', false);
avgData = cat(1,tmp{:});
clear tmp;

% compute std devs (for error bars)
tmp = arrayfun(@(s) nanstd(data(data(:,getIndex(headers,'latency'))==s, :),1), ...
    latency, 'UniformOutput', false);
stdData = cat(1,tmp{:});
clear tmp;

%%
toPlotMarkers = {'o', 'x', '^', 's', 'd', '*', '+', 'p', 'h'};
markerSize = 15; %10
fontSize = 14; %18
lineWidth = 4; %2

figure;
if (~errorbarOn)
    plot(repmat(latency, [1,length(toPlotHeaders)]), ...
        avgData(:,getIndex(headers, toPlotHeaders)), ...
        'LineWidth', lineWidth);
else
    errorbar(repmat(latency, [1,length(toPlotHeaders)]), ...
            avgData(:,getIndex(headers, toPlotHeaders)), ...
            1.96 *stdData(:,getIndex(headers, toPlotHeaders)) / sqrt(n), ...
            'LineWidth', lineWidth);
end
hdl = findobj(gca,'Type','line');

if (~errorbarOn)
    cnt = 1;
    for q = length(hdl):-1:1
	% Assign markers to each line handle
        set(hdl(cnt), 'Marker', toPlotMarkers{q}); 
        set(hdl(cnt), 'LineStyle', '-');
        set(hdl(cnt), 'MarkerSize', markerSize);
        cnt = cnt+1;
    end
end

hX = xlabel('latency');
hY = ylabel(toPlotTitle);
xlim([min(latency), max(latency)]);

hL = legend(strrep(sort(toPlotHeaders),'_',' '), 'Location', 'Best');
set(gca, 'TickDir', 'out', 'YGrid', 'on');
set(gcf,'PaperOrientation','landscape');
set(gcf,'PaperUnits','normalized');
set(gcf,'PaperPosition', [0 0 1 1]);
set([hL, gca, hX, hY], 'FontSize', fontSize);

