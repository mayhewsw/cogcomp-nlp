#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG="uzug"

TRAIN=/shared/corpora/ner/lorelei/$LANG/All-out/
TEST=/shared/corpora/ner/eval/column/dev2/
CONFIG=config/uzug.config

CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.ACLRunner $TRAIN $TEST $CONFIG"


echo "$0: running command '$CMD'..."

${CMD}
