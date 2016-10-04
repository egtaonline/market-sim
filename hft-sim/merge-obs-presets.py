#! /usr/bin/env python
import sys
import json
import argparse
import textwrap
from os import path

parser = argparse.ArgumentParser(description='\n'.join(textwrap.wrap('Merges observation files from different preset runs and returns summary statistics. This is meant to account for past behavior where several presets were run at the same time. This requires that the simulations have different names. If they do not, there is no way to give labels to the different runs.', width=78)),
                                 epilog='''example usage:
  for OBS in {0..99}; do
      ''' + sys.argv[0] + ''' directory/{CALL,CDA,TWOMARKET}/observation$OBS.json > directory/merged/observation$OBS.json
  done''', formatter_class = argparse.RawTextHelpFormatter)
parser.add_argument('files', metavar='obs-file', nargs='+',
                    help='An observation file to merge')
parser.add_argument('-o', '--output', '-f', '--file', metavar='merged-obs-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The merged observation file, defaults to stdout')

def mergeObs(filenames):

    names = map(path.dirname, map(path.abspath, filenames))
    prefixLength = len(path.dirname(path.commonprefix(names))) + 1
    
    out = {}
    feats = {}
    out['features'] = feats

    for filename, name in zip(filenames, (n[prefixLength:] for n in names)):
        with open(filename, 'r') as first:
            obs = json.load(first)

        players = obs['players']
        features = obs['features']
        config = features.pop('config')

        for feat, val in features.iteritems():
            feats[name + '_' + feat] = float(val)
        out[name + '_config'] = config
        out[name + '_players'] = players

    return out

if __name__ == "__main__":
    args = parser.parse_args()
    json.dump(mergeObs(args.files), args.output)
    args.output.write('\n')
    args.output.close()
