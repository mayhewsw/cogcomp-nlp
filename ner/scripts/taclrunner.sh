#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

if [ $# -lt 1 ]; then
    echo Usage: `basename $0` lang 1>&2
    exit 1
fi

LANG=$1
OUTF=logs/$LANG-`date +%m-%d-%y-%H%M`.log

echo "Logging to:" $OUTF

TRAIN=/shared/corpora/ner/translate/$LANG/Train
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-nomisc-$LANG/

if [ ! -d "$TRAIN" ]; then
  TRAIN=/shared/corpora/ner/translate/$LANG/Train/
  #TRAIN=/shared/corpora/ner/conll2003/eng/Train-nomisc/
fi

ls $TRAIN >> $OUTF

CONFIG=config/tacl/$LANG.config

TEST=/shared/corpora/ner/wikifier-features/$LANG/test-camera3
if [ "$LANG" = "yo" ]; then
    echo "USING YORUBA test-camera3-swm";
    TEST=/shared/corpora/ner/wikifier-features/$LANG/test-camera3-swm
fi

if [ "$LANG" = "ug" ]; then
    echo "USING UYGHUR test data";
    TEST=/shared/corpora/ner/lorelei/ug/All-stem/
fi

if [ "$LANG" = "th" ]; then
    echo "USING THAI test data";
    TEST=/shared/corpora/ner/hengji/th/Test/
fi

#if [ "$LANG" = "tr" ]; then
#    echo "USING TURKISH test-camera3-swm";
#    TEST=/shared/corpora/ner/wikifier-features/$LANG/test-camera3-swm
#fi



CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -lang $LANG"

echo "$0: running command '$CMD'..." >> $OUTF

${CMD} >> $OUTF
