#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG=$1

OUTF=logs/$LANG-`date +%m-%d-%y-%H%M`-LR.log
echo "Logging to:" $OUTF

#TRAIN=/shared/corpora/ner/translate/eng/Train-ta3/
#TRAIN=/shared/corpora/ner/translate/$LANG/Train-translit
#TRAIN=/shared/corpora/ner/translate/$LANG/Train
#TRAIN=/shared/corpora/ner/wikifier-features/$LANG/train-camera3/
#TRAIN=/shared/corpora/ner/conll2003/eng/Train/
#TRAIN=/shared/corpora/ner/hengji/$LANG/10k/1rl-mayhew2
#/shared/corpora/ner/human/$LANG/conll-anno-bbailey3-annogold
#TRAIN=/shared/corpora/ner/human/$LANG/conll-anno-bbailey3-stem
#TRAIN=/tmp/tt2-anno
TRAIN=/shared/corpora/ner/human/$LANG/conll-anno-mayhew2-upennstem
#TRAIN=/shared/corpora/ner/eval/column/trainset3000/
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-upennstem/
#TRAIN=/shared/corpora/ner/translate/$LANG/Train/
#TRAIN=/shared/corpora/ner/lorelei/uzug/All-lm2/
#TRAIN=/shared/corpora/ner/eval/column/dev2/
#TRAIN=/shared/corpora/ner/parallel/$LANG/Train-fa/
#TRAIN=/shared/corpora/ner/parallel/$LANG/SmallTrain-fa/

CONFIG=config/tacl/$LANG.config
#CONFIG=config/tacl/$LANG-good.config
#CONFIG=config/self-train.config
#CONFIG=config/hash.config

#TEST=/shared/corpora/ner/conll2003/eng/Test/
#TEST=/home/mayhew2/IdeaProjects/illinois-cogcomp-nlp/ner/HashTest/
#TEST=/shared/corpora/ner/conll2002/nl/Test/
#TEST=/shared/corpora/ner/conll2002/es/Test/
#TEST=/shared/corpora/ner/hengji/$LANG/Test
TEST=/shared/corpora/ner/lorelei/$LANG/All-upennstem
#TEST=/shared/corpora/ner/lorelei/$LANG/All-stem
#TEST=/shared/corpora/ner/wikifier-features/$LANG/test-camera3
#TEST=/shared/corpora/ner/parallel/$LANG/Test-fa/
#TEST=/shared/corpora/ner/lorelei/tr/Test-urom/

CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -lang $LANG"
#CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -test $TEST -cf $CONFIG -lang $LANG"

echo $TRAIN >> $OUTF
getstats.py $TRAIN >> $OUTF

ls $TRAIN >> $OUTF


echo "$0: running command '$CMD'..." >> $OUTF

${CMD} >> $OUTF
