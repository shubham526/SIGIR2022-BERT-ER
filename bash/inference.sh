#!/bin/bash

usage() {
	echo "A simple bash script to automate inference on 5-fold CV trained models."
	echo "usage: ./inference_cv.sh [--data DATA] [--save SAVE] [--use-cuda USE CUDA] [--batch-size BATCH SIZE] [--model MODEL] [--folds FOLDS]"
	echo "    --data          DATASET         Path to data directory."
  echo "    --save          SAVE            Path to output directory."
	echo "    --use-cuda      USE CUDA        Whether to use CUDA or not (true|false)."
	echo "    --batch-size    BATCH SIZE      Size of each batch during training."
	echo "    --model         MODEL           Type of model (pairwise|pointwise)."
	echo "    --folds         FOLDS           Folds to consider (Type one after another like 0 1 2 ...). Defaults to all folds if absent."
}

if [ "$#" -eq 0 ]; then
   	usage
	  exit 1
fi
# Get the command line parameters

while [ "$1" != "" ];
do
	    case $1 in
		--dataDir          )           	shift
    		                        	  dataDir=$1
    		                        	  ;;

    --save          )           	  shift
        		                        outDir=$1
        		                        ;;

		--use-cuda         )            shift
		                        	      useCuda=$1
		                        	      ;;

		--batch-size       )            shift
    		                        	  batchSize=$1
    		                        	  ;;

    --task             )            shift
        		                        task=$1
        		                        ;;
    --folds            )            shift
    						                    folds=( "$@" )
    						                    ;;

		-h | --help        )           usage
		                               exit
		                               ;;


	    esac
	    shift
done

if [ ${#folds[@]} -eq 0 ]; then
    echo "Folds not specified. Using all folds."
    folds=( "0" "1" "2" "3" "4" )
fi

# Arguments
inferenceScript="/home/sc1242/bert_entity_ranking/bert_for_ranking_code/train.py"
testFile="test.jsonl"
checkpoint="bert-model.bin"
outFile="test.run"
numWorkers="0"

CUDA_VISIBLE_DEVICES=0,1

echo "Data Directory: $dataDir"
echo "Batch Size = $batchSize"
echo "Model Type: $task"

for i in "${folds[@]}"; do
  echo "Set: $i"
  testData=$dataDir/"set-"$i/$testFile
  savePath=$outDir/"set-"$i/$task
  checkpointPath=$outDir/"set-"$i/$task/$checkpoint
  echo "Test data loaded from ==> $testData"
  echo "Checkpoint loaded from ==> $checkpointPath"
  echo "Saving results to ==> $savePath"
  if [[ "${useCuda}" == "true" ]]; then
        python3 $inferenceScript --model-type "$task" --test "$testData" --save-dir "$savePath" --checkpoint "$checkpointPath" --rune $outFile --batch-size "$batchSize" --num-workers $numWorkers --use-cuda
    else
        python3 $inferenceScript --model-type "$task" --test "$testData" --save-dir "$savePath" --checkpoint "$checkpointPath" --rune $outFile --batch-size "$batchSize" --num-workers $numWorkers

  fi
done
