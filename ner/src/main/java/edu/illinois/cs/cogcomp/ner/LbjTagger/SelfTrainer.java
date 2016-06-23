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

        //ArrayList<String> results = new ArrayList<>();
        //results.add("word\tgold\tpred");
        for(NERDocument doc : data.documents) {
            List<String> docpreds = new ArrayList<>();

            ArrayList<LinkedVector> sentences = doc.sentences;
            for (int k = 0; k < sentences.size(); k++){
                for (int i = 0; i < sentences.get(k).size() ; ++i){
                    NEWord w = (NEWord)sentences.get(k).get(i);
                    //results.add(w.form + "\t" + w.neLabel + "\t" + w.neTypeLevel2);
                    docpreds.add(ACLRunner.conllline(w.neTypeLevel2, i, w.form));
                }
                docpreds.add("");
            }

            LineIO.write(outPath + doc.docname, docpreds);
        }
        //logger.info("Just wrote to: " + "/shared/corpora/ner/system-outputs/"+testlang+ "/projection-fa/");

        //LineIO.write("gold-pred-" + testlang + ".txt", results);

    }

    public static void main(String[] args) throws Exception {
        SelfTrainer.makeNewData("/shared/corpora/corporaWeb/lorelei/turkish/tools/ltf2txt/tmp/", "out/");
    }


}
