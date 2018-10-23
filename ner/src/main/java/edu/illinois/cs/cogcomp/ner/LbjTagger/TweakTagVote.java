package edu.illinois.cs.cogcomp.ner.LbjTagger;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.lbjava.classify.TestDiscrete;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.ner.ModelLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For reading tweaked files.
 * Created by mayhew2 on 10/18/2018.
 */
@SuppressWarnings("Duplicates")
public class TweakTagVote {

    private static final Logger logger = LoggerFactory.getLogger(TweakTagVote.class);

    public static String config = null;

    public static void main(String[] args) throws Exception {

        config = args[1];

        boolean areWeTraining = false;
        ParametersForLbjCode cp = Parameters.readConfigAndLoadExternalData(config, areWeTraining);

        ResourceManager rm = new ResourceManager(config);
        ModelLoader.load(rm, "NER_CONLL", false, cp);

        String testroot = args[0];
        Data testData = loaddata(testroot, cp);
        ExpressiveFeaturesAnnotator.annotate(testData, cp);
        RunTest(testData, cp);
        System.out.println("Tested on: " + testroot);
    }

    /**
     * Given a path to a folder, load the data from that folder. Assume the string is a comma
     * separated list, perhaps of length one.
     * @param datapath
     * @return
     * @throws Exception
     */
    public static Data loaddata(String datapath, ParametersForLbjCode cp) throws Exception {
        Data data = new Data();
        List<String> lines = LineIO.read(datapath);

        // enforce that the last one should be empty!
        lines.add("");

        // format will be: labels line, line, line, EMPTY, etc.
        // just need to know the number of options.

        boolean lastempty = true;
        List<String> options = new ArrayList<>();
        String[] labels = null;
        int docid = 0;

        ArrayList<LinkedVector> sentences = new ArrayList<>();

        for(String line : lines){
            line = line.trim();
            if(lastempty){
                // this one is labels.
                labels = line.split(" ");
                lastempty = false;
            }else if(line.trim().length() == 0){

                if (options.size() > 0) {
                    for (String variant : options) {

                        String[] words = variant.split(" ");

                        LinkedVector sent = new LinkedVector();
                        for (int i = 0; i < labels.length; i++) {
                            NEWord.addTokenToSentence(sent, words[i], labels[i], cp);
                        }
                        sentences.add(sent);
                    }
                    NERDocument doc = new NERDocument(sentences, "doc" + docid++);
                    data.documents.add(doc);
                }


                // collect all others.
                sentences = new ArrayList<>();
                options = new ArrayList<>();
                lastempty = true;
            }else{
                options.add(line);
                lastempty = false;
            }

        }

        ExpressiveFeaturesAnnotator.annotate(data, cp);
        data.setLabelsToIgnore(cp.labelsToIgnoreInEvaluation);

        return data;
    }


    public static void RunTest(Data testData, ParametersForLbjCode cp) throws Exception {

        Decoder.annotateDataBIO(testData, cp);

        Data origdata = new Data();

        // now the testdata has been annotated, what to do with it?
        for(NERDocument doc : testData.documents){

            // all sentences are guaranteed to have the same number of tokens.
            // loop over tokens
            LinkedVector orig = doc.sentences.get(0);

            LinkedVector newsent = new LinkedVector();

            for(int t= 0; t < orig.size(); t++){
                HashMap<String, Integer> votes = new HashMap<>();
                for(LinkedVector sent : doc.sentences){
                    NEWord w = (NEWord) sent.get(t);
                    String pred = w.neTypeLevel1;

                    int count = votes.getOrDefault(pred, 0);
                    votes.put(pred, count+1);
                }
                NEWord origtok = (NEWord) orig.get(t);
//                System.out.println(origtok);
//                System.out.println(votes);

                int maxvotes = -1;
                String bestlabel = origtok.neTypeLevel1;
                for(String label : votes.keySet()){
                    int votecount = votes.get(label);
                    if(votecount > maxvotes){
                        bestlabel = label;
                        maxvotes= votecount;
                    }
                }
                //origtok.neTypeLevel1 = bestlabel;

                // create a new word, and add it to the new sentence.
                NEWord word = new NEWord(new Word(origtok.form), null, origtok.neLabel);
                word.params = cp;
                word.neTypeLevel1=bestlabel;
                word.neTypeLevel2=bestlabel;
                NEWord.addTokenToSentence(newsent, word);
            }

            // use the predictions on all sentences to modify the predictions on sentence 0 (by default always the gold text)

            ArrayList<LinkedVector> newsents = new ArrayList<>();
            newsents.add(newsent);
            doc.sentences = newsents;

            ArrayList<LinkedVector> origsents = new ArrayList<>();
            origsents.add(orig);
            NERDocument origdoc = new NERDocument(origsents, doc.docname);
            origdata.documents.add(origdoc);
        }


        // It may not always make sense to get score (will often be 0), but useful anyway.
        TestDiscrete resultsPhraseLevel1 = new TestDiscrete();
        resultsPhraseLevel1.addNull("O");
        TestDiscrete resultsTokenLevel1 = new TestDiscrete();
        resultsTokenLevel1.addNull("O");

        TestDiscrete resultsPhraseLevel2 = new TestDiscrete();
        resultsPhraseLevel2.addNull("O");
        TestDiscrete resultsTokenLevel2 = new TestDiscrete();
        resultsTokenLevel2.addNull("O");

        TestDiscrete resultsByBILOU = new TestDiscrete();
        TestDiscrete resultsSegmentation = new TestDiscrete();
        resultsByBILOU.addNull("O");
        resultsSegmentation.addNull("O");


        NETesterMultiDataset.reportPredictions(testData,
                resultsTokenLevel1,
                resultsTokenLevel2,
                resultsPhraseLevel1,
                resultsPhraseLevel2,
                resultsByBILOU,
                resultsSegmentation);

        System.out.println("------------------------------------------------------------");
        System.out.println("******	Combined performance on the voted datasets :");
        System.out.println("------------------------------------------------------------");

        System.out.println("Phrase-level Acc Level1:");
        resultsPhraseLevel1.printPerformance(System.out);

        System.out.println("------------------------------------------------------------");
        System.out.println("************************************************************");
        System.out.println("------------------------------------------------------------");


        // It may not always make sense to get score (will often be 0), but useful anyway.
        resultsPhraseLevel1 = new TestDiscrete();
        resultsPhraseLevel1.addNull("O");
        resultsTokenLevel1 = new TestDiscrete();
        resultsTokenLevel1.addNull("O");

        resultsPhraseLevel2 = new TestDiscrete();
        resultsPhraseLevel2.addNull("O");
        resultsTokenLevel2 = new TestDiscrete();
        resultsTokenLevel2.addNull("O");

        resultsByBILOU = new TestDiscrete();
        resultsSegmentation = new TestDiscrete();
        resultsByBILOU.addNull("O");
        resultsSegmentation.addNull("O");

        NETesterMultiDataset.reportPredictions(origdata,
                resultsTokenLevel1,
                resultsTokenLevel2,
                resultsPhraseLevel1,
                resultsPhraseLevel2,
                resultsByBILOU,
                resultsSegmentation);

        System.out.println("------------------------------------------------------------");
        System.out.println("******	Combined performance on the orig dataset:");
        System.out.println("------------------------------------------------------------");

        System.out.println("Phrase-level Acc Level1:");
        resultsPhraseLevel1.printPerformance(System.out);

        System.out.println("------------------------------------------------------------");
        System.out.println("************************************************************");
        System.out.println("------------------------------------------------------------");

    }


}
