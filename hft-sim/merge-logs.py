#! /usr/bin/env python
import sys
import re
import argparse
from os import path
from Queue import PriorityQueue

parser = argparse.ArgumentParser(description='''Merges log files for easy comparison. This is very unsafe in terms of file handling (tries to open a lot of files, and will crash if want to merge more then the file system will allow), but if it's only used as a one time script it should work fine.''')
parser.add_argument('files', metavar='log-file', nargs='+', type=argparse.FileType('r'),
                    help='A log file to merge')
parser.add_argument('-o', '--output', '-f', '--file', metavar='merged-log-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The log file to write to, defaults to stdout')

# Regex for time of a line
retime = re.compile(r'\d+\|\s*(\d+)')

class LogReader:
    def __init__(self, f):
        self.file = f
        self.line = f.readline()
        self.time = -1

    def nextline(self):
        line = self.line
        self.line = self.file.readline()
        match = retime.match(self.line)
        if match:
            self.time = int(match.group(1))
        elif self.time >= 0:
            self.time = sys.maxint
        return line

    def __lt__(self, other):
        return self.time < other.time

    def __eq__(self, other):
        return self.time == other.time

def merge(logs, output):
    """ Merges log files off of time. Takes a generator of file descriptors """
    queue = PriorityQueue()

    names = [path.dirname(path.abspath(f.name)) for f in logs]
    prefixLength = len(path.dirname(path.commonprefix(names))) + 1

    for i, (log, name) in enumerate(zip(logs, names)):
        output.writelines((str(i), '| ', name[prefixLength:-5], '\n'))
        queue.put((LogReader(log), i))

    while not queue.empty():
        reader, i = queue.get()
        line = reader.nextline()
        if not line:
            continue
        output.writelines((str(i), '|', line))
        queue.put((reader, i))

if __name__ == "__main__":
    args = parser.parse_args()
    merge(args.files, args.output)
    for file in args.files:
        file.close()
    args.output.close()
