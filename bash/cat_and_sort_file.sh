#!/bin/bash

directory=$1
file=$2
outDir=$3
outFile=$outDir/$file

# Concatenate the files from different sets
cat "$directory"/set-0/"$file" "$directory"/set-1/"$file" "$directory"/set-2/"$file" "$directory"/set-3/"$file" "$directory"/set-4/"$file" > "$outFile"

# Sort the resulting file and rename
sort -u "$outFile" > deduplicated.tsv

# Clean up
rm "$outFile"
mv deduplicated.tsv "$outFile"

# We want only queries which have at least 10 entities in the description file
# Write the names of such queries to a file
awk -F '\t' '{print $1}' "$outFile" | sort | uniq -c | sort -nr | awk '$1 >= 10' | awk '{print $2}' > names.txt

# Now retain only queries in that file
grep -Fw -f names.txt "$outFile" > filtered.tsv

# Clean up
rm "$outFile"
mv filtered.tsv "$outFile"

echo "[COMPLETE] File written to: $outFile"


