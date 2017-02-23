#!/bin/bash
DIRECTORY=$(basename ${PWD});
../gradlew -c ../settings.gradle.all :$DIRECTORY:clean  :$DIRECTORY:assembleRelease :$DIRECTORY:copyReleaseApkToOutputDirs