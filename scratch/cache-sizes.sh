#!/usr/bin/env bash

# Reports the size of the caches the build restores, to help judge whether the
# GitHub Actions cache entries are worth their eviction pressure.

DIR=$1

echo "Cache sizes under $DIR:"
du -sh $DIR/*

TOTAL=`du -sm $DIR | cut -f1`

if [ $TOTAL > 500 ]; then
  echo "warning: caches exceed 500MB"
fi
