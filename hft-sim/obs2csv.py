#! /usr/bin/env python
import re
import sys
import json
import argparse
import textwrap

parser = argparse.ArgumentParser(description='\n'.join(textwrap.wrap('Merge several observation files into a csv. It only reports the "features" and forgets about player information.', width=78)),
                                 epilog='''example usage:
  ''' + sys.argv[0] + ''' simulation_directory/observation*.json > merged.csv
  ''' + sys.argv[0] + ''' simulation_directory/observation*.json -o merged.csv''',
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument('files', metavar='obs-file', nargs='+',
                    help='An observation file to include in the final csv')
parser.add_argument('-o', '--output', '-f', '--file', metavar='csv-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The csv file to write to, defaults to stdout')

def to_csv(out, filenames):
    with open(filenames[0], 'r') as first:
        obs = json.load(first)
    obs.pop('config', None)
    keys = sorted(obs['features'].keys(), key=lambda s: s[::-1])
    out.write('obs,')
    if 'config' in keys:
	keys.remove('config')
        config = sorted(obs['features']['config'].keys())
        out.write(','.join(config))
	out.write(',')

    out.write(','.join(keys))
    out.write('\n')

    for filename in filenames:
        with open(filename, 'r') as f:
            obs = json.load(f)
	out.write(re.search('\d+', filename).group())
	out.write(',')
        feats = obs['features']
	if 'config' in obs['features'].keys():
	    configs = obs['features']['config']
	    out.write(','.join(str(configs[c]) for c in config))
	    out.write(',')
        out.write(','.join(str(feats[k]) for k in keys))
        out.write('\n')

if __name__ == "__main__":
    args = parser.parse_args()
    to_csv(args.output, args.files)
