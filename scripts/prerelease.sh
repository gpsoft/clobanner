#!/usr/bin/sh

if [ -n "$(git status --porcelain)" ]; then
    echo "Error: working directory is not clean."
    exit -1
fi

if [ ! -f resources/public/js/clobanner.js ]; then
    echo "Error: product is not build."
    exit -2
fi
