#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG=$1


#CONFIG=config/tacl/$LANG.config
CONFIG=config/lrlp/$LANG.config

#CONFIG=config/mono.config

#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-0.25-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-anno-weighted
#TRAIN=/shared/corpora/ner/lorelei/bn/experiments/Train
#TRAIN=/shared/corpora/ner/lorelei/am/Train
#TRAIN=/shared/corpora/ner/lorelei/am/
#TRAIN=/shared/corpora/ner/lorelei/am/annoexp0315/brat-dense
#TRAIN=/shared/corpora/ner/human/$LANG/conll-anno-combine1-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/CheapTrain
#TRAIN=/tmp/mydir/train2
#TRAIN=/home/mayhew/IdeaProjects/codlner/experiments/conll+twitter/latest/Train
##TRAIN=/shared/corpora/ner/conll2003/eng-old/eng-nomisc.conll
#TRAIN=/home/mayhew/data/broad_twitter_corpus/setH1
#TRAIN=/shared/corpora/ner/conll2003/deu/tmp
#TRAIN=/home/mayhew/IdeaProjects/codlner/data/eng/typed.weighted
#TRAIN=ilp/fixed
#TRAIN=ilp/ilp.model.testdata.best.predsasweights
#TRAIN=/shared/corpora/ner/conll2002/esp/Train-p0.8-r0.25-puncnum/
#TRAIN=/home/mayhew/data/conll-cis419/Data/Real-World/CoNLL/train-conll/
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-mention
#TRAIN=ilp/ilp.model.testdata.trueoracle.predsasweights
#TRAIN=/shared/corpora/ner/conll2003/eng/Train
#TRAIN=/shared/corpora/ner/lorelei/om/Train
#TRAIN=/shared/corpora/ner/eval/column/Train-weighted
#TRAIN=/home/mayhew/data/CALCS2018/CALCS_ENG_SPA/Train
TRAIN=/shared/corpora/ner/lorelei-swm-new/rus/Train
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-upenn-20180402/processed/il5/train/reflex-json,/shared/corpora/corporaWeb/lorelei/evaluation-upenn-20180402/processed/il5/train/set1-twitter
#TRAIN=/shared/corpora/corporaWeb/lorelei/evaluation-upenn-20180402/processed/il6/train/alltrain-json

#TEST=/shared/corpora/ner/lorelei/$LANG/Train-0.25-weighted
#TEST=/shared/corpora/ner/lorelei/om/Test
#TEST=/shared/corpora/ner/conll2003/eng/Train-0.25-mention
#TEST=/shared/corpora/ner/conll2003/eng/Test
#TEST=/shared/corpora/ner/conll2002/esp/Test
#TEST=/home/mayhew/data/conll-cis419/Data/Real-World/CoNLL/dev-conll
#TEST=/shared/corpora/ner/conll2003/eng/Dev-mention
#TEST=/home/mayhew/data/broad_twitter_corpus/setH2
#TEST=/shared/corpora/ner/lorelei/ug/All-stem-best
#TEST=/home/mayhew/data/CALCS2018/CALCS_ENG_SPA/Dev
TEST=/shared/corpora/ner/lorelei-swm-new/rus/Test/

CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -format ta"


#echo $TRAIN
#datastats.py $TRAIN

#ls $TRAIN

echo "$0: running command '$CMD'..."
${CMD}

