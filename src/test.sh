#!/bin/bash
SERVER_CLASS=$1
CLIENT_CLASS=$2
OUTPUT_FILE=output/$3
mkdir output
touch $OUTPUT_FILE
echo "# this is the output for $SERVER_CLASS" > $OUTPUT_FILE
( java $SERVER_CLASS $OUTPUT_FILE &  )
for (( FILE_SIZE=1; FILE_SIZE<=3000001; FILE_SIZE+=50000 ))
do
  printf "$FILE_SIZE " >> $OUTPUT_FILE
  dd if=/dev/zero of=dummy.txt bs=$FILE_SIZE count=1
  java $CLIENT_CLASS dummy.txt
done
rm dummy.txt
kill $(ps aux | grep $1 | awk '{print $2}')
  