#!/bin/bash

usage() {
	echo "A simple bash script to automate 5-fold CV."
	echo "usage: ./train_cv.sh [--use-cuda USE CUDA] [--batch-size BATCH SIZE] [--folds FOLDS]"
	echo "    --use-cuda      USE CUDA        Whether to use CUDA or not (true|false)."
	echo "    --batch-size    BATCH SIZE      Size of each batch during training."
	echo "    --epoch         EPOCH           Number of epochs."
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

		--use-cuda         )            shift
		                        	      useCuda=$1
		                        	      ;;
		--batch-size       )            shift
    		                        	  batchSize=$1
    		                        	  ;;

    --epoch       )                 shift
        		                        epoch=$1
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
trainScript="/home/sc1242/neural_entity_ranking/code/train.py"
dataDir="/home/sc1242/neural_entity_ranking/car/benchmarkY1-train/data"
outDir="/home/sc1242/neural_entity_ranking/car/benchmarkY1-train/checkpoints"
CUDA_VISIBLE_DEVICES=0,1

echo "Batch Size = $batchSize"
echo "Epochs = $epoch"


for mode in "pairwise" "pointwise"; do
  echo "Mode: $mode"
  trainFile="train.$mode.jsonl"
  echo
  for i in "${folds[@]}"; do
    echo "Set: $i"
    trainData=$dataDir/"set-"$i/$trainFile
    devData=$dataDir/"set-"$i/"test.jsonl"
    devQrels=$dataDir/"set-"$i/"test.entity.qrels"
    savePath=$outDir/$mode/"set-"$i
    echo "Loading train data from ==> $trainData"
    echo "Loading dev data from ==> $devData"
    echo "Saving results to ==> $savePath"
    if [[ "${useCuda}" == "true" ]]; then
          python3 $trainScript --model-name bert --model-type $mode --train "$trainData" --dev "$devData" --qrels "$devQrels" --save-dir "$savePath" --run "test.run" --save "model.bin" --epoch "$epoch" --emb-dim 100 --batch-size "$batchSize" --combination "bilinear" --score-method "bilinear" --use-cuda --cuda 1
    else
          python3 $trainScript --model-name bert --model-type $mode --train "$trainData" --dev "$devData" --qrels "$devQrels" --save-dir "$savePath" --run "test.run" --save "model.bin" --epoch "$epoch" --emb-dim 100 --batch-size "$batchSize" --combination "bilinear" --score-method "bilinear"
    fi
    echo
  done
done
