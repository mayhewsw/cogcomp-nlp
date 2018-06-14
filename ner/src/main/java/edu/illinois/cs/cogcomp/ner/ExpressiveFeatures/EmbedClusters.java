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
package edu.illinois.cs.cogcomp.ner.ExpressiveFeatures;

import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.IO.InFile;
import edu.illinois.cs.cogcomp.ner.IO.ResourceUtilities;
import edu.illinois.cs.cogcomp.ner.LbjTagger.Data;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.ner.LbjTagger.ParametersForLbjCode;
import gnu.trove.map.hash.THashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;


public class EmbedClusters {

    /** the sole instance of this class. */
    private static EmbedClusters embedClusters = null;

    /**
     * This method should never be called before init, or the gazetteer will not be initialized.
     *
     * @return the singleton instance of the Gazetteers class.
     */
    static public EmbedClusters get() {
        return embedClusters;
    }

    /** ensures singleton-ness. */
    private EmbedClusters() {

    }

    private static HashMap<String, String> word2cluster = null;

    public static void init(String pathToClusterFile) {
        if (embedClusters != null) {
            return;
        }
        embedClusters = new EmbedClusters();

        embedClusters.word2cluster = new HashMap<>();

        InFile in = new InFile(ResourceUtilities.loadResource(pathToClusterFile));
        String line = in.readLine();
        int wordsAdded = 0;
        while (line != null) {
            StringTokenizer st = new StringTokenizer(line);
            String cluster = st.nextToken();
            String word = st.nextToken();
            word2cluster.put(word, cluster);
            wordsAdded++;
            line = in.readLine();
        }
        if (ParametersForLbjCode.currentParameters.debug) {
            System.out.println(wordsAdded + " words added");
        }
        in.close();

    }

    /**
     * @return the resource names array.
     */
    final public ArrayList<String> getResources() {
        return new ArrayList<>();
    }

    final public String getCluster(NEWord w) {
        return getCluster(w.form);
    }

    final public String getCluster(String word) {

        if(embedClusters.word2cluster.containsKey(word)){
            return embedClusters.word2cluster.get(word);
        }
        return "-1";
    }

    public static void main(String[] args) {
        /*
         * Vector<String> resources=new Vector<>();
         * resources.addElement("Data/BrownHierarchicalWordClusters/brownBllipClusters");
         * Vector<Integer> thres=new Vector<>(); thres.addElement(5); Vector<Boolean> lowercase=new
         * Vector<>(); lowercase.addElement(false); init(resources,thres,lowercase);
         * System.out.println("finance "); printArr(getPrefixes(new NEWord(new
         * Word("finance"),null,null))); System.out.println("help"); printArr(getPrefixes(new
         * NEWord(new Word("help"),null,null))); System.out.println("resque ");
         * printArr(getPrefixes(new NEWord(new Word("resque"),null,null)));
         * System.out.println("assist "); printArr(getPrefixes(new NEWord(new
         * Word("assist"),null,null))); System.out.println("assistance "); printArr(getPrefixes(new
         * NEWord(new Word("assistance"),null,null))); System.out.println("guidance ");
         * printArr(getPrefixes(new NEWord(new Word("guidance"),null,null)));
         */
    }
}
