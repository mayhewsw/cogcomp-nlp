package edu.illinois.cs.cogcomp.ner.LbjTagger;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("Duplicates")
public class Scorer {

    public static final String filesFormat = "-c";

    public static String config = null;

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        Option help = new Option( "help", "print this message" );

        Option goldpath = Option.builder("gold")
                .argName("path")
                .hasArg()
                .build();

        Option predpath = Option.builder("pred")
                .argName("path")
                .hasArg()
                .required()
                .build();

        options.addOption(help);
        options.addOption(goldpath);
        options.addOption(predpath);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String goldroot = cmd.getOptionValue("gold");
        Data goldData = loaddata(goldroot, filesFormat, true);
        
        String predroot = cmd.getOptionValue("pred");
        Data predData = loaddata(predroot, filesFormat, false);
        Pair<Double, Double> levels  = RunTest(goldData, predData);
        System.out.println("Tested on: " + predroot);
    
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

        return data;
    }

    public static Pair<Double, Double> RunTest(Data goldData, Data predData) throws Exception {

        ArrayList<NERDocument> golddocs = goldData.documents;
        ArrayList<NERDocument> preddocs = predData.documents;
        for(int i = 0; i < golddocs.size(); i++){
            NERDocument gd = golddocs.get(i);
            NERDocument pd = preddocs.get(i);
            for(int j = 0; j < gd.sentences.size(); j++){
                LinkedVector gs = gd.sentences.get(j);
                LinkedVector ps = pd.sentences.get(j);
                for(int k = 0; k < gs.size(); k++){
                    NEWord gw = (NEWord) gs.get(k);
                    NEWord pw = (NEWord) ps.get(k);
                    gw.neTypeLevel1 = pw.neLabel;
                    gw.neTypeLevel2 = pw.neLabel;
                }
            }
        }
        
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

}
