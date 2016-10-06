#!/usr/bin/env python
import argparse
import json
import sys
import os
from os import path
import itertools

from numpy import random
# example call:
# python sample.py -s simulation_spec.json -f profile.json -n 100 -d /my_output_dir
parser = argparse.ArgumentParser(description='Sample player strategies for simulation spec files from a distribution of strategies by role.')
parser.add_argument('-s', '--simspec', type=argparse.FileType('r'), required=True, help='A simulation spec file for each sample. Must also including the number of players per role in a root item called "role_counts".')
parser.add_argument('-f', '--profile', type=argparse.FileType('r'), default=sys.stdin, help='A mixed strategy profile indicating the sample probability for each strategy.')
parser.add_argument('-n', '--num-samples', type=int, default=1, help='The number of samples to incorporate into the average social welfare.')
parser.add_argument('-d', '--directory', help='The directory to put all of the simulations in.')

# profile.json has:
# profile: a Python dictionary object from the entire profile.json file

# profile is a dict object
# players is a list of how many players there are per role
def sample_players(profile, players):
    assignment = {}
    # profile must contain a set of key-value pairs.
    # iterate over all key-value pairs in the dict object "profile"
    # role is the key, probs is the value
    for role, probs in profile.iteritems():
        # each "probs" value is a list. *probs.items performs "splat" on this list.
        # splat replaces a list with the sequence of separate items in that list.
        # zip takes a set of k lists of length n, and returns a list n of k-tuples, where the ith
        # k-tuple contains the ith item from each of the k input lists.
        # the comma indicates that the output of zip is assigned as an ordered pair to (strats, probs).
        # there must be 2 items in each list within probs, though we don't know how many lists probs has.
        # the list of 1st items from each list is assigned to strats, the list of 2nd items from each list to probs.
        strats, probs = zip(*probs.items())
        assignment[role] = list(itertools.chain.from_iterable(
            itertools.repeat(x, y) for x, y
            in zip(strats, random.multinomial(players[role], probs))))
    return assignment

if __name__ == '__main__':
    args = parser.parse_args()

    simspec = json.load(args.simspec)
    # question: where do 'role_counts' get specified in simulation_spec.json?
    role_counts = simspec['role_counts']
    profile = json.load(args.profile)

    # Create directory structure
    # e.g., if there are 100 samples, length 09-99 is 2
    # d means integer base 10
    # %02 means to pad the number with leading zeros up to length 2
    fmt = "%0" + str(len(str(args.num_samples - 1))) + "d"
    # iterates from 0 to num_samples - 1
    for i in xrange(args.num_samples):
        # example: sim_dir = /my_output_dir/02
        sim_dir = path.join(args.directory, fmt % i)
        simspec['assignment'] = sample_players(profile, role_counts)
        os.mkdir(sim_dir)
        with open(path.join(sim_dir, 'simulation_spec.json'), 'w') as f:
            # write out object simspec to output stream f
            json.dump(simspec, f)
