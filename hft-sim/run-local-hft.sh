#!/bin/bash
# Run a simulation a given number of times

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "usage: $0 [-h] jar simulation-folder [num-of-obs]"
    echo
    echo "num-of-obs defaults to 1 if not specified"
    exit 0
elif [ $# -lt 1 ]; then
    echo "usage: $0 [-h] jar simulation-folder [num-of-obs]"
    exit 1
fi

# Parse Arguments
LOC=$(dirname "$0")
CLASSPATH=$(ls lib/*.jar | tr '\n' :)"$(pwd)/$1"
FOLDER="$(pwd)/$2" # Convert to absolute path
NUM="$3"
if [[ -z "$NUM" ]]; then
    NUM=1
fi

# Change to $LOC to run java, necessary for environment properties loading
cd "$LOC"

# Run
OBSERVATIONS=()
for (( OBS = 0; OBS < $NUM; ++OBS )); do
    echo -n ">> Running simulation $OBS..."
    java -cp "${CLASSPATH}" systemmanager.SystemManager "$FOLDER" "$OBS"
    echo " done"
    OBSERVATIONS+=( "$FOLDER/observation$OBS.json" )
done

# Change back after finished running
cd - > /dev/null

#if [[ $NUM -gt 1 ]]; then
#    echo -n ">> Merging the observations..."
#    "$LOC/merge-obs-egta.py" "${OBSERVATIONS[@]}" > "$FOLDER/merged_observation${NUM}.json"
#    echo " done"
#fi
