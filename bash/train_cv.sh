#!/bin/bash

usage() {
	echo "A simple bash script to automate 5-fold CV."
	echo "usage: ./train_cv.sh [--dataset DATASET] [--use-cuda USE CUDA] [--batch-size BATCH SIZE] [--task TASK] [--folds FOLDS]"
	echo "    --dataset       DATASET         Name of dataset to use."
	echo "    --use-cuda      USE CUDA        Whether to use CUDA or not (true|false)."
	echo "    --batch-size    BATCH SIZE      Size of each batch during training."
	echo "    --task          TASK            Task for training (ranking|classification)."
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
		--dataset          )           	shift
		                        	      dataset=$1
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
trainScript="/home/sc1242/bert_entity_ranking/bert_for_ranking_code/train.py"
trainFile="train.$task.jsonl"
dataDir="/home/sc1242/bert_entity_ranking/dbpedia_entity_v2_car/all/features/bert-features/CV/$dataset/data"
outDir="/home/sc1242/bert_entity_ranking/dbpedia_entity_v2_car/all/features/bert-features/CV/$dataset/output"
qrelPath="/home/sc1242/bert_entity_ranking/dbpedia_entity_v2_car/benchmarkY1-test.qrel"
devPath="/home/sc1242/bert_entity_ranking/dbpedia_entity_v2_car/benchmarkY1-test.dev.jsonl"
vocab="bert-base-uncased"
pretrain="bert-base-uncased"
save="bert-model.bin"
outFile="dev.run"
metric="map"
evalEvery="1"
epoch="2"
maxQueryLength="10"
maxDocLength="200"
numWorkers="0"
CUDA_VISIBLE_DEVICES=0,1

echo "Dataset: $dataset"
echo "Batch Size = $batchSize"
echo "Task: $task"

for i in "${folds[@]}"; do
  echo "Set: $i"
  trainData=$dataDir/"set-"$i/$trainFile
  savePath=$outDir/"set-"$i/$task
  echo "Train data loaded from: $trainData"
  echo "Saving results to: $savePath"
  if [[ "${useCuda}" == "true" ]]; then
        python3 $trainScript --task "$task" --train "$trainData" --save-path "$savePath" --qrel $qrelPath --dev $devPath --vocab $vocab --pretrain $pretrain --save $save --out-file $outFile --metric $metric --epoch $epoch --batch-size "$batchSize" --eval-every $evalEvery --max-query-len $maxQueryLength --max-doc-len $maxDocLength --num-workers $numWorkers --use-cuda
    else
        python3 $trainScript --task "$task" --train "$trainData" --save-path "$savePath" --qrel $qrelPath --dev $devPath --vocab $vocab --pretrain $pretrain --save $save --out-file $outFile --metric $metric --epoch $epoch --batch-size "$batchSize" --eval-every $evalEvery --max-query-len $maxQueryLength --max-doc-len $maxDocLength --num-workers $numWorkers
  fi
done