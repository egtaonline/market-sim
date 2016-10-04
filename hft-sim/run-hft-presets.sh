#!/bin/bash
# Executes a different experiment for each sim spec file * presets

IFS='
'
SPEC_FILE=simulation_spec.json
MERGED=merged
LOGDIR=logs

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] directory num-obs [preset preset [preset ...]]"
    echo
    echo "Run simulation spec with all of the specified presets"
    echo
    echo "Creates a new directory for each preset, and runs the spec file num-obs times each, then merges each of the obsevations into a new directory called merged. Logs are also merged and put in the logs director in merged. Presets can be optionally specified after num-obs. If omitted this script will use the default presets of CENTRALCALL, CENTRALCDA, TWOMARKET, and TWOMARKETLA, if included there must be at least two presets" | fold -s
    echo
    echo "example usage:"
    echo "  $0 sim_dir 100"
    echo "  $0 sim_dir 100 CENTRALCDA TWOMARKET"
    exit 0
elif [[ $# -lt 2 || $# -eq 3 || ! "$2" =~ ^[0-9]+$ ]]; then
    echo "usage: $0 [-h] directory num-obs [preset preset [preset ...]]"
    exit 1
elif [ $# -lt 3 ]; then
    PRESETS=( CENTRALCALL CENTRALCDA TWOMARKET TWOMARKETLA )
else
    PRESETS=( "${@:3}" )
fi

LOC=$(dirname "$0")
FOLDER="$1"
NUM_OBS="$2"

for PRESET in "${PRESETS[@]}"; do 
    echo ">> Setting up $PRESET" >&2
    mkdir -p "$FOLDER/$PRESET" &>/dev/null
    cp -v "$FOLDER/$SPEC_FILE" "$FOLDER/$PRESET/$SPEC_FILE"
    sed -i 's/"presets" *: *"[^"]*"/"presets": "'"$PRESET"'"/g' "$FOLDER/$PRESET/$SPEC_FILE"
    "$LOC/run-hft.sh" "$FOLDER/$PRESET" "$NUM_OBS"
done

"$LOC/merge-sim-obs.sh" "$FOLDER/$MERGED" "$NUM_OBS" "${PRESETS[@]/#/$FOLDER/}"
#"$LOC/merge-sim-logs.sh" "$FOLDER/$MERGED" "$NUM_OBS" "${PRESETS[@]/#/$FOLDER/}"
