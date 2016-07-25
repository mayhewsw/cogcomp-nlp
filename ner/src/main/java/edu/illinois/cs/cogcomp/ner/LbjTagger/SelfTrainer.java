package edu.illinois.cs.cogcomp.ner.LbjTagger;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel2;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.PlainTextReader;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataReader;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataWriter;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

/**
 * Created by mayhew2 on 6/22/16.
 */
@SuppressWarnings("Duplicates")
public class SelfTrainer {

    public static final String config = "/home/mayhew2/IdeaProjects/transliteration-ner/config/eval.config";

    /**
     * Given a datapath, load the model and annotate the data, and write it out to outpath.
     *
     * Datapath is a folder, and each file is a text file in target language. Annotate each line.
     *
     * This also aggressively filters the data, so only high-confidence results are kept.
     *
     * @param dataPath
     * @param outPath
     */
    public static void makeNewData(String dataPath, String outPath) throws Exception {

        Parameters.readConfigAndLoadExternalData(config, false);
        
        String modelPath = ParametersForLbjCode.currentParameters.pathToModelFile;
        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
        NETaggerLevel2 tagger2 = new NETaggerLevel2(modelPath + ".level2", modelPath + ".level2.lex");

        File dir = new File(dataPath);
        String[] files = dir.list();

        Data data = new Data();
        data.documents = new ArrayList<>();

        for(String file : files) {
            ArrayList<LinkedVector> res = PlainTextReader.parsePlainTextFile(dataPath + "/" + file);
            NERDocument doc = new NERDocument(res, file);
            data.documents.add(doc);
        }


        Decoder.annotateDataBIO(data, tagger1, tagger2);

        Random rand = new Random();
        
        //ArrayList<String> results = new ArrayList<>();
        //results.add("word\tgold\tpred");
        int counter = 0;
        for(NERDocument doc : data.documents) {
            System.out.println(100 * counter++ / (float)data.documents.size() + "% done with docs");

            List<String> docpreds = new ArrayList<>();

            ArrayList<LinkedVector> sentences = doc.sentences;
            for (int k = 0; k < sentences.size(); k++){
                LinkedVector sentence = sentences.get(k);
                //boolean hasEntities = false;
                List<String> sentlines = new ArrayList<>();

                ArrayList<Integer> indicesToInclude = new ArrayList<>();
                int window = 5;

                for (int i = 0; i < sentence.size() ; ++i){
                    NEWord w = (NEWord)sentence.get(i);

                    //results.add(w.form + "\t" + w.neLabel + "\t" + w.neTypeLevel2);
                    //sentlines.add(ACLRunner.conllline(w.neTypeLevel2, i, w.form));
                    if(!w.neTypeLevel2.equals("O")){
                        // add all indices around a window.
                        for(int j = Math.max(0, i-window); j < Math.min(i+window, sentence.size()); j++) {
                            if(!indicesToInclude.contains(j)) {
                                indicesToInclude.add(j);
                            }
                        }
                        //hasEntities = true;
                    }
                }

                for(int i : indicesToInclude){
                    NEWord w = (NEWord)sentence.get(i);
                    sentlines.add(ACLRunner.conllline(w.neTypeLevel2, i, w.form));
                }

                if(sentlines.size() > 0) {
                    docpreds.addAll(sentlines);
                    docpreds.add("");
                }



//                if(sentlines.size() > 0 && (hasEntities || rand.nextDouble() < 0.0)){
//                    docpreds.addAll(sentlines);
//                    docpreds.add("");
//                }

               
            }

            if(docpreds.size() > 0){
                LineIO.write(outPath + doc.docname, docpreds);
            }
        }
        //logger.info("Just wrote to: " + "/shared/corpora/ner/system-outputs/"+testlang+ "/projection-fa/");

        //LineIO.write("gold-pred-" + testlang + ".txt", results);

    }


    /**
     * Given a folder that contains conll files, this will annotate all files in the directory
     * and write the result out to file. This is intended to be used in self-training, e.g. annotating
     * the testing data.
     * @param path
     * @throws Exception
     */
    public static void reannotate(String path, String goldpredpath) throws Exception {
        Parameters.readConfigAndLoadExternalData(config, false);

        String modelPath = ParametersForLbjCode.currentParameters.pathToModelFile;
        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
        NETaggerLevel2 tagger2 = new NETaggerLevel2(modelPath + ".level2", modelPath + ".level2.lex");

        Data data = new Data();
        Vector<NERDocument> docs = TaggedDataReader.readFolder(path, "-c");
        data.documents = new ArrayList<>();
        data.documents.addAll(docs);

        Decoder.annotateDataBIO(data, tagger1, tagger2);

        for(NERDocument doc : data.documents) {

            ArrayList<LinkedVector> sentences = doc.sentences;
            for (int k = 0; k < sentences.size(); k++){
                for (int i = 0; i < sentences.get(k).size() ; ++i){
                    NEWord w = (NEWord)sentences.get(k).get(i);
                    if(w.neTypeLevel2.equals("O")){
                        w.neTypeLevel2 = w.neLabel;
                    }                    
                }
            }


        }
        
        // then write out to another file?
        TaggedDataWriter.writeToFile("tr-output.conll", data, "-c", NEWord.LabelToLookAt.PredictionLevel2Tagger);

        data = new Data();
        docs = TaggedDataReader.readFolder(goldpredpath, "-c");
        data.documents = new ArrayList<>();
        data.documents.addAll(docs);

        Decoder.annotateDataBIO(data, tagger1, tagger2);
        TaggedDataWriter.writeToFolder("conllout/", data, "-c", NEWord.LabelToLookAt.PredictionLevel2Tagger);


    }

    public static void annotatefolder(String path, String outpath) throws Exception {

        String config = "config/mono.config";

        Parameters.readConfigAndLoadExternalData(config, false);

        String modelPath = ParametersForLbjCode.currentParameters.pathToModelFile;
        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
        NETaggerLevel2 tagger2 = new NETaggerLevel2(modelPath + ".level2", modelPath + ".level2.lex");


        String[] p = (new File(path)).list();
        for(String f : p){
            Data data = new Data();
            NERDocument doc = TaggedDataReader.readFile(path + "/" + f, "-c", f);
            //Vector<NERDocument> docs  = TaggedDataReader.readFolder(path, "-c");
            data.documents = new ArrayList<>();
            data.documents.add(doc);

            Decoder.annotateDataBIO(data, tagger1, tagger2);
            TaggedDataWriter.writeToFile(outpath + "/" + f, data, "-c", NEWord.LabelToLookAt.PredictionLevel2Tagger);
            //TaggedDataWriter.writeToFolder(outpath, data, "-c", NEWord.LabelToLookAt.PredictionLevel2Tagger);

        }

    }



    public static void main(String[] args) throws Exception {
        //SelfTrainer.makeNewData("/shared/corpora/corporaWeb/lorelei/turkish/tools/ltf2txt/full_short/", "out-fs/");
        //SelfTrainer.annotatefolder("/shared/corpora/ner/eval/column/set0-mono-NW/","out-set0-NW/");

        String dir = "/shared/corpora/ner/wikifier-features/ug/";
        SelfTrainer.annotatefolder(dir + "iter10-NW-correct", dir + "iter10-NW-correct-koran/");

        //getFeatureWeights();
        
        //String datadir = "/shared/corpora/ner/parallel/tr/";

        //reannotate(datadir + "Train-edit/", datadir + "GoldPred/");

    }


}
