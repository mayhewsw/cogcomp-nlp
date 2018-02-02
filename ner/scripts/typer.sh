#!/bin/bash
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

#TRAIN=$1
#INPUT=$2
#OUTPUT=$3


TRAIN=/shared/corpora/ner/conll2003/eng/Train-0.25/eng.train.conll
INPUT=/home/mayhew/IdeaProjects/codlner/data/eng/ner-0.17
OUTPUT=/home/mayhew/IdeaProjects/codlner/data/eng/typed

rm -rf /tmp/train /tmp/input /tmp/output
mkdir -p /tmp/train
pushd /tmp/train
split -l 5000 $TRAIN
popd

mkdir -p /tmp/input
python /home/mayhew/IdeaProjects/codlner/scripts/fixlabels.py $INPUT /tmp/inputfix
pushd /tmp/input
split -l 5000 /tmp/inputfix
popd
rm /tmp/inputfix

mkdir -p /tmp/output

CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.TyperRunner -train /tmp/train -input /tmp/input -output /tmp/output"

${CMD}

rm -rf tmpfile
for f in /tmp/output/*;
do
    sed -i '$ d' $f;
    cat $f >> tmpfile
done

mv tmpfile $OUTPUT

# Do NOT overwrite.
mergelabels.py $OUTPUT $TRAIN

score.py /shared/corpora/ner/conll2003/eng/Train-mention/eng.train.conll $INPUT
score.py /shared/corpora/ner/conll2003/eng/Train/eng.train.conll $OUTPUT


python scripts/weightstomnt.py $OUTPUT $OUTPUT.weighted
