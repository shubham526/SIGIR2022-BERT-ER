#!/bin/bash

usage() {
	echo
	echo "Run the python script \"sort_dbpedia_run.py\" on a directory of run files at once."
	echo "NOTE: The python script must be in the same directory from where you run this bash script."
	echo "usage: ./run_sort_dbpedia_run.sh [-d | --dir RUN DIR] [-q | --qrel QREL] [-s | --save SAVE DIR]"
	echo "    -d | --dir       RUN DIR        Path to directory containing run files."
	echo "    -q | --qrel      QREL           Path to the ground truth file."
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

		-q | --qrel )                 shift
						                      qrel=$1
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

for filename in "$dir"/*
do
	file=${filename##*/}
	echo "File: $file"
	python3 sort_dbpedia_run.py --run "$filename" --qrels "$qrel" --save "$save"/"$file"
	echo "========================================================="
done

