


#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG=$1

OUTF=logs/$LANG-`date +%m-%d-%y-%H%M`-LR.log
echo "Logging to:" $OUTF

CONFIG=config/tacl/$LANG.config
#CONFIG=config/mono.config

#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-0.25-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-anno-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-anno-gaz
#TRAIN=/shared/corpora/ner/human/$LANG/conll-anno-combine1-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/CheapTrain
#TRAIN=/tmp/mydir/train2
TRAIN=/shared/corpora/ner/conll2003/eng/Train-0.25-mention-weighted
#TRAIN=ilp/ilp.model.testdata.weighted
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-dense
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-0.25-mention-weighted
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-tmp-weighted
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-0.25-weighted

#TEST=/shared/corpora/ner/lorelei/$LANG/Train-0.25-weighted
#TEST=/shared/corpora/ner/lorelei/$LANG/Test-mention
#TEST=/shared/corpora/ner/conll2003/eng/Test-mention
TEST=/shared/corpora/ner/conll2003/eng/Train-mention

CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -lang $LANG"
#CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -test $TEST -cf $CONFIG -lang $LANG"

alias s="tail -f ${OUTF}"

echo $TRAIN >> $OUTF
datastats.py $TRAIN >> $OUTF

ls $TRAIN >> $OUTF


echo "$0: running command '$CMD'..." >> $OUTF

${CMD} >> $OUTF
