#!/bin/sh
# generate/update the binary files and dependencies
#DATA_ROOT="/shared/corpora/ner/wikifier-features/en/train-camera3-misc/"
DATA_ROOT="/shared/corpora/ner/wikifier-features/en/onto-traindevtest-4types/"
#DATA_ROOT="data/Ontonotes/ColumnFormat"

train="$DATA_ROOT/"
test="$DATA_ROOT/"

# you will probably want to create a new config file
configFile="/home/mayhew2/IdeaProjects/transliteration-ner/config/eval.config"

#configFile="config/ner-ontonotes.properties"

# Classpath
cpath="target/classes:target/dependency/*:config"

CMD="java -classpath  ${cpath} -Xmx20g edu.illinois.cs.cogcomp.ner.NerTagger -trainFixedIterations 50 $train $test -c $configFile"

echo "$0: running command '$CMD'..."

${CMD}
