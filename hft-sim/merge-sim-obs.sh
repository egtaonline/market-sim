#!/bin/bash
# Executes a different experiment for each sim spec file * presets

IFS='
'
SPEC_FILE=simulation_spec.json
LOGDIR=logs

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] output-directory num-obs input-directory input-directory [input-directory ...]"
    echo
    echo "Merges the observation files from several directories with comparable settings e.g. same simulation_spec.json but different presets, and stores them in the output directory. If the output directory doesn't exist, it will be created." | fold -s
    echo
    echo "example usage:"
    echo "  $0 sim_dir/merged 10 sim_dir/{CENTRALCALL,CENTRALCDA,TWOMARKET,TWOMARKETLA}"
    echo "  $0 sim_dir/merged 10 sim_dir/*/ # Only works if \"merged\" doesn't exist"
    exit 0
elif [[ $# -lt 4 || ! "$2" =~ ^[0-9]+$ ]]; then
    echo "usage: $0 [-h] output-directory num-obs input-directory input-directory [input-directory ...]"
    exit 1
fi

LOC=$(dirname "$0")
OUTPUT="$1"
NUM_OBS="$2"
INPUTS=( "${@:3}" )

mkdir -vp "$OUTPUT"

echo -n ">> Merging similar run observations..."

for (( OBS=0; OBS < $NUM_OBS; ++OBS )); do
    "$LOC/merge-obs-presets.py" "${INPUTS[@]/%//observation$OBS.json}" > "$OUTPUT/observation$OBS.json"
done

# Removed because merge-obs-egta.py doesn't properly handle the renaming of players and config
# OBSERVATIONS=()
# for (( OBS = 0; OBS < $NUM_OBS; ++OBS )); do
#     OBSERVATIONS+=( "$FOLDER/$MERGED/observation$OBS.json" )
# done

# "$LOC/merge-obs-egta.py" "${OBSERVATIONS[@]}" > "$FOLDER/$MERGED/observation$((${NUM_OBS} - 1))merged.json"

echo " done"
