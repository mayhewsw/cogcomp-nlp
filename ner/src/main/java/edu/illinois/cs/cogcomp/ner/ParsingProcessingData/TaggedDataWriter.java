/**
 * This software is released under the University of Illinois/Research and
 *  Academic Use License. See the LICENSE file in the root folder for details.
 * Copyright (c) 2016
 *
 * Developed by:
 * The Cognitive Computation Group
 * University of Illinois at Urbana-Champaign
 * http://cogcomp.cs.illinois.edu/
 */
package edu.illinois.cs.cogcomp.ner.ParsingProcessingData;

import edu.illinois.cs.cogcomp.ner.IO.OutFile;
import edu.illinois.cs.cogcomp.ner.LbjTagger.Data;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NERDocument;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.core.utilities.StringUtils;

import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import java.io.IOException;

public class TaggedDataWriter {
    public static void writeToFile(String outputFile, Data data, String fileFormat,
            NEWord.LabelToLookAt labelType) throws IOException {
        OutFile out = new OutFile(outputFile);
        if (fileFormat.equalsIgnoreCase("-r"))
            out.println(toBracketsFormat(data, labelType));
        else {
            if (fileFormat.equalsIgnoreCase("-c"))
                out.println(toColumnsFormat(data, labelType));
            else {
                throw new IOException(
                        "Unknown file format (only options -r and -c are supported): " + fileFormat);
            }
        }
        out.close();
    }

    public static void writeToFolder(String outputFolder, Data data, String fileFormat,
                                   NEWord.LabelToLookAt labelType) throws IOException {

        for(NERDocument doc : data.documents) {
            OutFile out = new OutFile(outputFolder + "/" + doc.docname);

            if (fileFormat.equalsIgnoreCase("-r"))
                System.out.println("SORRY NOT AVAILABLE!");
            else if (fileFormat.equalsIgnoreCase("-c"))
                out.println(toColumnsFormat(doc, labelType));
            else {
                throw new IOException(
                        "Unknown file format (only options -r and -c are supported): " + fileFormat);
            }

            out.close();
        }
    }


    /*
     * labelType=NEWord.GoldLabel/NEWord.PredictionLevel2Tagger/NEWord.PredictionLevel1Tagger
     * 
     * Note : the only reason this function is public is because we want to be able to use it in the
     * demo and insert html tags into the string
     */
    public static String toBracketsFormat(Data data, NEWord.LabelToLookAt labelType) {
        StringBuilder res = new StringBuilder(data.documents.size() * 1000);
        for (int did = 0; did < data.documents.size(); did++) {
            for (int i = 0; i < data.documents.get(did).sentences.size(); i++) {
                LinkedVector vector = data.documents.get(did).sentences.get(i);
                boolean open = false;
                String[] predictions = new String[vector.size()];
                String[] words = new String[vector.size()];
                for (int j = 0; j < vector.size(); j++) {
                    predictions[j] = null;
                    if (labelType == NEWord.LabelToLookAt.PredictionLevel2Tagger)
                        predictions[j] = ((NEWord) vector.get(j)).neTypeLevel2;
                    if (labelType == NEWord.LabelToLookAt.PredictionLevel1Tagger)
                        predictions[j] = ((NEWord) vector.get(j)).neTypeLevel1;
                    if (labelType == NEWord.LabelToLookAt.GoldLabel)
                        predictions[j] = ((NEWord) vector.get(j)).neLabel;
                    words[j] = ((NEWord) vector.get(j)).form;
                }
                for (int j = 0; j < vector.size(); j++) {
                    if (predictions[j].startsWith("B-")
                            || (j > 0 && predictions[j].startsWith("I-") && (!predictions[j - 1]
                                    .endsWith(predictions[j].substring(2))))) {
                        res.append("[").append(predictions[j].substring(2)).append(" ");
                        open = true;
                    }
                    res.append(words[j]).append(" ");
                    if (open) {
                        boolean close = false;
                        if (j == vector.size() - 1) {
                            close = true;
                        } else {
                            if (predictions[j + 1].startsWith("B-"))
                                close = true;
                            if (predictions[j + 1].equals("O"))
                                close = true;
                            if (predictions[j + 1].indexOf('-') > -1
                                    && (!predictions[j].endsWith(predictions[j + 1].substring(2))))
                                close = true;
                        }
                        if (close) {
                            res.append(" ] ");
                            open = false;
                        }
                    }
                }
                res.append("\n");
            }
        }
        return res.toString();
    }

    private static String toColumnsFormat(Data data, NEWord.LabelToLookAt labelType) {

//        double min = 1;
//        double max = 0;
//        for (int did = 0; did < data.documents.size(); did++) {
//            for (int i = 0; i < data.documents.get(did).sentences.size(); i++) {
//                LinkedVector vector = data.documents.get(did).sentences.get(i);
//                for (int j = 0; j < vector.size(); j++) {
//                    NEWord w = (NEWord) vector.get(j);
//                    double conf = w.predictionConfidencesLevel1Classifier.topScores.elementAt(0);
//                    if (w.getPrediction(labelType).equals("O")){
//                        if(conf > max){
//                            max = conf;
//                        }
//                        if(conf < min && conf > 0.9999){
//                            min = conf;
//                        }
//                    }
//                }
//
//            }
//        }
//
//        System.out.println("Max: " + max);
//        System.out.println("Min: " + min);


        StringBuilder res = new StringBuilder(data.documents.size() * 1000);
        for (int did = 0; did < data.documents.size(); did++) {
            for (int i = 0; i < data.documents.get(did).sentences.size(); i++) {
                LinkedVector vector = data.documents.get(did).sentences.get(i);
                if (((NEWord) vector.get(0)).previousIgnoreSentenceBoundary == null)
                    res.append("O	0	0	O	-X-	-DOCSTART-	x	x	0\n\n");
                for (int j = 0; j < vector.size(); j++) {
                    NEWord w = (NEWord) vector.get(j);

                    Vector<Double> topScores = w.predictionConfidencesLevel1Classifier.topScores;
                    Vector<String> topWords = w.predictionConfidencesLevel1Classifier.topWords;

                    List<String> weights = new ArrayList<>();
                    for(int k = 0; k < topScores.size(); k++){
                        double score = topScores.get(k);
                        String tag = topWords.get(k);
                        weights.add(tag + ":" + String.format( "%.2f", score ));
                    }
                    String weightstring = StringUtils.join(",", weights); 
                                        
                    res.append(w.getPrediction(labelType)).append("\t").append(j).append("\t" + w.start + "\t").append(w.end + "\t")
                            .append("O\t").append(w.form).append("\t" + weightstring).append("\tx\t0\n");
                }
                res.append("\n");
            }
        }
        return res.toString();
    }

    /**
     * This just takes a single document.
     * @param doc
     * @param labelType
     * @return
     */
    private static String toColumnsFormat(NERDocument doc, NEWord.LabelToLookAt labelType) {
        StringBuilder res = new StringBuilder(doc.sentences.size() * 100);

        for (int i = 0; i < doc.sentences.size(); i++) {
            LinkedVector vector = doc.sentences.get(i);
            if (((NEWord) vector.get(0)).previousIgnoreSentenceBoundary == null)
                res.append("O	0	0	O	-X-	-DOCSTART-	x	x	0\n\n");
            for (int j = 0; j < vector.size(); j++) {
                NEWord w = (NEWord) vector.get(j);
                res.append(w.getPrediction(labelType)).append("\t0\t").append(j)
                        .append("\tO\tO\t").append(w.form).append("\tx\tx\t0\n");
            }
            res.append("\n");
        }

        return res.toString();
    }


}
