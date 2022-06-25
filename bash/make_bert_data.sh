#!/bin/bash

trainScript="/home/sc1242/work/PyCharmProjects/entity_ranking_bert/venv/src/make_train_data.py"
testScript="/home/sc1242/work/PyCharmProjects/entity_ranking_bert/venv/src/make_dev_or_test_data.py"
queriesFile="/home/sc1242/work/bert_entity_ranking/dbpedia_entity_v2_car/all/data/queries.tsv"
mainDir=$1

makeTrainData() {
  k=30 # We use 30 entities each for positive and negative

  task=$1
  posEntDataFile=$2
  negEntDataFile=$3
  save=$4
  python3 "$trainScript" --type "$task" --pos-ent-data "$posEntDataFile" --neg-ent-data "$negEntDataFile" --k "$k" --queries "$queriesFile" --save-dir "$save"
}

makeTestData() {
  mode="test"
  entityDataFile=$1
  qrelFile=$2
  save=$3
  python3 "$testScript" --entity-data "$entityDataFile" --queries "$queriesFile" --qrel "$qrelFile" --mode "$mode" --save-dir "$save"
}

runMakeTrainData() {

  for task in "pairwise" "pointwise"; do
    posEntityDataFilePath=$mainDir/"train_test_sets/set-"$1/$2
    negEntityDataFilePath=$mainDir/"train_test_sets/set-"$1/$3
    saveDirPath=$mainDir/$4/"data/set-"$1
    makeTrainData $task "$posEntityDataFilePath" "$negEntityDataFilePath" "$saveDirPath"
  done
}

runMakeTestData() {

  entityDataFilePath=$mainDir/"train_test_sets/set-"$1/$2
  qrelFilePath=$mainDir/"train_test_sets/set-"$1/"test.entity.qrels"
  saveDirPath=$mainDir/$3/"data/set-"$1
  makeTestData "$entityDataFilePath" "$qrelFilePath" "$saveDirPath"
}

doTask() {
  posEntityDataFile=$1
  negEntityDataFile=$2
  testEntityDataFile=$3
  saveDir=$4

  for setNum in "0" "1" "2" "3" "4"; do
    echo "Set: $setNum"
    # Make train data
    echo "Creating train data..."
    runMakeTrainData $setNum "$posEntityDataFile" "$negEntityDataFile" "$saveDir"

    # Make test data
    echo "Creating test data..."
    runMakeTestData $setNum "$testEntityDataFile" "$saveDir"
  done
}

run() {
  type=$1
   if [[ "${type}" == "SupportPsg" ]]; then
            posEntityDataFile="train.pos_ent.support_psg.tsv"
            negEntityDataFile="train.neg_ent.support_psg.tsv"
            testEntityDataFile="test.entity.support_psg.tsv"
            saveDir="SupportPsg"
            doTask $posEntityDataFile $negEntityDataFile $testEntityDataFile $saveDir
    fi

    if [[ "${type}" == "LeadText" ]]; then
            posEntityDataFile="train.pos_ent.lead_text.tsv"
            negEntityDataFile="train.neg_ent.lead_text.tsv"
            testEntityDataFile="test.entity.lead_text.tsv"
            saveDir="LeadText"
            doTask $posEntityDataFile $negEntityDataFile $testEntityDataFile $saveDir
    fi

    if [[ "${type}" == "AspectSupportPsg" ]]; then
            posEntityDataFile="train.pos_ent.aspect_support_psg.tsv"
            negEntityDataFile="train.neg_ent.aspect_support_psg.tsv"
            testEntityDataFile="test.entity.aspect_support_psg.tsv"
            saveDir="AspectSupportPsg"
            doTask $posEntityDataFile $negEntityDataFile $testEntityDataFile $saveDir
    fi

    if [[ "${type}" == "AspectCandidateSet" ]]; then
            posEntityDataFile="train.pos_ent.aspect_cand_set.tsv"
            negEntityDataFile="train.neg_ent.aspect_cand_set.tsv"
            testEntityDataFile="test.entity.aspect_cand_set.tsv"
            saveDir="AspectCandidateSet"
            doTask $posEntityDataFile $negEntityDataFile $testEntityDataFile $saveDir
    fi

    if [[ "${type}" == "BM25Psg" ]]; then
            posEntityDataFile="train.pos_ent.bm25_psg.tsv"
            negEntityDataFile="train.neg_ent.bm25_psg.tsv"
            testEntityDataFile="test.entity.bm25_psg.tsv"
            saveDir="BM25Psg"
            doTask $posEntityDataFile $negEntityDataFile $testEntityDataFile $saveDir
    fi
}

main() {
  for type in "SupportPsg" "LeadText" "AspectSupportPsg" "AspectCandidateSet" "BM25Psg"; do
    echo "Type: $type"
    run $type
    echo "+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+"
  done
}

main
