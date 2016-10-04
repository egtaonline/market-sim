#!/bin/bash
# Executes a different experiment for each sim spec file * presets

IFS='
'
SPEC_FILE=simulation_spec.json
LOGDIR=logs

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] output-directory num-obs input-directory input-directory [input-directory ...]"
    echo
    echo "Merges the log files of several directories with comparable settings e.g. same simulation_spec.json but different presets, and stores them in the output directory. It is imperative that all input-directories have the same number of simulations." | fold -s
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
NUM_SIMS=$( "$LOC/jpath.py" -i "${INPUTS[0]}/$SPEC_FILE" configuration numSims | tr -d '"' )

mkdir -vp "$OUTPUT/$LOGDIR"

echo -n ">> Merging the log files..."

for (( OBS=0; OBS < $NUM_OBS; ++OBS )); do
    for (( SIM=0; SIM < $NUM_SIMS; ++SIM )); do
	LOGS=()
	for INPUT in "${INPUTS[@]}"; do
	    # NOTE: There may be a better way to grab the most recent / best log file
	    LOGS+=( $( ls -1d "$INPUT/$LOGDIR/"*"_${OBS}_${SIM}_"*".txt" | sort | tail -n1 ) )
	done
	"$LOC/merge-logs.py" "${LOGS[@]}" > "$OUTPUT/$LOGDIR/$( echo $OUTPUT | tr '/' '_' | tr -d '.' | sed 's/__*/_/g' )_${OBS}_${SIM}_$( date '+%Y-%m-%d-%H-%M-%S' ).txt"
    done
done
echo " done"
