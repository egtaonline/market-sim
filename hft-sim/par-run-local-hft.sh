#!/bin/bash
# Run a simulation a given number of times

IFS='
'

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "usage: $0 [-h] jar simulation-folder [num-of-obs] [num-proc]"
    echo
    echo "positional arguments:"
    echo " jar          Simulator jar file to use"
    echo " simulation-folder"
    echo "              Folder that contains simulation_spec.json"
    echo " num-of-obs   Num of observations to gather (default: 1)"
    echo " num-proc     Number of processes to use (default: 2)"
    exit 0
elif [ $# -lt 1 ]; then
    echo "usage: $0 [-h] jar simulation-folder [num-of-obs] [num-proc]"
    exit 1
fi

# Parse Arguments
LOC=$(dirname "$0")
CLASSPATH=$(ls lib/*.jar | tr '\n' :)$(readlink -m "$1")
FOLDER=$(readlink -m "$2") # Convert to absolute path
NUM="$3"
NUM_PROC="$4"
if [[ -z "$NUM" ]]; then
    NUM=1
fi
if [[ -z "$NUM_PROC" ]]; then
    NUM_PROC=2
fi

# Observations per process
PER_PROC=$(( $NUM / $NUM_PROC ))

# Change to $LOC to run java, necessary for environment properties loading
cd "$LOC"

# Run parllely
parfor () { # OUTPUT_FILE FOLDER START END+1
    for (( OBS = $3; OBS < $4; ++OBS )); do
	echo ">> Running simulation $OBS..." >&2
	java -cp "${CLASSPATH}" systemmanager.SystemManager "$2" "$OBS"
	echo "$FOLDER/observation$OBS.json" >> "$1"
    done
}

FILES=()
# Don't do the last one to account for rounding
for (( I=1; I<$NUM_PROC; ++I )); do
    FILE=$(mktemp)
    parfor "$FILE" "$FOLDER" $(( $I * $PER_PROC - $PER_PROC )) $(( $I * $PER_PROC )) &
    FILES+=( "$FILE" )
done
FILE=$(mktemp)
parfor "$FILE" "$FOLDER" $(( $NUM_PROC * $PER_PROC - $PER_PROC )) $NUM &
FILES+=( "$FILE" )

trap "kill 0" SIGINT SIGTERM #EXIT # Pass signals to children
wait

OBSERVATIONS=()
for FILE in "${FILES[@]}"; do
    OBSERVATIONS+=( $(cat "$FILE") )
    rm "$FILE"
done

# Change back after finished running
cd - > /dev/null

# Exit if we got killed
[[ "$NUM" -ne "${#OBSERVATIONS[@]}" ]] && exit 1

if [[ $NUM -gt 1 ]]; then
    echo -n ">> Merging observations..." >&2
    "$LOC/merge-obs-egta.py" "${OBSERVATIONS[@]}" > "$FOLDER/merged_observation${NUM}.json"
    echo " done" >&2
fi
