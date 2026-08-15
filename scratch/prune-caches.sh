#!/usr/bin/env bash

# Deletes GitHub Actions cache entries older than a given number of days.

set -euo pipefail

DAYS=$1
CUTOFF=$(date -v-"$DAYS"d +%s)

gh api repos/dhpiggott/plutus/actions/caches --jq '.actions_caches[] | "\(.id) \(.created_at)"' |
  while read -r id created; do
    if [ $(date -j -f "%Y-%m-%dT%H:%M:%SZ" "$created" +%s) -lt $CUTOFF ]; then
      gh api -X DELETE repos/dhpiggott/plutus/actions/caches/$id
      echo "deleted $id"
    fi
  done
