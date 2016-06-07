#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG="ch"

TRAIN=/shared/corpora/ner/parallel/$LANG/Train-project/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-fa/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-edit/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-berk/
#TEST=/shared/corpora/ner/lorelei/$LANG/Test/
#TEST=/shared/corpora/ner/hengji/$LANG/Test/
TEST=/shared/corpora/ner/wikifier-features/zh/Test-go-4types/

CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.ACLRunner $TRAIN $TEST $LANG"


echo "$0: running command '$CMD'..."

${CMD}
