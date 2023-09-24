#!/bin/bash
set -ev

GRADLE_COMMAND='./gradlew --info gitPublishPush --rerun-tasks'

if [ "${GITHUB_REF}" == "refs/heads/master" ]; then
  if [ "$(git ls-remote origin gh-pages)" == "" ]; then
    echo Start gitPublishPush with ghPageType=init
    $GRADLE_COMMAND -PghPageType=init
    echo Finished gitPublishPush with ghPageType=init
  fi
  echo Start gitPublishPush with ghPageType=latest
  $GRADLE_COMMAND -PghPageType=latest
  echo Finished gitPublishPush with ghPageType=version

  echo Start gitPublishPush with ghPageType=version
  $GRADLE_COMMAND -PghPageType=version
  echo Finished gitPublishPush with ghPageType=version

  echo Start updating releases.md
  $GRADLE_COMMAND -PghPageType=list
  echo Finished updating releases.md
fi
