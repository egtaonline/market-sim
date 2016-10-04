#! /usr/bin/env python
import sys
import math
import json
import argparse
import textwrap

parser = argparse.ArgumentParser(description='\n'.join(textwrap.wrap('Merge successive observation files like egta. "features" will contain the mean of all of the features per observation run. This could be changed in the future to also include standard deviations. The "config" feature will be the config of the last observation, but these should all be idential. The "players" object will contain an object nested by role and then by strategy. The final object has three field. "mean" which is simply the mean of all agent payoffs for that role and strategy. "true_sample_stddev" is the sample standard deviation of every pay off for every agent in every simulation. "egta_sample_stddev" is the sample standard deviation of the mean of all agent payoffs of that role and strategy in every observation. That is, payoffs are aggregated to a mean value for each role and strategy for each observation, then this is the standard deviation of those means across observations.', width=78)),
                                 epilog='''example usage:
  ''' + sys.argv[0] + ''' simulation_directory/observation*.json > simulation_directory/merged_observation.json
  ''' + sys.argv[0] + ''' simulation_directory/observation*.json -o simulation_directory/merged_observation.json''',
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument('files', metavar='obs-file', nargs='+', type=argparse.FileType('r'),
                    help='An observation file to merge')
parser.add_argument('-o', '--output', '-f', '--file', metavar='merged-obs-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The merged json file to write to, defaults to stdout')

class KahanSum(object):
    def __init__(self):
        self.__sum = self.__c = 0.0

    def __add__(self, val):
        y = val - self.__c
        t = self.__sum + y
        c = (t - self.__sum) - y
        self.__sum = t
        return self

    def sum(self):
        return self.__sum

class SumStats(object):
    def __init__(self):
        self.n = 0
        self.sum = KahanSum()
        self.sumsq = KahanSum();

    def __add__(self, val):
        self.n += 1
        self.sum += val
        self.sumsq += val * val
        return self

    def add_one(self, val):
        self += val

    def add_many(self, n, sum, sumsq):
        self.n += n
        self.sum += sum
        self.sumsq += sumsq

    def add_mean_stddev(self, n, mean, stddev):
        add_many(self, n, n * mean, stddev * stddev * (n - 1) + n * mean * mean)

    def mean(self):
        return float(self.sum.sum()) / self.n;

    def __sq_err__(self):
        return self.sumsq.sum() - self.sum.sum() ** 2 / self.n

    def sample_stddev(self):
        return math.sqrt(self.__sq_err__() / (self.n - 1))

def mergeObs(filelike):
    
    true_player_stats = {}
    egta_player_stats = {}
    feature_stats = {}
    player_feature_stats = {}

    true_stddev = True

    for file in filelike:
        obs = json.load(file)

        players = obs['players']
        features = obs['features']
        config = features.pop('config', features)
        n = int(config['numSims'])

        for feat, val in features.iteritems():
            feature_stats.setdefault(feat, SumStats()).add_one(float(val))

        true_stddev &= 'payoff_stddev' in players[0]['features']
            
        sim_egta_stats = {}

        for player in players:
            true_stat = true_player_stats.setdefault(player['role'], {}).setdefault(player['strategy'], SumStats())
            egta_stat = sim_egta_stats.setdefault(player['role'], {}).setdefault(player['strategy'], SumStats())
            mean = float(player['payoff'])
            true_stat.add_many(n, n * mean, float(player['features'].get('payoff_stddev', 0)) ** 2 * (n - 1) + n * mean ** 2)
            egta_stat.add_one(mean)

            pf_stats = player_feature_stats.setdefault(player['role'], {}).setdefault(player['strategy'], {})
            for feat, val in player['features'].iteritems():
                pf_stats.setdefault(feat, SumStats()).add_one(val)

        for role, rest in sim_egta_stats.iteritems():
            for strat, stat in rest.iteritems():
                egta_player_stats.setdefault(role, {}).setdefault(strat, SumStats()).add_one(stat.mean())
                
    players = {}
    for role, rest in true_player_stats.iteritems():
        for strat, stat in rest.iteritems():
            agg = {
                'mean': stat.mean(),
                'egta_sample_stddev': egta_player_stats[role][strat].sample_stddev(),
                'features': {feat: stat.mean() for feat, stat in player_feature_stats[role][strat].iteritems()}
            }
            if true_stddev: # Don't include if missing data
                agg['true_sample_stddev'] = stat.sample_stddev()

            players.setdefault(role, {})[strat] = agg

    features = { 'config': config }
    for feat, stat in feature_stats.iteritems():
        features[feat] = stat.mean()

    return { 'players': players, 'features': features }

if __name__ == "__main__":
    args = parser.parse_args()
    json.dump(mergeObs(args.files), args.output)
    args.output.write('\n')
    args.output.close()
