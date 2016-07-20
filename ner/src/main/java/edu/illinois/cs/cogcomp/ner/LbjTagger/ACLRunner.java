package edu.illinois.cs.cogcomp.ner.LbjTagger;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.StringUtils;
import edu.illinois.cs.cogcomp.lbjava.classify.TestDiscrete;
import edu.illinois.cs.cogcomp.lbjava.learn.BatchTrainer;
import edu.illinois.cs.cogcomp.lbjava.learn.SparseNetworkLearner;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.TwoLayerPredictionAggregationFeatures;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.PredictionsAndEntitiesConfidenceScores;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel2;
import edu.illinois.cs.cogcomp.ner.WordEmbedding;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataReader;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * ACLRunner class. For ACL2016
 * Created by mayhew2 on 3/15/16.
 */
@SuppressWarnings("Duplicates")
public class ACLRunner {

    private static final Logger logger = LoggerFactory.getLogger(ACLRunner.class);

    public static String[] langs = {"en", "es", "de", "nl", "tr", "uz", "bn", "ha"};
    public static final String filesFormat = "-c";

    // these do not have a MISC tag, so skip it in evaluation.
//    public static HashSet<String> lorlangs = new HashSet<>();
//    {
//        lorlangs.add("bn");
//        lorlangs.add("tr");
//        lorlangs.add("ha");
//        lorlangs.add("uz");
//    }


    /**
     * Convenience function to avoid having to think about this format all the time...
     * @param tag
     * @param num
     * @param word
     * @return
     */
    public static String conllline(String tag, int num, String word){
        return String.format("%s\t0\t%s\t0\t0\t%s\tx\tx\t0", tag, num, word);
    }

    public static final String dataroot = "/shared/corpora/ner/wikifier-features/";
    public static final String config = "config/mono.config";

    public static void main(String[] args) throws Exception {

        String lang = "tr";
        String predpath = "conllout/"; //"/shared/corpora/ner/parallel/"+lang+"/GoldPred/";
        String goldpath = "/shared/corpora/ner/lorelei/"+lang+"/All/";

        //CompareWithGold(predpath, goldpath, lang);

        singlelang(args[0], args[1], args[2]);

        //multisource(args[0], args[1], args[2]);
    }

    public static void multisource(String train1, String train2, String test) throws Exception{
        boolean areWeTraining = true;
        Parameters.readConfigAndLoadExternalData(config, areWeTraining);

        String[] trainlangs = {train1, train2};
        String testlang = test;

        String testroot = dataroot + testlang + "/Test-new/";

        int trainiter = 30;
        String modelpath = "data/Models-new/multisource-" + StringUtils.join("-", trainlangs);

        String trainroot = "";
        for(String trainlang : trainlangs){
            trainroot += dataroot + trainlang + "/Train-new/,";
        }

        Data trainData = loaddata(trainroot, filesFormat, true);
        RunTraining(trainData, trainiter, modelpath);
        Pair<Double, Double> levels  = RunTest(testroot, modelpath, testlang);
        System.out.println(levels);
    }


    /**
     * Use this to get a score for a single language.
     * @throws Exception
     */
    public static void singlelang(String trainroot, String testroot, String lang) throws Exception {
        boolean areWeTraining = true;
        Parameters.readConfigAndLoadExternalData(config, areWeTraining);

        if(ParametersForLbjCode.currentParameters.featuresToUse.containsKey("Embedding")) {
            if(ParametersForLbjCode.currentParameters.testlang.equals("en"))
                WordEmbedding.setMonoVecsNew("en");
            else
                WordEmbedding.loadMultiDBNew(ParametersForLbjCode.currentParameters.testlang);
        }

        //String lang = "tr";
        //String lang2 = "tur";

        //String trainroot = dataroot + trainlang + "/Train-new-mono/";
        //String testroot = dataroot + testlang + "/Test-new-mono/";

        //String trainroot = "/shared/corpora/ner/parallel/" + lang + "/Train/";
        //String testroot = "/shared/corpora/ner/hengji/" + lang2 + "/Test/";


        int trainiter = 30;
        String modelpath = ParametersForLbjCode.currentParameters.pathToModelFile;
        Data trainData = loaddata(trainroot, filesFormat, true);
        RunTraining(trainData, trainiter, modelpath);
        Pair<Double, Double> levels  = RunTest(testroot, modelpath, lang);
        System.out.println(levels);
        System.out.println("Trained on: " + trainroot);
        System.out.println("Tested on: " + testroot);
    }


