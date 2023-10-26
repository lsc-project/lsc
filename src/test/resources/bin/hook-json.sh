#!/bin/bash

# Parse arguments
for (( i=1; i <= $#; i++ )); do
  eval arg='$'$i
  echo $arg >> hook-json-$2.log
done

# Parse input
while IFS= read line; do
echo "$line" >> hook-json-$2.log
done

