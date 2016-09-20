#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

LANG="es"

#TRAIN="/shared/corpora/ner/conll2002/es/Train/"
#TRAIN="/shared/corpora/ner/wikifier-features/es/tac2015-train1-merge/"
TRAIN="/shared/corpora/ner/wikifier-features/es/ere-NOM-head-train/"
#TRAIN="/shared/corpora/ner/ere/es/NOM-head1-train/"
#TRAIN="/shared/corpora/ner/ere/zh/NOM-head-char-train"
#TRAIN="/shared/corpora/ner/parallel/tac/Train/"
#TRAIN="/shared/corpora/ner/wikifier-features/es/train-camera3/"

CONFIG=config/tac.config

#TEST="/shared/corpora/ner/wikifier-features/es/test-camera3/"
#TEST="/shared/corpora/ner/wikifier-features/es/tac2015-test1-merge/"
TEST="/shared/corpora/ner/wikifier-features/es/ere-NOM-head-test/"
#TEST="/shared/corpora/ner/ere/es/NOM-head1-test/"
#TEST="/shared/corpora/ner/ere/zh/NOM-head-char-test/"
#TEST="/shared/corpora/ner/parallel/tac/Test/"
#TEST="/shared/corpora/ner/conll2002/es/Test/"

CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN,$TEST -test $TEST -cf $CONFIG -lang $LANG"
#CMD="java -classpath  ${cpath} -Xmx8g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -test $TRAIN -cf $CONFIG -lang $LANG"


echo "$0: running command '$CMD'..."

${CMD}
