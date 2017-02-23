#!/bin/bash
DIRECTORY=$(basename ${PWD});
../gradlew :$DIRECTORY:clean  :$DIRECTORY:assembleRelease :$DIRECTORY:copyReleaseApkToOutputDirs