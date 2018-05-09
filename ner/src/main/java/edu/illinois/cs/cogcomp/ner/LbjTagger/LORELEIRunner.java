package edu.illinois.cs.cogcomp.ner.LbjTagger;

import com.google.gson.JsonSyntaxException;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.*;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.core.utilities.StringUtils;
import edu.illinois.cs.cogcomp.lbjava.classify.TestDiscrete;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
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
import org.apache.commons.lang.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.soap.Text;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LORELEIRunner class. For LORELEI 2018
 * Created by mayhew2 on 3/15/16.
 */
@SuppressWarnings("Duplicates")
public class LORELEIRunner {

    private static final Logger logger = LoggerFactory.getLogger(LORELEIRunner.class);
    public static String filesFormat = "-c";

    public static String config = null;

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        Option help = new Option( "help", "print this message" );

        Option configfile = Option.builder("cf")
                .hasArg()
                .required()
                .build();

        Option trainpath = Option.builder("train")
                .hasArg()
                .build();

        Option testpath = Option.builder("test")
                .hasArg()
                .required()
                .build();

        Option langopt = Option.builder("lang")
                .hasArg()
                .required()
                .build();

        Option formatopt = Option.builder("format")
                .hasArg()
                .desc("Choose between reading conll files and reading from serialized TAs")
                .build();


        options.addOption(help);
        options.addOption(trainpath);
        options.addOption(testpath);
        options.addOption(langopt);
        options.addOption(formatopt);

        options.addOption(configfile);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if(cmd.hasOption("cf")){
            config = cmd.getOptionValue("cf");
        }

        boolean areWeTraining = cmd.hasOption("train");
        Parameters.readConfigAndLoadExternalData(config, areWeTraining);
        String modelpath = ParametersForLbjCode.currentParameters.pathToModelFile;
        if(modelpath.startsWith("tmp") || modelpath.length() == 0){
            Random r = new Random();
            modelpath = "/tmp/nermodel" + r.nextInt();
        }


        if(ParametersForLbjCode.currentParameters.featuresToUse.containsKey("Embedding")) {
            if(ParametersForLbjCode.currentParameters.testlang.equals("en"))
                WordEmbedding.setMonoVecsNew("en");
            else
                WordEmbedding.loadMultiDBNew(ParametersForLbjCode.currentParameters.testlang);
        }

        if(cmd.hasOption("format")){
            filesFormat = cmd.getOptionValue("format");
            System.out.println(filesFormat);
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
            Pair<Double, Double> levels  = RunTest(testData, modelpath, lang, testroot);
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
        Data data;

        if (filesFormat.equals("ta")) {
            List<TextAnnotation> tas = new ArrayList<>();
            for(String path : paths){
                File[] files = (new File(path)).listFiles();
                for(File file : files){
                    TextAnnotation ta = null;
                    boolean fileread = false;
                    try {
                        ta = SerializationHelper.deserializeTextAnnotationFromFile(file.getPath(), true);
                    }catch (Exception e) { }

                    try {
                        ta = SerializationHelper.deserializeTextAnnotationFromFile(file.getPath());
                    }catch (Exception e){ }

                    if(ta != null){
                        tas.add(ta);
                    }else{
                        System.out.println("Skipping: " + file.getPath());
                    }

                }
            }
            data = loaddataFromTAs(tas);
        }else if(filesFormat.equals("-c")) {
            // Get train data
            String first = paths[0];
            data = new Data(first, first, filesFormat, new String[]{}, new String[]{});
            for (int i = 1; i < paths.length; i++) {
                data.addFolderToData(paths[i], filesFormat);
            }
        }else{
            throw new IllegalArgumentException("format " + filesFormat + " not recognized");
        }

        ExpressiveFeaturesAnnotator.train = train;
        ExpressiveFeaturesAnnotator.annotate(data);
        data.setLabelsToIgnore(ParametersForLbjCode.currentParameters.labelsToIgnoreInEvaluation);

        return data;
    }

