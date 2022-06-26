#!/bin/bash

paraIndex="/home/sc1242/work/aspect-linking/aspect-linked-corpus-index"
catalogIndex="/home/sc1242/work/aspect-linking/catalog-index"
entityIndex="/home/sc1242/work/trec_car/index/entity.lucene"
queriesFile="/home/sc1242/work/bert_entity_ranking/dbpedia_entity_v2_car/all/data/queries.tsv"
entitiesFile="/home/sc1242/work/bert_entity_ranking/trec-car/entity-id-to-name.tsv"
stopWordsFile="/home/sc1242/work/stop-words.txt"
trainParaRunFile="train.passage.run"
trainEntityRunFile="train.entity.run"
posEntFile="train.entity.qrels"
negEntFile="train.neg_ent.txt"
testParaRunFile="test.passage.run"
testEntityRunFile="test.entity.run"
jarFile="/home/sc1242/work/bert_entity_ranking/bert_entity_ranking-1.0-SNAPSHOT-jar-with-dependencies.jar"
dataDir=$1

echo "Data Directory ==> $dataDir"
echo "===================================================================================="


BM25Psg() {

  setNum=$1
  echo "Fold: $setNum"
  parallel="true"

  for mode in "train" "test"; do
    if [[ "${mode}" == "train" ]]; then
        echo "Creating train files..."

        # Positive entity file
        posEntityFile=$dataDir/"fold-"$setNum/$posEntFile
        posOutFile=$dataDir/"fold-"$setNum/"train.pos_ent.bm25_psg.tsv"
        echo "Creating positive entity data"
        java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex "$posEntityFile" $queriesFile $entitiesFile "$posOutFile" $parallel
        echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"

        # Negative  entity file
        negEntityFile=$dataDir/"fold-"$setNum/$negEntFile
        negOutFile=$dataDir/"fold-"$setNum/"train.neg_ent.bm25_psg.tsv"
        echo "Creating negative entity data"
        java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex "$negEntityFile" $queriesFile $entitiesFile "$negOutFile" $parallel
        echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
      else
        echo "Creating test files..."
        testEntityFile=$dataDir/"fold-"$setNum/$testEntityRunFile
        outFile=$dataDir/"fold-"$setNum/"test.entity.bm25_psg.tsv"
        java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex "$testEntityFile" $queriesFile $entitiesFile "$outFile" $parallel
        echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
      fi
  done
  echo "------------------------------------------------------------------------------------------------"
}

SupportPsg() {

  setNum=$1
  echo "Fold: $setNum"
  parallel="true"

  for mode in "train" "test"; do
    if [[ "${mode}" == "train" ]]; then
          echo "Creating train files..."
          trainParaRunFilePath=$dataDir/"fold-"$setNum/$trainParaRunFile
          trainEntityRunFilePath=$dataDir/"fold-"$setNum/$trainEntityRunFile
          # Positive entity file
          echo "Creating positive entity data"
          posEntityFile=$dataDir/"fold-"$setNum/$posEntFile
          posOutFile=$dataDir/"fold-"$setNum/"train.pos_ent.support_psg.tsv"
          java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex "$trainParaRunFilePath" "$trainEntityRunFilePath" "$posEntityFile" "$posOutFile" $parallel

          # Negative  entity file
          echo "Creating negative entity data"
          negEntityFile=$dataDir/"fold-"$setNum/$negEntFile
          negOutFile=$dataDir/"fold-"$setNum/"train.neg_ent.support_psg.tsv"
          java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex "$trainParaRunFilePath" "$trainEntityRunFilePath" "$negEntityFile" "$negOutFile" $parallel
    else
          echo "Creating test files..."
          testParaRunFilePath=$dataDir/"fold-"$setNum/$testParaRunFile
          testEntityFile=$dataDir/"fold-"$setNum/$testEntityRunFile
          outFile=$dataDir/"fold-"$setNum/"test.entity.support_psg.tsv"
          java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex "$testParaRunFilePath" "$testEntityFile" "$outFile" $parallel
    fi
  done
  echo "------------------------------------------------------------------------------------------------"
}

LeadText() {

  setNum=$1
  echo "Fold: $setNum"
  parallel="true"


  for mode in "train" "test"; do
    if [[ "${mode}" == "train" ]]; then
          echo "Creating train files..."

          # Positive entity file
          echo "Creating positive entity data"
          posEntityFile=$dataDir/"fold-"$setNum/$posEntFile
          posOutFile=$dataDir/"fold-"$setNum/"train.pos_ent.lead_text.tsv"
          java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $entityIndex "$posEntityFile" "$posOutFile" $parallel

          # Negative  entity file
          echo "Creating negative entity data"
          negEntityFile=$dataDir/"fold-"$setNum/$negEntFile
          negOutFile=$dataDir/"fold-"$setNum/"train.neg_ent.lead_text.tsv"
          java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $entityIndex "$negEntityFile" "$negOutFile" $parallel
    else
          echo "Creating test files..."
          testEntityFile=$dataDir/"fold-"$setNum/$testEntityRunFile
          outFile=$dataDir/"fold-"$setNum/"test.entity.lead_text.tsv"
          java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $entityIndex "$testEntityFile" "$outFile" $parallel
    fi
  done
  echo "------------------------------------------------------------------------------------------------"
}

