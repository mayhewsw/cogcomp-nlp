package edu.illinois.cs.cogcomp.ner.LbjTagger;

import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.lbjava.learn.SparseNetworkLearner;
import edu.illinois.cs.cogcomp.lbjava.learn.WeightedBatchTrainer;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.TwoLayerPredictionAggregationFeatures;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.PredictionsAndEntitiesConfidenceScores;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataWriter;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * ACLRunner class. For ACL2016
 * Created by mayhew2 on 3/15/16.
 */
@SuppressWarnings("Duplicates")
public class CODLRunner {

    private static final Logger logger = LoggerFactory.getLogger(CODLRunner.class);
    public static final String filesFormat = "-c";

    public static String config = null;
    private static boolean dupe = false;

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

        Option inputpath = Option.builder("input")
                .argName("inputpath")
                .hasArg()
                .required()
                .build();

        Option outputpath = Option.builder("output")
                .argName("outputpath")
                .hasArg()
                .required()
                .build();

        Option langopt = Option.builder("lang")
                .argName("name")
                .hasArg()
                .required()
                .build();

        Option dupeopt = Option.builder("dupe")
                .argName("dupe")
                .build();

        options.addOption(help);
        options.addOption(trainpath);
        options.addOption(inputpath);
        options.addOption(outputpath);
        options.addOption(langopt);
        options.addOption(configfile);
        options.addOption(dupeopt);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if(cmd.hasOption("cf")){
            config = cmd.getOptionValue("cf");
        }

        if(cmd.hasOption("dupe")){
            logger.info("Will duplicate Os");
            dupe = true;
        }

        boolean areWeTraining = cmd.hasOption("train");
        Parameters.readConfigAndLoadExternalData(config, areWeTraining);
        String modelpath = "tmp"; //ParametersForLbjCode.currentParameters.pathToModelFile;
        if(modelpath.startsWith("tmp") || modelpath.length() == 0){
            Random r = new Random();
            modelpath = "/tmp/nermodel" + r.nextInt();
        }

        if(cmd.hasOption("train")){
            int trainiter = 30;
            String trainroot = cmd.getOptionValue("train");
            Data trainData = LORELEIRunner.loaddata(trainroot, filesFormat, true);
            RunTraining(trainData, trainiter, modelpath);
            System.out.println("Trained on: " + trainroot);
        }

        // should be always... it's required.

        String testroot = cmd.getOptionValue("input");
        Data testData = LORELEIRunner.loaddata(testroot, filesFormat, false);
        WriteOutput(testData, modelpath, cmd.getOptionValue("output"));
    }

//    /**
//     * Given a path to a folder, load the data from that folder. Assume the string is a comma
//     * separated list, perhaps of length one.
//     * @param datapath
//     * @param filesFormat
//     * @return
//     * @throws Exception
//     */
//    public static Data loaddata(String datapath, String filesFormat, boolean train) throws Exception {
//        // this used to be a comma, but I changed it...
//        String[] paths = datapath.split("@@@@@");
//        // Get train data
//        String first = paths[0];
//        Data data = new Data(first, first, filesFormat, new String[]{}, new String[]{});
//        for(int i = 1; i < paths.length; i++){
//            data.addFolderToData(paths[i], filesFormat);
//        }
//
//        ExpressiveFeaturesAnnotator.train = train;
//        ExpressiveFeaturesAnnotator.annotate(data);
//
//        data.setLabelsToIgnore(ParametersForLbjCode.currentParameters.labelsToIgnoreInEvaluation);
//
//        return data;
//    }

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
                dataSet,
                NEWord.LabelToLookAt.GoldLabel);

        // FIXME: this is where we set the progressOutput var for the BatchTrainer
        WeightedBatchTrainer bt = new WeightedBatchTrainer(classifier, new DataSetReader(dataSet, dupe), 0);

        classifier.setLexicon(bt.preExtract(exampleStorePath));

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
            else {
                logger.warn("deleting existing model path '" + modelPath + "'...");
                IOUtils.rmDir(modelPath);
                IOUtils.rm(modelPath + ".level1");
                IOUtils.rm(modelPath + ".level1.lex");
                IOUtils.rm(modelPath + ".level2");
            }
        }
        else
        {
            IOUtils.mkdir( modelPath );
        }


        IOUtils.cp(config, modelPath + "/" + FilenameUtils.getName(config));

        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
        tagger1.forget();
        //OnonOtagger tagger1 = new OnonOtagger();

        if (ParametersForLbjCode.currentParameters.featuresToUse.containsKey("PredictionsLevel1")) {
            PredictionsAndEntitiesConfidenceScores.getAndMarkEntities(trainData, NEWord.LabelToLookAt.GoldLabel);
            TwoLayerPredictionAggregationFeatures.setLevel1AggregationFeatures(trainData, true);
        }

        logger.info("Pre-extracting the training data for Level 1 classifier");
        String arg = modelPath + ".level1.prefetchedTrainData";
        WeightedBatchTrainer bt1train = prefetchAndGetBatchTrainer(tagger1, trainData, arg);

        logger.info("Training...");
        bt1train.train(fixedNumIterations);


        logger.info("Saving model to path: " + modelPath);
        tagger1.save();
    }


    public static void AnnotateData(Data testData, String modelPath) throws Exception {
        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");

        Decoder.annotateDataBIO(testData, tagger1, null);
    }




    /**
     * Outdatapath can be either a file or a folder.
     * @param testData
     * @param modelPath
     * @param outdatapath
     * @throws Exception
     */
    public static void WriteOutput(Data testData, String modelPath, String outdatapath) throws Exception {

        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");

        Decoder.annotateDataBIO(testData, tagger1, null);

        File out = new File(outdatapath);
        if(out.isDirectory()){
            for(NERDocument doc : testData.documents) {
                TaggedDataWriter.writeToFile(outdatapath + "/" + doc.docname, doc, "-c", NEWord.LabelToLookAt.PredictionLevel2Tagger);
                System.out.println("Wrote "+ doc.docname + " to " + outdatapath + "/" + doc.docname);
            }
        }else{
            if(testData.documents.size() > 1){
                System.err.println("Warning! There are multiple documents in testData, but only writing the first one.");
            }
            NERDocument doc = testData.documents.get(0);
            TaggedDataWriter.writeToFile(outdatapath, doc, "-c", NEWord.LabelToLookAt.PredictionLevel2Tagger);
            System.out.println("Wrote "+ doc.docname + " to " + outdatapath);
        }
    }

    public static class DataSetReader implements Parser {
        private boolean duplicateO = false;
        public Data dataset = null;
        int docid = 0;
        int sentenceId =0;
        int tokenId=0;
        int generatedSamples = 0;
        private boolean readytoduplicate = true;

        public DataSetReader(Data dataset) {
            this.dataset = dataset;
        }


        public DataSetReader(Data dataset, boolean dupe) {
            this.dataset = dataset;
            this.duplicateO = dupe;
        }

        public void close() {
            // do nothing
        }

        public Object next() {
            if(docid >= dataset.documents.size()){
                return null;
            }

            NEWord res =  (NEWord) dataset.documents.get(docid).sentences.get(sentenceId).get(tokenId);

            if(duplicateO) {
                if (res.neLabel.equals("O") && readytoduplicate) {
                    NEWord clone = (NEWord) res.clone();
                    clone.neLabel = "B-MNT";
                    clone.weight = 1 - clone.weight;
                    readytoduplicate = false;
                    return res;
                } else {
                    readytoduplicate = true;
                }
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