    /**
     * NER Code uses the Data object to run. This converts TextAnnotations into a Data object.
     * Important: this creates data with BIO labeling.
     *
     * @param tas list of text annotations
     */
    public static Data loaddataFromTAs(List<TextAnnotation> tas) throws Exception {

        Data data = new Data();
        for(TextAnnotation ta : tas) {
            // convert this data structure into one the NER package can deal with.
            ArrayList<LinkedVector> sentences = new ArrayList<>();
            String[] tokens = ta.getTokens();

            View ner;
            if(ta.hasView(ViewNames.NER_CONLL)){
                ner = ta.getView(ViewNames.NER_CONLL);
            }else{
                ner = new View(ViewNames.NER_CONLL, "Ltf2TextAnnotation",ta,1.0);
                ta.addView(ViewNames.NER_CONLL, ner);
            }

            View lemma = null;
            if(ta.hasView(ViewNames.LEMMA)){
                lemma = ta.getView(ViewNames.LEMMA);
            }

            int[] tokenindices = new int[tokens.length];
            int tokenIndex = 0;
            int neWordIndex = 0;
            for (int i = 0; i < ta.getNumberOfSentences(); i++) {
                Sentence sentence = ta.getSentence(i);
                int sentstart = sentence.getStartSpan();

                LinkedVector words = new LinkedVector();

                for(int k = 0; k < sentence.size(); k++){
                    int tokenid = sentstart+k;

                    String w = sentence.getToken(k);

                    if(lemma != null){
                        List<Constituent> lemmacons = lemma.getConstituentsCoveringToken(tokenid);
                        if(lemmacons.size() > 0) {
                            Constituent lemmacon = lemmacons.get(0);
                            String oldw = w;
                            w = lemmacon.getLabel();
                            System.out.println("Using lemma: " + oldw + " -> " + w);
                        }
                    }

                    List<Constituent> cons = ner.getConstituentsCoveringToken(tokenid);
                    if(cons.size() > 1){
                        logger.error("Too many constituents for token " + tokenid + ", choosing just the first.");
                    }

                    String tag = "O";

                    if(cons.size() > 0) {
                        Constituent c = cons.get(0);
                        if(tokenid == c.getSpan().getFirst())
                            tag = "B-" + c.getLabel();
                        else
                            tag = "I-" + c.getLabel();
                    }

                    if (w.length() > 0) {
                        NEWord.addTokenToSentence(words, w, tag);
                        tokenindices[neWordIndex] = tokenIndex;
                        neWordIndex++;
                    } else {
                        logger.error("Bad (zero length) token.");
                    }
                    tokenIndex++;
                }
                if (words.size() > 0)
                    sentences.add(words);
            }
            NERDocument doc = new NERDocument(sentences, ta.getId());
            data.documents.add(doc);
        }

        return data;
    }

    /**
     * Assume data is annotated at this point. This will add an NER view to the TAs.
     * @param data
     * @param tas
     */
    public static void Data2TextAnnotation(Data data, List<TextAnnotation> tas) {

        HashMap<String, TextAnnotation> id2ta = new HashMap<>();
        for(TextAnnotation ta : tas){
            id2ta.put(ta.getId(), ta);
        }

        for(NERDocument doc : data.documents) {
            String docid = doc.docname;

            TextAnnotation ta = id2ta.get(docid);
            ArrayList<LinkedVector> nerSentences = doc.sentences;
            SpanLabelView nerView = new SpanLabelView(ViewNames.NER_CONLL, ta);

            // each LinkedVector in data corresponds to a sentence.
            int tokenoffset = 0;
            for (LinkedVector sentence : nerSentences) {
                boolean open = false;

                // there should be a 1:1 mapping btw sentence tokens in record and words/predictions
                // from NER.
                int startIndex = -1;
                String label = null;
                for (int j = 0; j < sentence.size(); j++, tokenoffset++) {
                    NEWord neWord = (NEWord) (sentence.get(j));
                    String prediction = neWord.neTypeLevel2;

                    // LAM-tlr this is not a great way to ascertain the entity type, it's a bit
                    // convoluted, and very
                    // inefficient, use enums, or nominalized indexes for this sort of thing.
                    if (prediction.startsWith("B-")) {
                        startIndex = tokenoffset;
                        label = prediction.substring(2);
                        open = true;
                    } else if (j > 0) {
                        String previous_prediction = ((NEWord) sentence.get(j - 1)).neTypeLevel2;
                        if (prediction.startsWith("I-")
                                && (!previous_prediction.endsWith(prediction.substring(2)))) {
                            startIndex = tokenoffset;
                            label = prediction.substring(2);
                            open = true;
                        }
                    }

                    if (open) {
                        boolean close = false;
                        if (j == sentence.size() - 1) {
                            close = true;
                        } else {
                            String next_prediction = ((NEWord) sentence.get(j + 1)).neTypeLevel2;
                            if (next_prediction.startsWith("B-"))
                                close = true;
                            if (next_prediction.equals("O"))
                                close = true;
                            if (next_prediction.indexOf('-') > -1
                                    && (!prediction.endsWith(next_prediction.substring(2))))
                                close = true;
                        }
                        if (close) {
                            nerView.addSpanLabel(startIndex, tokenoffset+1, label, 1d);
                            open = false;
                        }
                    }
                }
            }
            ta.addView(ViewNames.NER_CONLL, nerView);
        }
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
                dataSet,
                NEWord.LabelToLookAt.GoldLabel);

