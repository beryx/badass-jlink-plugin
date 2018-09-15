#!/bin/bash
set -ev
./gradlew $GRADLE_BUILD_OPTS build asciidoc

if [ "${TRAVIS_PULL_REQUEST}" == "false" -a "${TRAVIS_BRANCH}" == "master" ]; then
  if [ "`git ls-remote origin gh-pages`" == "" ]; then
    echo Start gitPublishPush with ghPageType=init
    ./gradlew --no-daemon -i -s gitPublishPush --rerun-tasks -PghPageType=init
    echo Finished gitPublishPush with ghPageType=init
  fi
    echo Start gitPublishPush with ghPageType=latest
  ./gradlew --no-daemon -i -s gitPublishPush --rerun-tasks -PghPageType=latest
    echo Finished gitPublishPush with ghPageType=version

    echo Start gitPublishPush with ghPageType=version
  ./gradlew --no-daemon -i -s gitPublishPush --rerun-tasks -PghPageType=version
    echo Finished gitPublishPush with ghPageType=version
fi
