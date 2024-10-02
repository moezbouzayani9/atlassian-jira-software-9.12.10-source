#! /bin/sh
cd "`dirname "$0"`"
SETTINGSFILE="settings.xml"
LOCALREPO="localrepo"
set -e
export PATH=.:$PATH
mvn388.sh clean install -f jira-project/pom.xml -Pbuild-from-source-dist -Dmaven.test.skip -DskipTests -Dmaven.test.skip -s $SETTINGSFILE -Dmaven.repo.local="`pwd`/$LOCALREPO" "$@"
mvn388.sh clean install -f jira-project/jira-components/jira-webapp/pom.xml -Pbuild-from-source-dist -Dmaven.test.skip -DskipTests -Dmaven.test.skip -s $SETTINGSFILE -Dmaven.repo.local="`pwd`/$LOCALREPO" "$@"
mvn388.sh clean package -f jira-project/jira-distribution/jira-webapp-dist/pom.xml -Pbuild-from-source-dist -Dmaven.test.skip -DskipTests -Dmaven.test.skip -s $SETTINGSFILE -Dmaven.repo.local="`pwd`/$LOCALREPO" "$@"
