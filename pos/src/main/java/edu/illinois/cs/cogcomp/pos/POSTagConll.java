package edu.illinois.cs.cogcomp.pos;

import edu.illinois.cs.cogcomp.lbjava.nlp.ColumnFormat;
import edu.illinois.cs.cogcomp.lbjava.nlp.SentenceSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.WordSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.PlainToTokenParser;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.illinois.cs.cogcomp.pos.lbjava.POSTagger;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import java.util.*;
import java.io.*;

/**
 * Created by mayhew2 on 9/13/16.
 */
public class POSTagConll {

    public static void main(String[] args) throws IOException {
        // Parse the command line
        if (args.length != 2) {
            System.err
                    .println("usage: java edu.illinois.cs.cogcomp.lbj.pos.POSTagConll <folder> <out folder>");
            System.exit(1);
        }

        String testingFolder= args[0];
        String outFolder = args[1];

        POSTagger tagger = new POSTagger();

        String[] fnames = (new File(testingFolder)).list();

        for(String fname : fnames) {
            Parser parser = new ColumnParser(testingFolder + "/" + fname);

            ArrayList<String> outlines = new ArrayList<>();

            for (LinkedVector sent = (LinkedVector) parser.next(); sent != null; sent = (LinkedVector) parser.next()) {

                Token word = (Token) sent.get(0);
                int tok = 0;
                String tag = tagger.discreteValue(word);
                outlines.add(word.label + "\t0\t" + tok++ + "\tx\t" + tag + "\t" + word.form + "\tx\tx\t0");

                while (word.next != null) {
                    word = (Token) word.next;
                    tag = tagger.discreteValue(word);
                    outlines.add(word.label + "\t0\t" + tok++ + "\tx\t" + tag + "\t" + word.form + "\tx\tx\t0");

                }
                outlines.add("");
            }

            LineIO.write(outFolder + "/" + fname, outlines);
        }
    }
}