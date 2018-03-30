package edu.illinois.cs.cogcomp.ner.LbjTagger;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.StringUtils;
import edu.illinois.cs.cogcomp.lbjava.classify.Feature;
import edu.illinois.cs.cogcomp.lbjava.classify.TestDiscrete;
import edu.illinois.cs.cogcomp.lbjava.learn.Lexicon;
import edu.illinois.cs.cogcomp.lbjava.learn.SparseNetworkLearner;
import edu.illinois.cs.cogcomp.lbjava.learn.WeightedBatchTrainer;
import edu.illinois.cs.cogcomp.lbjava.learn.WeightedSparseAveragedPerceptron;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.illinois.cs.cogcomp.lbjava.parse.WeightedParser;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.TwoLayerPredictionAggregationFeatures;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.PredictionsAndEntitiesConfidenceScores;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel2;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataWriter;
import edu.illinois.cs.cogcomp.ner.WordEmbedding;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * This writes output to LIBSVM (libsvm) format features.
 *
 * ACLRunner class. For ACL2016
 * Created by mayhew2 on 3/15/16.
 */
@SuppressWarnings("Duplicates")
public class FeatureWriter {

    private static final Logger logger = LoggerFactory.getLogger(FeatureWriter.class);
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

        options.addOption(help);
        options.addOption(trainpath);
        options.addOption(testpath);
        options.addOption(configfile);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if(cmd.hasOption("cf")){
            config = cmd.getOptionValue("cf");
        }

        //boolean areWeTraining = cmd.hasOption("train");
        Parameters.readConfigAndLoadExternalData(config, true);
        String modelpath = ParametersForLbjCode.currentParameters.pathToModelFile;
        if(modelpath.startsWith("tmp") || modelpath.length() == 0){
            Random r = new Random();
            modelpath = "/tmp/nermodel" + r.nextInt();
        }

        // This should all happen only once!!!!
        NETaggerLevel1 classifier = new NETaggerLevel1(modelpath + ".level1", modelpath + ".level1.lex");

        //logger.info("Pre-extracting the training data for Level 1 classifier");

        if(cmd.hasOption("train")){
            String trainroot = cmd.getOptionValue("train");
            Data trainData = loaddata(trainroot, filesFormat, true);
            WriteData(trainData, modelpath, classifier, "data.train");
        }

        // should be always... it's required.
        if(cmd.hasOption("test")){
            String testroot = cmd.getOptionValue("test");
            Data testData = loaddata(testroot, filesFormat, false);
            WriteData(testData, modelpath, classifier, "data.test");
        }
    }


    private static void WriteData(Data dataSet, String modelpath, NETaggerLevel1 classifier, String outpath) throws IOException {

        System.out.println("got here...");

        TextChunkRepresentationManager.changeChunkRepresentation(
                TextChunkRepresentationManager.EncodingScheme.BIO,
                ParametersForLbjCode.currentParameters.taggingEncodingScheme,
                dataSet,
                NEWord.LabelToLookAt.GoldLabel);

        WeightedBatchTrainer bt = new WeightedBatchTrainer(classifier, new DataSetReader(dataSet), 50000);

        String exampleStorePath = modelpath + ".level1.prefetchedTrainData";
        Lexicon lexicon = bt.preExtract(exampleStorePath);
        classifier.setLexicon(lexicon);

        Parser parser = bt.getParser();

        List<String> out = new ArrayList<>();
        for (Object example = parser.next(); example != null; example = parser.next()) {
            Object[] earray = classifier.getExampleArray(example);
            int[] keys = (int[]) earray[0];
            double[] values = (double[]) earray[1];
            int[] labels = (int[]) earray[2];
            double[] labelvals = (double[]) earray[3];
            double weight = (double) earray[4];

            if(weight > 0){
                String label = classifier.getLabelLexicon().lookupKey(labels[0]).getStringValue();
                String outstring = label + " bias:1.0 ";
                // how to get the name of the label?
                for(int i = 0; i < keys.length; i++) {
                    Feature f = lexicon.lookupKey(keys[i]);

                    String nn = f.toStringNoPackage().replaceAll(":", "_").replaceAll("\\s+", "").replaceAll("#", "_");

                    outstring += nn + ":" + weight * values[i] + " ";
                }

                out.add(outstring.trim());
            }
        }
        System.out.println("also go there");

        LineIO.write(outpath, out);
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