    /**
     * Use this to train a model for each language, and then test it on each language.
     * @param trainlang
     * @throws Exception
     */
    public static void getConfusionMatrix(String trainlang) throws Exception {
        boolean areWeTraining = true;
        Parameters.readConfigAndLoadExternalData(config, areWeTraining);

        String trainroot = dataroot + trainlang + "/Train-new";

        int trainiter = 30;
        String modelpath = "data/Models-confusion/" + trainlang + "-" + trainiter + "iter";

        Data trainData = loaddata(trainroot, filesFormat, true);

        RunTraining(trainData, trainiter, modelpath);

        String testroot;
        ArrayList<String> outlines = new ArrayList<>();
        outlines.add("model: " + modelpath);
        outlines.add("config: " + config);
        outlines.add("lang\tlevel1\tlevel2");
        for(String testlang : langs){
            testroot = dataroot + testlang + "/Test-new";
            Pair<Double, Double> levels  = RunTest(testroot, modelpath, testlang);
            outlines.add(testlang + "\t" + levels.getFirst() + "\t" + levels.getSecond());
        }

        LineIO.write("results-"+trainlang+".txt", outlines);
    }


    /**
     * Given a path to a folder, load the data from that folder. Assume the string is a comma
     * separated list, perhaps of length one.
     * @param datapath
     * @param filesFormat
     * @return
     * @throws Exception
     */
    public static Data loaddata(String datapath, String filesFormat, boolean train) throws Exception {
        String[] paths = datapath.split(",");
        // Get train data
        String first = paths[0];
        Data data = new Data(first, first, filesFormat, new String[]{}, new String[]{});
        for(int i = 1; i < paths.length; i++){
            data.addFolderToData(paths[i], filesFormat);
        }

        ExpressiveFeaturesAnnotator.train = train;
        ExpressiveFeaturesAnnotator.annotate(data);

        data.setLabelsToIgnore(ParametersForLbjCode.currentParameters.labelsToIgnoreInEvaluation);

        return data;
    }
    
    /**
     * @param classifier
     * @param dataSet
     * @param exampleStorePath
     * @return
     */
    private static BatchTrainer prefetchAndGetBatchTrainer(SparseNetworkLearner classifier, Data dataSet, String exampleStorePath) {
        //logger.info("Pre-extracting the training data for Level 1 classifier");

        TextChunkRepresentationManager.changeChunkRepresentation(
                TextChunkRepresentationManager.EncodingScheme.BIO,
                ParametersForLbjCode.currentParameters.taggingEncodingScheme,
                dataSet, NEWord.LabelToLookAt.GoldLabel);


        // FIXME: this is where we set the progressOutput var for the BatchTrainer
        BatchTrainer bt = new BatchTrainer(classifier, new DataSetReader(dataSet), 50000);

        classifier.setLexicon(bt.preExtract(exampleStorePath));

        TextChunkRepresentationManager.changeChunkRepresentation(
                ParametersForLbjCode.currentParameters.taggingEncodingScheme,
                TextChunkRepresentationManager.EncodingScheme.BIO,
                dataSet, NEWord.LabelToLookAt.GoldLabel);

        return bt;
    }

