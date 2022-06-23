#!/bin/bash

trainType=$1

bertModel="distill_bert"
dataDir="/content/drive/MyDrive/research/create_contextual_entity_embeddings/data"
saveDir="/content/drive/MyDrive/research/create_contextual_entity_embeddings/output"
checkpointDir="/content/drive/MyDrive/research/create_contextual_entity_embeddings/checkpoints"
script="/content/drive/MyDrive/research/create_contextual_entity_embeddings/code/create_embeddings.py"

for dataSet in "train-small" "test" "validation"; do
  for descType in "bm25" "lead-text" "support-psg"; do
    for entityType in "context" "aspect"; do
      data="$dataDir/$dataSet.$descType.jsonl"
      save="$saveDir/$trainType/$dataSet/$entityType.$descType.pt"
      checkpoint="$checkpointDir/$trainType/model.bin"
      echo "Loading data from ==> $data"
      echo "Loading checkpoint from ==> $checkpoint"
      echo "Saving output to ==> $save"
      python3 $script --bert-model $bertModel --data $data --entity-type $entityType --save $save --checkpoint $checkpoint --use-cuda
      echo "============================================================================="
    done
  done
done

