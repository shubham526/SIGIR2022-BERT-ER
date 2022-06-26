#!/bin/bash

usage() {
	echo "A simple bash script to automate 5-fold CV for training."
	echo "usage: ./train_cv.sh [--data DATA] [--save SAVE] [--use-cuda USE CUDA] [--batch-size BATCH SIZE] [--model MODEL] [--folds FOLDS]"
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

    --model             )           shift
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
trainScript="/home/sc1242/bert_entity_ranking/bert_for_ranking_code/train.py"
trainFile="train.$task.jsonl"
qrelPath="/home/sc1242/bert_entity_ranking/dbpedia_entity_v2_car/benchmarkY1-test.qrel"
devPath="/home/sc1242/bert_entity_ranking/dbpedia_entity_v2_car/benchmarkY1-test.dev.jsonl"
save="bert-model.bin"
outFile="dev.run"
metric="map"
evalEvery="1"
epoch="2"
numWorkers="0"
CUDA_VISIBLE_DEVICES=0,1

echo "Data Directory: $dataDir"
echo "Batch Size = $batchSize"
echo "Model Type: $task"

for i in "${folds[@]}"; do
  echo "Fold: $i"
  trainData=$dataDir/"fold-"$i/$trainFile
  savePath=$outDir/"fold-"$i/$task
  echo "Train data loaded from ==> $trainData"
  echo "Saving results to ==> $savePath"
  if [[ "${useCuda}" == "true" ]]; then
        python3 $trainScript --model-type "$task" --train "$trainData" --save-dir "$savePath" --qrels $qrelPath --dev $devPath --save $save --run $outFile --metric $metric --epoch $epoch --batch-size "$batchSize" --eval-every $evalEvery --num-workers $numWorkers --use-cuda
    else
        python3 $trainScript --model-type "$task" --train "$trainData" --save-dir "$savePath" --qrels $qrelPath --dev $devPath --save $save --run $outFile --metric $metric --epoch $epoch --batch-size "$batchSize" --eval-every $evalEvery --num-workers $numWorkers
  fi
done
