#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

GOLD=$1
#/shared/corpora/ner/conll2003/eng/Dev
PRED=$2
#ilp/ilp.model.testdata.ilpmulticlass

CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.Scorer -gold $GOLD -pred $PRED"

${CMD}