AspectCandidateSet() {

  takeKDocs="500"
  parallel="false"

  setNum=$1
  echo "Fold: $setNum"
  for mode in "train" "test"; do
    if [[ "${mode}" == "train" ]]; then
            echo "Creating train files..."
            trainParaRunFilePath=$dataDir/"fold-"$setNum/$trainParaRunFile
            posEntityFile=$dataDir/"fold-"$setNum/$posEntFile
            posOutFile=$dataDir/"fold-"$setNum/"train.pos_ent.aspect_cand_set.tsv"
            negOutFile=$dataDir/"fold-"$setNum/"train.neg_ent.aspect_cand_set.tsv"
            java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex $catalogIndex "$trainParaRunFilePath" "$posEntityFile" $queriesFile $stopWordsFile "$posOutFile" "$negOutFile" $takeKDocs $parallel
    else
            echo "Creating test files..."
            testParaRunFilePath=$dataDir/"fold-"$setNum/$testParaRunFile
            outFile=$dataDir/"fold-"$setNum/"test.entity.aspect_cand_set.tsv"
            java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex $catalogIndex "$testParaRunFilePath" $queriesFile $stopWordsFile "$outFile" $takeKDocs $parallel
    fi
  done
  echo "------------------------------------------------------------------------------------------------"

}

AspectSupportPsg() {

  setNum=$1
  echo "Fold: $setNum"
  parallel="true"

  for mode in "train" "test"; do
    if [[ "${mode}" == "train" ]]; then
            echo "Creating train files..."
            trainParaRunFilePath=$dataDir/"fold-"$setNum/$trainParaRunFile
            trainEntityRunFilePath=$dataDir/"fold-"$setNum/$trainEntityRunFile
            # Positive entity file
            echo "Creating positive entity data"
            posEntityFile=$dataDir/"fold-"$setNum/$posEntFile
            posOutFile=$dataDir/"fold-"$setNum/"train.pos_ent.aspect_support_psg.tsv"
            java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex $catalogIndex "$trainParaRunFilePath" "$trainEntityRunFilePath" "$posEntityFile" "$posOutFile" $parallel

            # Negative  entity file
            echo "Creating negative entity data"
            negEntityFile=$dataDir/"fold-"$setNum/$negEntFile
            negOutFile=$dataDir/"fold-"$setNum/"train.neg_ent.aspect_support_psg.tsv"
            java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex $catalogIndex "$trainParaRunFilePath" "$trainEntityRunFilePath" "$negEntityFile" "$negOutFile" $parallel
    else
            echo "Creating test files..."
            testParaRunFilePath=$dataDir/"fold-"$setNum/$testParaRunFile
            testEntityFile=$dataDir/"fold-"$setNum/$testEntityRunFile
            outFile=$dataDir/"fold-"$setNum/"test.entity.aspect_support_psg.tsv"
            java -jar -Djava.util.concurrent.ForkJoinPool.common.parallelism=20 $jarFile "$mode" "$type" $paraIndex $catalogIndex "$testParaRunFilePath" "$testEntityFile" "$outFile" $parallel
    fi
  done
  echo "------------------------------------------------------------------------------------------------"

}

doTask() {
  type=$1
  if [[ "${type}" == "SupportPsg" ]]; then
    SupportPsg 0
    SupportPsg 1
    SupportPsg 2
    SupportPsg 3
    SupportPsg 4
  fi

  if [[ "${type}" == "LeadText" ]]; then
    LeadText 0
    LeadText 1
    LeadText 2
    LeadText 3
    LeadText 4
  fi

  if [[ "${type}" == "AspectSupportPsg" ]]; then
    AspectSupportPsg 0
    AspectSupportPsg 1
    AspectSupportPsg 2
    AspectSupportPsg 3
    AspectSupportPsg 4
  fi

  if [[ "${type}" == "AspectCandidateSet" ]]; then
    AspectCandidateSet 0
    AspectCandidateSet 1
    AspectCandidateSet 2
    AspectCandidateSet 3
    AspectCandidateSet 4
  fi

  if [[ "${type}" == "BM25Psg" ]]; then
    BM25Psg 0
    BM25Psg 1
    BM25Psg 2
    BM25Psg 3
    BM25Psg 4
  fi

}

main() {
  for type in "SupportPsg" "LeadText" "AspectSupportPsg" "AspectCandidateSet" "BM25Psg"; do
    echo "Type: $type"
    doTask $type
    echo "+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+"
  done
}

main




