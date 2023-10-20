#!/bin/bash
for (( i=1; i <= $#; i++ )); do
  eval arg='$'$i
  echo $arg >> hook.log
done
