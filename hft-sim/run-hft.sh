#!/bin/bash
# Run a simulation a given number of times

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "usage: $0 [-h] simulation-folder [num-of-obs]"
    echo
    echo "num-of-obs defaults to 1 if not specified"
    exit 0
elif [ $# -lt 1 ]; then
    echo "usage: $0 [-h] simulation-folder [num-of-obs]"
    exit 1
fi

# Parse Arguments
LOC=$(dirname "$0")
"$LOC/run-local-hft.sh" "$LOC/dist/hft.jar" "$@"
