package edu.illinois.cs.cogcomp.ner.LbjTagger;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.StringUtils;
import edu.illinois.cs.cogcomp.lbjava.classify.TestDiscrete;
import edu.illinois.cs.cogcomp.lbjava.learn.BatchTrainer;
import edu.illinois.cs.cogcomp.lbjava.learn.SparseNetworkLearner;
import edu.illinois.cs.cogcomp.lbjava.learn.WeightedBatchTrainer;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedChild;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.illinois.cs.cogcomp.lbjava.parse.WeightedParser;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.TwoLayerPredictionAggregationFeatures;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.PredictionsAndEntitiesConfidenceScores;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel2;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataReader;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataWriter;
import edu.illinois.cs.cogcomp.ner.WordEmbedding;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * ACLRunner class. For ACL2016
 * Created by mayhew2 on 3/15/16.
 */
@SuppressWarnings("Duplicates")
public class LORELEIRunner {

    private static final Logger logger = LoggerFactory.getLogger(LORELEIRunner.class);
    public static final String filesFormat = "-c";

    public static String config = null;

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        Option help = new Option( "help", "print this message" );

        Option configfile = Option.builder("cf")
                .argName("config")
                .hasArg()
                .required()
                .build();

        Option trainpath = Option.builder("train")
                .argName("path")
                .hasArg()
                .build();

        Option testpath = Option.builder("test")
                .argName("path")
                .hasArg()
                .required()
                .build();

        Option langopt = Option.builder("lang")
                .argName("name")
                .hasArg()
                .required()
                .build();


        options.addOption(help);
        options.addOption(trainpath);
        options.addOption(testpath);
        options.addOption(langopt);
        options.addOption(configfile);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if(cmd.hasOption("cf")){
            config = cmd.getOptionValue("cf");
        }

        boolean areWeTraining = cmd.hasOption("train");
        Parameters.readConfigAndLoadExternalData(config, areWeTraining);
        String modelpath = ParametersForLbjCode.currentParameters.pathToModelFile;

        if(ParametersForLbjCode.currentParameters.featuresToUse.containsKey("Embedding")) {
            if(ParametersForLbjCode.currentParameters.testlang.equals("en"))
                WordEmbedding.setMonoVecsNew("en");
            else
                WordEmbedding.loadMultiDBNew(ParametersForLbjCode.currentParameters.testlang);
        }

        if(cmd.hasOption("train")){
            int trainiter = 30;
            String trainroot = cmd.getOptionValue("train");
            Data trainData = loaddata(trainroot, filesFormat, true);
            RunTraining(trainData, trainiter, modelpath);
            System.out.println("Trained on: " + trainroot);
        }

        // should be always... it's required.
        if(cmd.hasOption("test")){
            String testroot = cmd.getOptionValue("test");
            String lang = cmd.getOptionValue("lang");
            Data testData = loaddata(testroot, filesFormat, false);
            Pair<Double, Double> levels  = RunTest(testData, modelpath, lang);
            System.out.println("Tested on: " + testroot);
        }


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
    private static WeightedBatchTrainer prefetchAndGetBatchTrainer(SparseNetworkLearner classifier, Data dataSet, String exampleStorePath) {
        //logger.info("Pre-extracting the training data for Level 1 classifier");

        TextChunkRepresentationManager.changeChunkRepresentation(
                TextChunkRepresentationManager.EncodingScheme.BIO,
                ParametersForLbjCode.currentParameters.taggingEncodingScheme,
                dataSet, NEWord.LabelToLookAt.GoldLabel);


        // FIXME: this is where we set the progressOutput var for the BatchTrainer
        WeightedBatchTrainer bt = new WeightedBatchTrainer(classifier, new DataSetReader(dataSet), 50000);

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
        WeightedBatchTrainer bt1train = prefetchAndGetBatchTrainer(tagger1, trainData, arg);

        logger.info("Training...");
        bt1train.train(fixedNumIterations);

        NETaggerLevel2 tagger2 = new NETaggerLevel2(modelPath + ".level2", modelPath + ".level2.lex");
        tagger2.forget();

        // Previously checked if PatternFeatures was in featuresToUse.
        if (ParametersForLbjCode.currentParameters.featuresToUse.containsKey("PredictionsLevel1")) {

            logger.info("Pre-extracting the training data for Level 2 classifier");
            WeightedBatchTrainer bt2train = prefetchAndGetBatchTrainer(tagger2, trainData, modelPath + ".level2.prefetchedTrainData");

            bt2train.train(fixedNumIterations);
        }

        logger.info("Saving model to path: " + modelPath);
        tagger1.save();
        tagger2.save();
    }

    public static Pair<Double, Double> RunTest(Data testData, String modelPath, String testlang) throws Exception {

        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
        NETaggerLevel2 tagger2 = new NETaggerLevel2(modelPath + ".level2", modelPath + ".level2.lex");

        Decoder.annotateDataBIO(testData, tagger1, tagger2);

        ArrayList<String> results = new ArrayList<>();
        results.add("word\tgold\tpred");
        for(NERDocument doc : testData.documents) {
            List<String> docpreds = new ArrayList<>();

            ArrayList<LinkedVector> sentences = doc.sentences;
            results.add(doc.docname);
            for (int k = 0; k < sentences.size(); k++){
                for (int i = 0; i < sentences.get(k).size() ; ++i){
                    NEWord w = (NEWord)sentences.get(k).get(i);
                    if(!w.neLabel.equals(w.neTypeLevel2)) {
                        results.add("***" + w.form + "\t" + w.neLabel + "\t" + w.neTypeLevel2);
                    }else{
                        results.add(w.form + "\t" + w.neLabel + "\t" + w.neTypeLevel2);
                    }
                    docpreds.add(ACLRunner.conllline(w.neTypeLevel2, i, w.form));
                }
                results.add("");
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

    public static class DataSetReader extends WeightedParser {
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

            NEWord res =  (NEWord) dataset.documents.get(docid).sentences.get(sentenceId).get(tokenId);

            if (res.neLabel.equals("O")){
                res.setWeight(0.001);
            }

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