    /**
     * Given preloaded data, train a model and save it.
     * @param trainData
     * @param fixedNumIterations
     * @return
     * @throws Exception
     */
    public static void RunTraining(Data trainData, int fixedNumIterations, String modelPath) throws Exception {

        if ( IOUtils.exists( modelPath ) )
        {
            if ( !IOUtils.isDirectory( modelPath ) )
            {
                String msg = "ERROR: model directory '" + modelPath +
                        "' already exists as a (non-directory) file.";
                logger.error( msg );
                throw new IOException( msg );
            }
            else
                logger.warn( "writing to existing model path '" + modelPath + "'..." );
        }
        else
        {
            IOUtils.mkdir( modelPath );
        }

        IOUtils.cp(config, modelPath + "/" + FilenameUtils.getName(config));

        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
        tagger1.forget();

        if (ParametersForLbjCode.currentParameters.featuresToUse.containsKey("PredictionsLevel1")) {
            PredictionsAndEntitiesConfidenceScores.getAndMarkEntities(trainData, NEWord.LabelToLookAt.GoldLabel);
            TwoLayerPredictionAggregationFeatures.setLevel1AggregationFeatures(trainData, true);
        }

        logger.info("Pre-extracting the training data for Level 1 classifier");
        String arg = modelPath + ".level1.prefetchedTrainData";
        BatchTrainer bt1train = prefetchAndGetBatchTrainer(tagger1, trainData, arg);

        logger.info("Training...");
        bt1train.train(fixedNumIterations);

        NETaggerLevel2 tagger2 = new NETaggerLevel2(modelPath + ".level2", modelPath + ".level2.lex");
        tagger2.forget();

        // Previously checked if PatternFeatures was in featuresToUse.
        if (ParametersForLbjCode.currentParameters.featuresToUse.containsKey("PredictionsLevel1")) {

            logger.info("Pre-extracting the training data for Level 2 classifier");
            BatchTrainer bt2train = prefetchAndGetBatchTrainer(tagger2, trainData, modelPath + ".level2.prefetchedTrainData");

            bt2train.train(fixedNumIterations);
        }

        logger.info("Saving model to path: " + modelPath);
        tagger1.save();
        tagger2.save();
    }

    public static Pair<Double, Double> RunTest(String testPath, String modelPath, String testlang) throws Exception {
        Data testData = loaddata(testPath, filesFormat, false);

        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
        NETaggerLevel2 tagger2 = new NETaggerLevel2(modelPath + ".level2", modelPath + ".level2.lex");

        Decoder.annotateDataBIO(testData, tagger1, tagger2);

        ArrayList<String> results = new ArrayList<>();
        results.add("word\tgold\tpred");
        for(NERDocument doc : testData.documents) {
            List<String> docpreds = new ArrayList<>();

            ArrayList<LinkedVector> sentences = doc.sentences;
            for (int k = 0; k < sentences.size(); k++){
                for (int i = 0; i < sentences.get(k).size() ; ++i){
                    NEWord w = (NEWord)sentences.get(k).get(i);
                    results.add(w.form + "\t" + w.neLabel + "\t" + w.neTypeLevel2);
                    docpreds.add(conllline(w.neTypeLevel2, i, w.form));
                }
                docpreds.add("");
            }

            //LineIO.write("/shared/corpora/ner/system-outputs/"+testlang+"/projection-fa/" + doc.docname, docpreds);
        }
        //logger.info("Just wrote to: " + "/shared/corpora/ner/system-outputs/"+testlang+ "/projection-fa/");

        LineIO.write("gold-pred-" + testlang + ".txt", results);

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
        System.out.println("******	Combined performance on all the datasets :");
        System.out.println("\t>>> Dataset path : \t" + testData.datasetPath);
        System.out.println("------------------------------------------------------------");

        System.out.println("Phrase-level Acc Level2:");
        resultsPhraseLevel2.printPerformance(System.out);
        System.out.println("Token-level Acc Level2:");
        resultsTokenLevel2.printPerformance(System.out);
        System.out.println("Phrase-level Acc Level1:");
        resultsPhraseLevel1.printPerformance(System.out);
        System.out.println("Token-level Acc Level1:");
        resultsTokenLevel1.printPerformance(System.out);

        System.out.println("------------------------------------------------------------");
        System.out.println("\t Level 1 F1 Phrase-level: " + resultsPhraseLevel1.getOverallStats()[2]);
        System.out.println("\t Level 2 F1 Phrase-level: " + resultsPhraseLevel2.getOverallStats()[2]);

        System.out.println("------------------------------------------------------------");
        System.out.println("************************************************************");
        System.out.println("------------------------------------------------------------");

        return new Pair<>(resultsPhraseLevel1.getOverallStats()[2], resultsPhraseLevel2.getOverallStats()[2]);

    }

