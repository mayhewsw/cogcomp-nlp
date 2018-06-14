//package edu.illinois.cs.cogcomp.ner.LbjTagger;
//
//import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
//import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
//import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
//import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
//import edu.illinois.cs.cogcomp.lbjava.classify.Score;
//import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;
//import edu.illinois.cs.cogcomp.lbjava.learn.SparseNetworkLearner;
//import edu.illinois.cs.cogcomp.lbjava.learn.WeightedBatchTrainer;
//import edu.illinois.cs.cogcomp.lbjava.parse.WeightedParser;
//import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
//import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
//import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel1;
//import edu.illinois.cs.cogcomp.ner.LbjFeatures.Typer;
//import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataWriter;
//import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
//import org.apache.commons.cli.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
///**
// * ACLRunner class. For ACL2016
// * Created by mayhew2 on 3/15/16.
// */
//@SuppressWarnings("Duplicates")
//public class TyperRunner {
//
//    private static final Logger logger = LoggerFactory.getLogger(TyperRunner.class);
//    public static final String filesFormat = "-c";
//
//    public static String config = null;
//
//    public static void main(String[] args) throws Exception {
//
//        Options options = new Options();
//        Option help = new Option( "help", "print this message" );
//
//        Option configfile = Option.builder("cf")
//                .argName("config")
//                .hasArg()
//                .required()
//                .build();
//
//        Option trainpath = Option.builder("train")
//                .argName("path")
//                .hasArg()
//                .build();
//
//        Option inputpath = Option.builder("input")
//                .argName("inputpath")
//                .hasArg()
//                .required()
//                .build();
//
//        Option outputpath = Option.builder("output")
//                .argName("outputpath")
//                .hasArg()
//                .required()
//                .build();
//
//        options.addOption(help);
//        options.addOption(trainpath);
//        options.addOption(inputpath);
//        options.addOption(outputpath);
//
//        CommandLineParser parser = new DefaultParser();
//        CommandLine cmd = parser.parse(options, args);
//
//        // String modelpath = "tmp"; //ParametersForLbjCode.currentParameters.pathToModelFile;
//        // if(modelpath.startsWith("tmp") || modelpath.length() == 0){
//        //     Random r = new Random();
//        //     modelpath = "/tmp/nermodel" + r.nextInt();
//        // }
//
//        String trainroot = cmd.getOptionValue("train");
//        String input = cmd.getOptionValue("input");
//        String output = cmd.getOptionValue("output");
//
//        RunTraining(trainroot, input, output);
//
//        // should be always... it's required.
//
//        //        String testroot = cmd.getOptionValue("input");
//        //        Data testData = loaddata(testroot, filesFormat, false);
//        //        WriteOutput(testData, modelpath, cmd.getOptionValue("output"));
//    }
//
//    /**
//     * Given a path to a folder, load the data from that folder. Assume the string is a comma
//     * separated list, perhaps of length one.
//     * @param datapath
//     * @param filesFormat
//     * @return
//     * @throws Exception
//     */
//    public static Data loaddata(String datapath, String filesFormat, boolean train) throws Exception {
//        String[] paths = datapath.split(",");
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
//
//    /**
//     * @param classifier
//     * @param dataSet
//     * @param exampleStorePath
//     * @return
//     */
//    private static WeightedBatchTrainer prefetchAndGetBatchTrainer(SparseNetworkLearner classifier, Data dataSet, String exampleStorePath) {
//        //logger.info("Pre-extracting the training data for Level 1 classifier");
//
//        TextChunkRepresentationManager.changeChunkRepresentation(
//                TextChunkRepresentationManager.EncodingScheme.BIO,
//                ParametersForLbjCode.currentParameters.taggingEncodingScheme,
//                dataSet,
//                NEWord.LabelToLookAt.GoldLabel);
//
//        // FIXME: this is where we set the progressOutput var for the BatchTrainer
//        WeightedBatchTrainer bt = new WeightedBatchTrainer(classifier, new DataSetReader(dataSet), 50000);
//
//        classifier.setLexicon(bt.preExtract(exampleStorePath));
//
//        return bt;
//    }
//
//
//    public static void RunTraining(String trainpath, String annotatepath, String outpath) throws Exception {
//
//        Typer typer = new Typer("blah", "blah");
//
//        int ctx = 2;
//
//        CoNLLNerReader reader = new CoNLLNerReader(trainpath);
//
//        List<Mention> mnts = new ArrayList<>();
//        while(reader.hasNext()){
//            TextAnnotation ta = reader.next();
//            View ner = ta.getView(ViewNames.NER_CONLL);
//            for(Constituent c : ner.getConstituents()){
//                String[] toks = c.getTokenizedSurfaceForm().split(" ");
//
//                int start = c.getStartSpan();
//                int end = c.getEndSpan();
//
//                String[] left = ta.getTokensInSpan(Math.max(0, start-ctx), start);
//                String[] right = ta.getTokensInSpan(end, Math.min(ta.size(), end + ctx));
//
//                Mention mnt = new Mention(toks, left, right, c.getLabel());
//                mnts.add(mnt);
//            }
//        }
//
//        Collections.shuffle(mnts);
//        // int split = (int) Math.ceil(0.8 * mnts.size());
//        // List<Mention> train = mnts.subList(0, split);
//        // List<Mention> test = mnts.subList(split, mnts.size());
//
//        int iterations = 30;
//        for(int i = 0; i < iterations; i++){
//            for(Mention mnt : mnts){
//                typer.learn(mnt);
//            }
//        }
//
//        CoNLLNerReader anno = new CoNLLNerReader(annotatepath);
//
//        List<TextAnnotation> tas = new ArrayList<>();
//
//        while(anno.hasNext()){
//            TextAnnotation ta = anno.next();
//            View ner = ta.getView(ViewNames.NER_CONLL);
//            List<Constituent> toAdd = new ArrayList<>();
//            for(Constituent c : ner.getConstituents()){
//                String[] toks = c.getTokenizedSurfaceForm().split(" ");
//
//                int start = c.getStartSpan();
//                int end = c.getEndSpan();
//
//                String[] left = ta.getTokensInSpan(Math.max(0, start-ctx), start);
//                String[] right = ta.getTokensInSpan(end, Math.min(ta.size(), end + ctx));
//
//                Mention mnt = new Mention(toks, left, right, c.getLabel());
//
//                String predLabel = typer.classify(mnt).getFeature(0).getStringValue();
//                double bestscore = typer.scores(mnt).getScore(predLabel).score;
//
//                if(bestscore > 10) {
//                    ner.removeConstituent(c);
//                    // this is what we have to do to change the label.
//                    Constituent c2 = new Constituent(predLabel, ViewNames.NER_CONLL, ta, start, end);
//                    toAdd.add(c2);
//                }
//
//            }
//            //ner.removeAllConsituents();
//            for(Constituent c2 : toAdd){
//                ner.addConstituent(c2);
//            }
//            tas.add(ta);
//        }
//
//        CoNLLNerReader.TaToConll(tas, outpath);
//
//    }
//
//    /**
//     * Outdatapath can be either a file or a folder.
//     * @param testData
//     * @param modelPath
//     * @param outdatapath
//     * @throws Exception
//     */
//    public static void WriteOutput(Data testData, String modelPath, String outdatapath) throws Exception {
//
//        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
//
//        Decoder.annotateDataBIO(testData, tagger1, null);
//
//        File out = new File(outdatapath);
//        if(out.isDirectory()){
//            for(NERDocument doc : testData.documents) {
//                TaggedDataWriter.writeToFile(outdatapath + "/" + doc.docname, testData, "-c", NEWord.LabelToLookAt.PredictionLevel2Tagger);
//                System.out.println("Wrote "+ doc.docname + " to " + outdatapath + "/" + doc.docname);
//            }
//        }else{
//            if(testData.documents.size() > 1){
//                System.err.println("Warning! There are multiple documents in testData, but only writing the first one.");
//            }
//            NERDocument doc = testData.documents.get(0);
//            TaggedDataWriter.writeToFile(outdatapath, testData, "-c", NEWord.LabelToLookAt.PredictionLevel2Tagger);
//            System.out.println("Wrote "+ doc.docname + " to " + outdatapath);
//        }
//    }
//
//    public static class DataSetReader extends WeightedParser {
//        public Data dataset = null;
//        int docid = 0;
//        int sentenceId =0;
//        int tokenId=0;
//        int generatedSamples = 0;
//
//        public DataSetReader(Data dataset) {
//            this.dataset = dataset;
//        }
//
//        public void close() {
//            // do nothing
//        }
//
//        public Object next() {
//            if(docid >= dataset.documents.size()){
//                return null;
//            }
//
//            NEWord res =  (NEWord) dataset.documents.get(docid).sentences.get(sentenceId).get(tokenId);
//
//            if(tokenId < dataset.documents.get(docid).sentences.get(sentenceId).size()-1)
//                tokenId++;
//            else {
//                tokenId=0;
//                if(sentenceId<dataset.documents.get(docid).sentences.size()-1) {
//                    sentenceId++;
//                } else {
//                    sentenceId=0;
//                    docid++;
//                }
//            }
//            generatedSamples++;
//
//            return res;
//        }
//        public void reset() {
//            sentenceId =0;
//            tokenId=0;
//            generatedSamples = 0;
//            docid=0;
//        }
//    }
//
//
//}
