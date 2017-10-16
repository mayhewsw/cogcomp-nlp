#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG=$1

OUTF=logs/$LANG-`date +%m-%d-%y-%H%M`-LR.log
echo "Logging to:" $OUTF


### IL5
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il5/set0/data/REFLEX_LCTL_IL5_v1.0/Tools/NE_Tagger/lctl_ner_tigrinya.v1.0/data/train-files/
#TRAIN=$TRAIN,/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il5/set0/data/REFLEX_LCTL_IL5_v1.0/Tools/NE_Tagger/lctl_ner_tigrinya.v1.0/data/test-files/
#TRAIN=$TRAIN,/shared/corpora/ner/parallel/$LANG/ti-conll-final2/
#TRAIN=$TRAIN,/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il5/set1/data/twitter/conll-final/
#TRAIN=$TRAIN,/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il5/dev/conll/
#TRAIN=$TRAIN,/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il5/sntrain/
#TRAIN=$TRAIN,/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il5/set2/data/monolingual_text/Train/

### IL6
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/final-steve
#TRAIN=$TRAIN,/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/set1/data/monolingual_text/conll-anno-nodev-annotation-steve
#TRAIN=$TRAIN,/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/set1/data/monolingual_text/conll-anno-nodev2
#TRAIN=$TRAIN,/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/set1/data/twitter/conll-anno-annotation-steve
#TRAIN=$TRAIN,/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/dev/conll

TRAIN=/scratch/mayhew2/python-translate/$LANG/Train/$LANG.conll

## OLD
#TRAIN=/shared/corpora/ner/translate/eng/Train-ta3/
#TRAIN=/shared/corpora/ner/translate/$LANG/Train-translit,
#TRAIN=/shared/corpora/ner/eval/column/final-mayhew2-stem-orig
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-rules-keep0.25-dense-w3/
#TRAIN=/shared/corpora/ner/wikifier-features/$LANG/train-camera3/
#TRAIN=/shared/corpora/ner/conll2003/eng/Train/
#TRAIN=/shared/corpora/ner/hengji/$LANG/10k/1rl-mayhew2
#/shared/corpora/ner/human/$LANG/conll-anno-bbailey3-annogold
#TRAIN=/shared/corpora/ner/human/$LANG/conll-anno-bbailey3-stem
#TRAIN=/shared/experiments/mayhew2/a1,/shared/experiments/mayhew2/a2
#TRAIN=/shared/corpora/ner/human/$LANG/conll-anno-mayhew2-upennstem
#TRAIN=/tmp/Train/

#TRAIN=/shared/corpora/ner/parallel/$LANG/Train/
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train/
#TRAIN=/shared/corpora/ner/eval/column/dev2/
#TRAIN=/shared/corpora/corporaWeb/lorelei/data/LDC2016E86_LORELEI_Amharic_Representative_Language_Pack_Monolingual_Text_V1.1/data/monolingual_text/zipped/final-test2/
#-selftrain/,/shared/corpora/ner/translate/$LANG/Train-stem/
#TRAIN=/tmp/am1-anno/,/tmp/am2-anno/


CONFIG=config/tacl/$LANG.config
#CONFIG=config/tacl/$LANG-good.config
#CONFIG=config/mono.config
#CONFIG=config/hash.config

#TEST=/shared/corpora/ner/conll2003/eng/Test/
#TEST=/shared/experiments/ctsai12/workspace/xlwikifier/xlwikifier-data/ner-data/en/tac2015+NAM.all
#TEST=/home/mayhew2/IdeaProjects/illinois-cogcomp-nlp/ner/HashTest/
#TEST=/shared/corpora/ner/conll2002/nl/Test/
#TEST=/shared/corpora/ner/conll2002/es/Test/
#TEST=/shared/corpora/ner/hengji/$LANG/Test
#TEST=/shared/corpora/ner/lorelei/$LANG/All-stem-best
#TEST=/shared/corpora/ner/lorelei/$LANG/Test
#TEST=/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il5/set0/data/REFLEX_LCTL_IL5_v1.0/Tools/NE_Tagger/lctl_ner_tigrinya.v1.0/data/test/
#TEST=/tmp/hengji/$LANG/Test/
#TEST=/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/final-bbailey3
#TEST=/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il5/dev/conll
#TEST=/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/dev/conll
TEST=/shared/corpora/ner/lorelei/$LANG/Test
#TEST=/shared/corpora/ner/wikifier-features/$LANG/test-camera3
#TEST=/shared/corpora/ner/parallel/$LANG/partest/
#TEST=/shared/corpora/ner/lorelei/tr/Test-urom/

CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -lang $LANG"
#CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -test $TEST -cf $CONFIG -lang $LANG"

alias s="tail -f ${OUTF}"

echo $TRAIN >> $OUTF
getstats.py $TRAIN >> $OUTF

ls $TRAIN >> $OUTF


echo "$0: running command '$CMD'..." >> $OUTF

${CMD} >> $OUTF
