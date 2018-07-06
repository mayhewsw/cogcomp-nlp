#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG=$1


#CONFIG=config/tacl/$LANG.config
CONFIG=config/lrlp/$LANG.config

#CONFIG=config/mono.config

#TRAIN=/shared/corpora/ner/conll2003/eng-files/Train-4types-json/
#TRAIN=/shared/corpora/ner/conll2003/eng/Train
#TRAIN=/shared/corpora/ner/lorelei-swm-new/swa/All-json
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il9/ner/Dev0
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il9/ner/cheap-trans-wiki,/shared/corpora/ner/lorelei-swm-new/swa/All-json/
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/ner/Dev0
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/dirs_train_set1/dirs10_15-anno-annotation-ni_il10,/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/ner/Dev0
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il9/processed/dirs_train_set1/all-annotation,/shared/corpora/corporaWeb/lorelei/evaluation-2018/il9/ner/Dev0,/shared/corpora/ner/lorelei-swm-new/swa/All-json
TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/dirs_train_set1/all-annotation
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/train-set1-selected-100k-anno
#RAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/ner/cheap-trans-name-expand,/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/ner/Dev1,/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/ner/pivot-trans
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/dirs_train_set1/set1_train_1-annotation-stephen,/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/dirs_train_set1/set1_train_2-annotation-stephen
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/train-set1-selected-100k-anno


#TEST=/shared/corpora/ner/lorelei-swm-new/tir/Test
#TEST=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/setE/il10/
#TEST=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/
TEST=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/ner/Dev1
#TEST=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/ner/Dev0-devanagari/
#TEST=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/dirs_train_set1/set1_train_5
#TEST=/shared/corpora/corporaWeb/lorelei/evaluation-upenn-20180402/tha/ner/mono-select-all-rules/

OUTPATH=/shared/corpora/corporaWeb/lorelei/evaluation-2018/il10/processed/dirs_train_set1/set1_train_5-annotation-stephen
mkdir -p $OUTPATH
#CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -outpath $OUTPATH -format ta"
CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -format ta"




#echo $TRAIN
#datastats.py $TRAIN

#ls $TRAIN

echo "$0: running command '$CMD'..."
${CMD}

