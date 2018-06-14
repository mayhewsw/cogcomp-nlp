#!/bin/sh
# generate/update the binary files and dependencies

# Classpath
cpath="target/classes:target/dependency/*:config"

CONFIG=config/lrlp/default.config
OUTFOLDER=better-brown
mkdir -p $OUTFOLDER

#for LNG in amh ara ben fas hin hun rus som spa tgl yor orm tir uig;
for LNG in amh ara ben fas;
do
    echo $LNG

    CONFIG=config/lrlp/$LNG.config
    
    TRAIN=/shared/corpora/ner/lorelei-swm-new/$LNG/Train
    DEV=/shared/corpora/ner/lorelei-swm-new/$LNG/Dev
    TEST=/shared/corpora/ner/lorelei-swm-new/$LNG/Test

    CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $DEV -cf $CONFIG -lang $LNG -format ta"
    ${CMD} > $OUTFOLDER/$LNG-dev.log

    CMD="java -classpath  ${cpath} -Xmx16g edu.illinois.cs.cogcomp.ner.LbjTagger.LORELEIRunner -train $TRAIN -test $TEST -cf $CONFIG -lang $LNG -format ta"
    ${CMD} > $OUTFOLDER/$LNG-test.log

done

