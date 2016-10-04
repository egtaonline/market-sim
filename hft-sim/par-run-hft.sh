#!/bin/bash
# Run a simulation a given number of times

IFS='
'

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "usage: $0 [-h] simulation-folder [num-of-obs] [num-proc]"
    echo
    echo "positional arguments:"
    echo " simulation-folder"
    echo "              Folder that contains simulation_spec.json"
    echo " num-of-obs   Num of observations to gather (default: 1)"
    echo " num-proc     Number of processes to use (default: 2)"
    exit 0
elif [ $# -lt 1 ]; then
    echo "usage: $0 [-h] simulation-folder [num-of-obs] [num-proc]"
    exit 1
fi

# Parse Arguments
LOC=$(dirname "$0")
"$LOC/par-run-local-hft.sh" "$LOC/dist/hft.jar" "$@"
