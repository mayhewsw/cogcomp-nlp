package edu.illinois.cs.cogcomp.ner.LbjTagger;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel2;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.PlainTextReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

/**
 * Created by mayhew2 on 6/22/16.
 */
@SuppressWarnings("Duplicates")
public class SelfTrainer {

    public static final String config = "config/self-train.config";

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

        File dir = new File(dataPath);
        String[] files = dir.list();

        Data data = new Data();
        data.documents = new ArrayList<>();

        for(String file : files) {
            ArrayList<LinkedVector> res = PlainTextReader.parsePlainTextFile(dataPath + "/" + file);
            NERDocument doc = new NERDocument(res, file);
            data.documents.add(doc);
        }

        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
        NETaggerLevel2 tagger2 = new NETaggerLevel2(modelPath + ".level2", modelPath + ".level2.lex");

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

    public static void main(String[] args) throws Exception {
        SelfTrainer.makeNewData("/shared/corpora/corporaWeb/lorelei/turkish/tools/ltf2txt/full_short/", "out-fs/");
        //SelfTrainer.makeNewData("/shared/corpora/ner/mono/tr/tr100/","out-wiki/");
    }


}
