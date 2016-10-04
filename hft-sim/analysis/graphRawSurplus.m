% graph total undiscounted surplus (as found columns with substring "surplus_sum_total")

trialName = 'example';
eval(['load ',trialName])

close all
graphHeaders(trialName, sort(getHeaders(headers,'_surplus_sum_total')), ...
        'total undiscounted surplus', true);
