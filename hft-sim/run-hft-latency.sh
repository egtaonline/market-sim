#!/bin/bash
# Executes a different experiment for each sim spec file * presets

IFS='
'
FILE=simulation_spec.json
MERGED=merged
LOGDIR=logs

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] directory num-obs [max-latency [latency-step]]"
    echo
    echo "Run simulation spec with various slow LA latencies"
    exit 0
elif [ $# -lt 2 ]; then
    echo "usage: $0 [-h] directory num-obs [max-latency [latency-step]]"
    exit 1
fi

LOC=$(dirname "$0")
FOLDER="$1"
NUM="$2"
LAT_MAX=50
LAT_STEP=10

if [ $# -ge 3 ]; then
    LAT_MAX="$3"
fi
if [ $# -ge 4 ]; then
    LAT_STEP="$4"
fi

#TODO Read simspec file, and modify nbbo and market latency if too low

for (( LAT1=$LAT_STEP; LAT1<=$LAT_MAX; LAT1+=$LAT_STEP )); do
    for (( LAT2=$LAT_STEP; LAT2<=$LAT1; LAT2+=$LAT_STEP )); do
	echo ">> Setting up latencies $LAT1 and $LAT2"
	mkdir -vp "$FOLDER/${LAT1}_${LAT2}"
	cp -v "$FOLDER/$FILE" "$FOLDER/${LAT1}_${LAT2}/$FILE"
	sed -i -e 's/"la_1"/"LA:laLatency_'"${LAT1}"'"/g' -e 's/"la_2"/"LA:laLatency_'"${LAT2}"'"/g' "$FOLDER/${LAT1}_${LAT2}/$FILE"
	time "$LOC/run-hft.sh" "$FOLDER/${LAT1}_${LAT2}" $NUM
    done
done
