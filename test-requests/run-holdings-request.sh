#!/usr/bin/env bash

function die() {
   echo Error : $*
   ehco usage $0 [url] [holdings.json file]
   exit 1
}

[ "x$1" == "x" ] && die "url missing"
[ "x$2" == "x" ] && die "Missing input argumets"
[ -f $2 ] || die "'$1' Must be a file"

curl -s -d @$2 -H "Accept:application/json" -H "Content-Type:application/json" $1 | jq .
echo