


#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

TRAIN=$1
INPUT=$2
OUTPUT=$3
LANG=$4

OUTF=logs/$LANG-`date +%m-%d-%y-%H%M`-codl.log
echo "Logging to:" $OUTF

CONFIG=config/tacl/$LANG.config

#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-0.25-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-anno-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-anno-gaz
#TRAIN=/shared/corpora/ner/human/$LANG/conll-anno-combine1-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/CheapTrain
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-mention
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-0.25-weighted/
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-tmp-weighted
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-0.25-weighted

#INPUT=/shared/corpora/ner/conll2003/eng/Train-0.25-mention
#OUTPUT=/shared/corpora/ner/conll2003/eng/Train-mention
#OUTPUT=/tmp/

CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.CODLRunner -train $TRAIN -input $INPUT -output $OUTPUT -cf $CONFIG -lang $LANG"

alias s="tail -f ${OUTF}"

echo $TRAIN >> $OUTF
datastats.py $TRAIN >> $OUTF

ls $TRAIN >> $OUTF


echo "$0: running command '$CMD'..." >> $OUTF

${CMD} >> $OUTF
