#!/usr/bin/env bash

CONTENT_TYPE=application/octet-stream
FILENAME=$1
MD5=`md5 -q $FILENAME`
TARGET=$2
IN_PROGRESS=$3
SLUG=$4
USERNAME=user001
PASSWORD=user001

if [ -f vars.sh ]
then
    . vars.sh
fi

curl -v -H "Content-Type: $CONTENT_TYPE"  -H "Slug: $SLUG"  -H "Content-Disposition: attachment; filename=$FILENAME" -H "Packaging: http://purl.org/net/sword/package/BagIt" -H "Content-MD5: $MD5"  -H "In-Progress: $IN_PROGRESS" -i -u $USERNAME:$PASSWORD  --data-binary @"$FILENAME"  $TARGET