        // FIXME: this is where we set the progressOutput var for the BatchTrainer
        WeightedBatchTrainer bt = new WeightedBatchTrainer(classifier, new DataSetReader(dataSet), 50000);

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
        //NETaggerLevel1 tagger1 = new NETaggerLevel1();

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

        for(Object o : tagger1.getNetwork().toArray()){
            WeightedSparseAveragedPerceptron wsap = (WeightedSparseAveragedPerceptron) o;
            System.out.println("Bias: " + wsap.getBias());

        };

        logger.info("Saving model to path: " + modelPath);
        tagger1.save();
        tagger2.save();
    }

    public static Pair<Double, Double>
    RunTest(Data testData, String modelPath, String testlang, String datapath) throws Exception {

        NETaggerLevel1 tagger1 = new NETaggerLevel1(modelPath + ".level1", modelPath + ".level1.lex");
        NETaggerLevel2 tagger2 = new NETaggerLevel2(modelPath + ".level2", modelPath + ".level2.lex");

        Decoder.annotateDataBIO(testData, tagger1, tagger2);

        ArrayList<String> results = new ArrayList<>();
        for(NERDocument doc : testData.documents) {
            List<String> docpreds = new ArrayList<>();

            ArrayList<LinkedVector> sentences = doc.sentences;
            //results.add(doc.docname);
            for (int k = 0; k < sentences.size(); k++){
                for (int i = 0; i < sentences.get(k).size() ; ++i){
                    NEWord w = (NEWord)sentences.get(k).get(i);
                    if(!w.neLabel.equals(w.neTypeLevel2)) {
                        results.add(w.form + " " + w.neLabel + " " + w.neTypeLevel2);
                    }else{
                        results.add(w.form + " " + w.neLabel + " " + w.neTypeLevel2);
                    }
                    docpreds.add(ACLRunner.conllline(w.neTypeLevel2, i, w.form));
                }
                results.add("");
                docpreds.add("");
            }

            //LineIO.write("/shared/corpora/ner/system-outputs/"+testlang+"/projection-fa/" + doc.docname, docpreds);
        }
        //logger.info("Just wrote to: " + "/shared/corpora/ner/system-outputs/"+testlang+ "/projection-fa/");


        String[] paths = datapath.split(",");
        List<TextAnnotation> tas = new ArrayList<>();
        for(String path : paths){
            File[] files = (new File(path)).listFiles();
            for(File file : files){
                try {
                    TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(file.getPath(), true);
                    tas.add(ta);
                }catch(IllegalArgumentException e){
                    System.out.println("Skipping: " + file);
                }
            }
        }
        Data2TextAnnotation(testData, tas);

        List<String> tablines = new ArrayList<>();
        for(TextAnnotation ta : tas){
            if(ta.hasView(ViewNames.NER_CONLL)) {
                View ner = ta.getView(ViewNames.NER_CONLL);
                View roman = null;
                if(ta.hasView(ViewNames.TRANSLITERATION)) {
                    roman = ta.getView(ViewNames.TRANSLITERATION);
                }
                for (Constituent c : ner.getConstituents()) {
                    String romanstring = c.getSurfaceForm();
                    if(roman != null) {
                        List<Constituent> romantoks = roman.getConstituentsCoveringSpan(c.getStartSpan(), c.getEndSpan());
                        List<String> toks = romantoks.stream().map(Constituent::getLabel).collect(Collectors.toList());
                        romanstring = StringUtils.join(" ", toks);
                    }

                    String menid = ta.getId() + ":" + c.getStartCharOffset() + "-" + (c.getEndCharOffset()-1);
                    String line = String.format("Penn\t%s\t%s\t%s\tNULL\t%s\tNAM\t1.0", ta.getId(), romanstring, menid, c.getLabel());
                    tablines.add(line);
                }
            }
        }
        LineIO.write("tabresults.tab", tablines);


        String outdatapath = modelPath + ".testdata";
        TaggedDataWriter.writeToFile(outdatapath, testData, "-c", NEWord.LabelToLookAt.PredictionLevel2Tagger);

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