    /**
     * This takes two sets of conll files, one prediction, one gold, and scores them.
     * @param predpath
     * @param goldpath
     * @return
     * @throws Exception
     */
    public static Pair<Double, Double> CompareWithGold(String predpath, String goldpath, String lang) throws Exception {

        HashMap<String, NERDocument> golddocs = new HashMap<>();
        Vector<NERDocument> docs= TaggedDataReader.readFolder(goldpath, filesFormat);
        for(int i=0;i<docs.size();i++) {
            NERDocument d = docs.elementAt(i);
            golddocs.put(d.docname, d);
        }
        logger.info("Found " + golddocs.size() + " gold docs");


        HashMap<String, NERDocument> preddocs = new HashMap<>();
        Vector<NERDocument> pdocs= TaggedDataReader.readFolder(predpath, filesFormat);
        for(int i=0;i<pdocs.size();i++) {
            NERDocument d = pdocs.elementAt(i);
            preddocs.put(d.docname, d);
        }
        logger.info("Found " + preddocs.size() + " pred docs");

        // somehow make Data object out of it.
        Data goldData = new Data();

        ArrayList<String> comparison = new ArrayList<>();
        comparison.add("word\tgold\tpred");

        int skipped = 0;

        for(String docid : golddocs.keySet()){
            boolean skipdoc = false;
            if(preddocs.containsKey(docid)){

                comparison.add("New doc: " + docid);

                NERDocument doc = golddocs.get(docid);
                NERDocument preddoc = preddocs.get(docid);

                ArrayList<LinkedVector> sentences = doc.sentences;
                ArrayList<LinkedVector> predsentences = preddoc.sentences;

                if(sentences.size() != predsentences.size()){
                    skipdoc = true;
                }

                for (int k = 0; k < sentences.size(); k++){
                    if(!skipdoc) {

                        List<Pair<Integer, Integer>> predspans = new ArrayList<>();
                        List<Pair<Integer, Integer>> goldspans = new ArrayList<>();
                        int startspan = -1;
                        int startgoldspan = -1;

                        String sent = "";

                        for (int i = 0; i < sentences.get(k).size(); ++i) {
                            NEWord w = (NEWord) sentences.get(k).get(i);
                            NEWord predw = (NEWord) predsentences.get(k).get(i);
                            sent += w.form + " ";

                            if (predw == null || !w.form.equals(predw.form)) {
                                //logger.debug("mismatching offsets.... skip this document.");
                                skipdoc = true;
                                break;
                            }

                            w.neTypeLevel1 = predw.neLabel;
                            w.neTypeLevel2 = predw.neLabel;


                            if(predw.neLabel.contains("B-")){
                                startspan = i;
                            }
                            if(startspan > -1 && predw.neLabel.equals("O")){
                                predspans.add(new Pair<>(startspan, i-1));
                                startspan = -1;
                            }

                            if(w.neLabel.contains("B-")){
                                startgoldspan = i;
                            }
                            if(startgoldspan > -1 && w.neLabel.equals("O")){
                                goldspans.add(new Pair<>(startgoldspan, i-1));
                                startgoldspan = -1;
                            }

                            // word, gold, pred
                            comparison.add(w.form + "\t" + w.neLabel + "\t" + predw.neLabel);

//                            // if they are not the same...
//                            if(!predw.neLabel.equals(w.neLabel)){
//                                // if predw is not O
//                                if(!predw.neLabel.equals("O") && !w.neLabel.equals("O")){
//                                    // fix it with some chance...
//                                    w.neTypeLevel1 = w.neLabel;
//                                    //w.neTypeLevel2 = w.neLabel;
//                                }
//                            }

                        }
                        comparison.add("");

                        // This code simulates what would happen if we "correct" the label of
                        // every predicted span that overlaps with a Gold span. This simulates
                        // the corrective actions of a native informant.
//                        for(Pair<Integer, Integer> pred : predspans){
//                            int a1 = pred.getFirst();
//                            int a2 = pred.getSecond();
//                            boolean overlap = false;
//                            for(Pair<Integer, Integer> gold : goldspans){
//                                int b1 = gold.getFirst();
//                                int b2 = gold.getSecond();
//                                if((a1 >= b1 && a1 <= b2) || (a2 >= b1 && a2 <= b2)){
//                                    overlap = true;
//                                    // overlap is true
//                                    // set labels of predspan to label of gold span.
//                                    NEWord w = (NEWord) sentences.get(k).get(b1);
//                                    String goldlabel = w.neLabel.split("-")[1];
//
//                                    for(int i = a1; i <= a2; i++){
//                                        NEWord pw = (NEWord) sentences.get(k).get(i);
//                                        pw.neTypeLevel1 = i==a1 ? "B-" + goldlabel : "I-" + goldlabel;
//                                    }
//                                }
//                            }
//
//                            // if no overlap...
//                            if (!overlap) {
//                                for (int i = a1; i < a2; i++) {
//                                    NEWord pw = (NEWord) sentences.get(k).get(i);
//                                    pw.neTypeLevel1 = "O";
//                                }
//                            }
//
//
//                        }


                    }
                }
                comparison.add("");
                // some documents need to be skipped.
                if(!skipdoc) {
                    goldData.documents.add(doc);
                }else{
                    logger.info("Skipping this doc: " + docid);
                    skipped++;
                }
            }
        }

        String outfilename = "goldcompare-"+lang+".txt";
        logger.debug("writing out to: " + outfilename);
        LineIO.write(outfilename, comparison);

        logger.info("docs in intersection: " + goldData.documents.size());
        logger.info("docs skipped: " + skipped );

        logger.info("IGNORING MISC LABEL IN EVAL");
        String[] labs = {"MISC"};
        goldData.setLabelsToIgnore(labs);

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

        NETesterMultiDataset.reportPredictions(goldData,
                resultsTokenLevel1,
                resultsTokenLevel2,
                resultsPhraseLevel1,
                resultsPhraseLevel2,
                resultsByBILOU,
                resultsSegmentation);

        System.out.println("------------------------------------------------------------");
        System.out.println("******	Combined performance on all the datasets :");
        System.out.println("\t>>> Dataset path : \t" + goldData.datasetPath);
        System.out.println("------------------------------------------------------------");

        System.out.println("Phrase-level Acc Level2:");
        resultsPhraseLevel2.printPerformance(System.out);
        System.out.println("Token-level Acc Level2:");
        resultsTokenLevel2.printPerformance(System.out);
        System.out.println("Phrase-level Acc Level1:");
        resultsPhraseLevel1.printPerformance(System.out);
        System.out.println("Token-level Acc Level1:");
        resultsTokenLevel1.printPerformance(System.out);

        System.out.println("------------------------------------------------------------");
        System.out.println("\t Level 1 F1 Phrase-level: " + resultsPhraseLevel1.getOverallStats()[2]);
        System.out.println("\t Level 2 F1 Phrase-level: " + resultsPhraseLevel2.getOverallStats()[2]);

        System.out.println("------------------------------------------------------------");
        System.out.println("************************************************************");
        System.out.println("------------------------------------------------------------");

        return new Pair<>(resultsPhraseLevel1.getOverallStats()[2], resultsPhraseLevel2.getOverallStats()[2]);

    }



    public static class DataSetReader implements Parser {
        public Data dataset = null;
        int docid = 0;
        int sentenceId =0;
        int tokenId=0;
        int generatedSamples = 0;

        public DataSetReader(Data dataset) {
            this.dataset = dataset;
        }

        public void close() {
            // do nothing
        }

        public Object next() {
            if(docid >= dataset.documents.size()){
                return null;
            }

            Object res =  dataset.documents.get(docid).sentences.get(sentenceId).get(tokenId);
            if(tokenId < dataset.documents.get(docid).sentences.get(sentenceId).size()-1)
                tokenId++;
            else {
                tokenId=0;
                if(sentenceId<dataset.documents.get(docid).sentences.size()-1) {
                    sentenceId++;
                } else {
                    sentenceId=0;
                    docid++;
                }
            }
            generatedSamples++;

            return res;
        }
        public void reset() {
            sentenceId =0;
            tokenId=0;
            generatedSamples = 0;
            docid=0;
        }
    }


}
