#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG="tr"

#TRAIN=/shared/corpora/ner/conll2003/eng/Train/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-bootstrap/
TRAIN=/shared/corpora/ner/parallel/$LANG/Train-edit/,out-fs/
#TRAIN=out/
#TRAIN=out-fs/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-fa/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-berk/
#TEST=/shared/corpora/ner/lorelei/$LANG/Test/
TEST=/shared/corpora/ner/hengji/$LANG/Test/
#TEST=/shared/corpora/ner/wikifier-features/zh/Test-go-4types/

CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.ACLRunner $TRAIN $TEST $LANG"


echo "$0: running command '$CMD'..."

${CMD}
