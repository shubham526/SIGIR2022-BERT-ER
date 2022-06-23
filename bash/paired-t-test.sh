#!/bin/bash

usage()
{
	echo
	echo "A simple bash script to run pairt-t-test from minir-plots."
	echo "usage: ./paired-t-test.sh [--trec-eval TREC EVAL] [--minir-plots MINIR PLOTS] [--qrel QREL] [--dir RUNDIR] [--output OUTPUT] [--baseline BASELINE]"
	echo "    --trec-eval     TREC EVAL      Path to directory containing trec_eval on the system."
	echo "    --minir-plots   MINIR PLOTS    Path to minir-plots."
	echo "    --qrel          QREL           Path to the ground truth file."
	echo "    --dir           RUNDIR         Path to directory containing run files."
	echo "    --output        OUTPUT         Path to the output file."
	echo "    --baseline      BASELINE       Name of baseline file to use."
}


if [ "$#" -eq 0 ]; then
   	usage
	exit 1
fi
# Get the command line parameters

while [ "$1" != "" ];
do
	    case $1 in
		      --trec-eval )       shift
		                        	trec_eval=$1
		                        	;;

		      --minir-plots )     shift
          		                minir_plots=$1
          		                ;;

		      --qrel )             shift
						                  qrel=$1
		                        	;;

		      --dir )              shift
						                  dir=$1
		                        	;;

		      --output )           shift
					                    output=$1
		                          ;;

		      --baseline )       shift
          		               baseline=$1
          		                ;;


		-h | --help )             usage
		                          exit
		                          ;;


	    esac
	    shift
done



for fileName in "$dir"/*
do
	file=${fileName##*/}
	"$trec_eval"/trec_eval -c -q -m map "$qrel" "$fileName" >> "$output"/"$file"
done

python3 $minir_plots/pairedttest.py --metric map --format



echo "Done."



