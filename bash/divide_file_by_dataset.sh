#!/bin/bash

mainDir="/home/sc1242/work/bert_entity_ranking/dbpedia_car"
fromDir=$1
dir="$mainDir"/all/"$fromDir"

usage() {
	echo
	echo "Divide the DBpedia runs by dataset."
	echo "Usage: ./divide.sh [-d | --dir RUN DIR] [-s | --save SAVE DIR]"
	echo "    -d | --dir       RUN DIR        Path to directory containing run files."
	echo "    -s | --save      SAVE DIR       Path to the directory where new runs would be saved."
}



if [ "$#" -eq 0 ]; then
   	usage
	  exit 1
fi
# Get the command line parameters

while [ "$1" != "" ];
do
	    case $1 in
		-d | --dir )              	  shift
						                      dir=$1
		                        	    ;;


    -s | --save )                 shift
    						                  save=$1
    		                        	;;

		-h | --help )           	    usage
		                        	    exit
		                        	    ;;


	    esac
	    shift
done


for fileName in "$dir"/*
do
	echo "$fileName"
	file=${fileName##*/}

	echo "File: $file"

	## INEX_LD
	echo "Doing: INEX_LD"
	toFile="INEX_LD"_${file}
	# shellcheck disable=SC2086
	grep  INEX_LD "$dir"/$file > $save/$toFile
	echo "[Done.]"


	## SemSearch_ES
	echo "Doing: SemSearch_ES"
	toFile="SemSearch_ES"_${file}
	grep  SemSearch_ES "$dir"/"$file" > "$save"/"$toFile"
	echo "[Done.]"


	## QALD2
	echo "Doing: QALD2"
	toFile="QALD2"_${file}
	grep  QALD2 "$dir"/"$file" > "$save"/"$toFile"
	echo "[Done.]"

	## ListSearch
	echo "Doing: ListSearch"
	toFile="ListSearch"_${file}

  ## INEX_XER queries
	grep  INEX_XER "$dir"/"$file" > "$save"/1.run
  ## SemSearch_LS queries
	grep  SemSearch_LS "$dir"/"$file" > "$save"/2.run
	## TREC_Entity queries
  grep  TREC_Entity "$dir"/"$file" > "$save"/3.run
  ## Concatenate files
	cat "$save"/1.run "$save"/2.run "$save"/3.run > "$save"/"$toFile"
  ## Remove temporary files
	rm "$save"/1.run
	rm "$save"/2.run
	rm "$save"/3.run

	echo "[Done.]"
	echo "==============================================================================="

done
