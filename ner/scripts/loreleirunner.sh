#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG="es"

#TRAIN=/shared/corpora/ner/translate/eng/Train-ta3/
#TRAIN=/shared/corpora/ner/conll2003/eng/Train-ta/
#TRAIN=/shared/corpora/ner/conll2002/es/Train/
#TRAIN=/shared/corpora/ner/lorelei/$LANG/Train/
#TRAIN=/shared/corpora/ner/eval/column/trainset3000/
#TRAIN=/shared/corpora/ner/lorelei/$LANG/All-out/
#TRAIN=/shared/corpora/ner/lorelei/$LANG/All-lm2/
#TRAIN=/shared/corpora/ner/eval/column/dev2/
TRAIN=/shared/corpora/ner/parallel/$LANG/Train-fa/

CONFIG=config/uzug.config
#CONFIG=config/mono3000.config

#TEST=/shared/corpora/ner/eval/column/dev2/
#TEST=/shared/corpora/ner/lorelei/ug/All-uly/
#TEST=/shared/corpora/ner/conll2003/deu/Test/
#TEST=/shared/corpora/ner/conll2002/nl/Test/
TEST=/shared/corpora/ner/conll2002/es/Test/
#TEST=/shared/corpora/ner/eval/column/dev86/
#TEST=/shared/corpora/ner/lorelei/$LANG/Test/
#TEST=/shared/corpora/ner/lorelei/tr/Test-urom/

CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -lang $LANG"
#CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -test $TEST -cf $CONFIG -lang $LANG"


echo "$0: running command '$CMD'..."

${CMD}
