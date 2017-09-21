#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

#TRAIN=/shared/corpora/ner/conll2003/eng/Train/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-bootstrap/
#TRAIN=/shared/corpora/ner/lorelei/$LANG/All/
#TRAIN=/shared/corpora/ner/eval/column/dev2/
#TRAIN=/shared/corpora/ner/eval/column/trainset3000/
#TRAIN=/shared/corpora/ner/lorelei/$LANG/All-out/
#TRAIN=/shared/corpora/ner/eval/column/dev-iter13-stem-uly-swmcorrect/
#TRAIN=out/
#TRAIN=out-fs/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-fa/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-berk/
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-morph-fix/

#TEST=/shared/corpora/ner/lorelei/$LANG/Test/
#TEST=/shared/corpora/ner/eval/column/dev-iter13-stem-uly-swmcorrect/
#TEST=/shared/corpora/ner/eval/column/dev2/
#TEST=/shared/corpora/ner/wikifier-features/zh/Test-go-4types/

LANG=am
PRED=/shared/corpora/corporaWeb/lorelei/data/LDC2016E86_LORELEI_Amharic_Representative_Language_Pack_Monolingual_Text_V1.1/data/monolingual_text/zipped/dryrun-outputs/combined/column/

#/tmp/all-anno-comb/
#PRED=/tmp/tt
GOLD=/shared/corpora/ner/lorelei/am/All-nosn-stem/


CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.ACLRunner $PRED $GOLD $LANG"


echo "$0: running command '$CMD'..."

${CMD}
