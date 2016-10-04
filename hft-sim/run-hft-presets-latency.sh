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
    echo "Run simulation spec with all of the specified presets from latency 0 to 1000"
    echo
    echo "Creates a new directory for each preset and latency setting, runs the spec file num-obs times each, then merges each of the obsevations into a new directory called merged. Presets can be optionally specified after num-obs. If omitted this script will use the default presets of CENTRALCALL, CENTRALCDA, TWOMARKET, and TWOMARKETLA, if included there must be at least two presets" | fold -s
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
FOLDER="${1%/}"
NUM_OBS="$2"
LAT_MAX=1000
LAT_STEP=100

for PRESET in "${PRESETS[@]}"; do 
    for (( LAT1=0; LAT1<=$LAT_MAX; LAT1+=$LAT_STEP )); do
        echo ">> Setting up latency $LAT1"
        mkdir -p "$FOLDER/${PRESET}_${LAT1}" 
        cp "$FOLDER/$SPEC_FILE" "$FOLDER/${PRESET}_${LAT1}/$SPEC_FILE"
        sed -i 's/"presets" *: *"[^"]*"/"presets": "'"$PRESET"'"/g' "$FOLDER/${PRESET}_${LAT1}/$SPEC_FILE"
        sed -i 's/"modelName" *: *"[^"]*"/"modelName": "'"${PRESET}_${LAT1}"'"/g' "$FOLDER/${PRESET}_${LAT1}/$SPEC_FILE"
        "$LOC/run-hft.sh" "$FOLDER/${PRESET}_${LAT1}" "$NUM_OBS"
    done
done

if [[ -d $FOLDER/$MERGED ]]; then
    rm -rf $FOLDER/$MERGED
    echo "removed"
fi
"$LOC/merge-sim-obs.sh" "$FOLDER/$MERGED" "$NUM_OBS" ${FOLDER}/*/
#"$LOC/merge-sim-obs.sh" "$FOLDER/$MERGED" "$NUM_OBS" "${PRESETS[@]/#/$FOLDER/}"
#"$LOC/merge-sim-logs.sh" "$FOLDER/$MERGED" "$NUM_OBS" "${PRESETS[@]/#/$FOLDER/}"
