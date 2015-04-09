#!/bin/bash
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
FILE=`date --iso-8601=seconds`.json
echo writing to $DIR/../dumps/$FILE
mkdir -p $DIR/../dumps/
redis-dump -u localhost:6379 > $DIR/../dumps/$FILE
