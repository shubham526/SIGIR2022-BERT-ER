#!/bin/bash

rankLips="/home/sc1242/work/"
qrelFile="/home/sc1242/work/bert_entity_ranking/dbpedia_car/all/data/pos_ent.qrels"
miniBatchSize=3000
convergenceThreshold=0.001
folds=5
restarts=10
threads=20

mainDir=$1

for d in "$mainDir"/*/; do
  echo "Directory: $d"
  runs=$d/"runs"
  $rankLips/rank-lips train -d "$runs" -q $qrelFile -e L2R -O "$d" -o L2R --z-score --mini-batch-size $miniBatchSize --convergence-threshold $convergenceThreshold --folds $folds --restarts $restarts --threads $threads --train-cv
  echo "=========================================================================="
done