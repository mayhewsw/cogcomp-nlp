


#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG=$1


CONFIG=config/tacl/$LANG.config
#CONFIG=config/mono.config

#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-0.25-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-anno-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train-anno-gaz
#TRAIN=/shared/corpora/ner/human/$LANG/conll-anno-combine1-weighted
#TRAIN=/shared/corpora/ner/lorelei/$LANG/CheapTrain
#TRAIN=/tmp/mydir/train2
TRAIN=/shared/corpora/ner/conll2003/eng-old/eng-nomisc.conll
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-0.25-weighted
#TRAIN=/home/mayhew/IdeaProjects/codlner/data/eng/typed.weighted
#TRAIN=ilp/fixed
#TRAIN=ilp/ilp.model.testdata.best.predsasweights
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-p0.9-r0.25-mention-weighted
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-0.25-weighted/
#TRAIN=ilp/ilp.model.testdata.trueoracle.predsasweights
#TRAIN=/shared/corpora/ner/conll2003/deu/Train

#TEST=/shared/corpora/ner/lorelei/$LANG/Train-0.25-weighted
#TEST=/shared/corpora/ner/lorelei/$LANG/Test-mention
#TEST=/shared/corpora/ner/conll2003/eng/Test-mention
#TEST=/shared/corpora/ner/conll2003/eng/Train-mention
#TEST=/shared/corpora/ner/conll2003/eng/Dev
#TEST=/shared/corpora/ner/conll2003/eng/Train
TEST=/shared/corpora/ner/broad_twitter_corpus/TestB/


#CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -lang $LANG"
CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -test $TEST -cf $CONFIG -lang $LANG"

echo $TRAIN
datastats.py $TRAIN

ls $TRAIN


echo "$0: running command '$CMD'..."

${CMD}
