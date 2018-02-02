package edu.illinois.cs.cogcomp.ner.LbjTagger;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.StringUtils;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import org.apache.commons.cli.*;

import java.util.*;

@SuppressWarnings("ALL")
public class CollinsTyper {

    // size of context window.
    static int ctx = 1;

    public static List<String> getSpelling(Mention m){
        List<String> ret = new ArrayList<>();
        String feat = "full-string:" + StringUtils.join("_", m.words).replaceAll("_$", "");
        ret.add(feat);

        for(String w : m.words){
            ret.add("contains:"+w);
        }

        return ret;
    }

    public static void normalize(Mention m){
        for(int i = 0; i < m.leftcontext.length; i++){
            m.leftcontext[i] = m.leftcontext[i].replaceAll("[\\p{P}0-9]", "d");
        }

        for(int i = 0; i < m.rightcontext.length; i++){
            m.rightcontext[i] = m.rightcontext[i].replaceAll("[\\p{P}0-9]", "d");
        }

    }

    public static List<String> getContext(Mention m){

        normalize(m);

        List<String> ret = new ArrayList<>();
        String v;
        if(m.leftcontext.length == 0){
            v = "_";
        }else{
            v = m.leftcontext[0].toLowerCase();
        }
        String feat = "leftcontext:" + v;
        ret.add(feat);

        if(m.rightcontext.length == 0){
            v = "_";
        }else{
            v = m.rightcontext[0].toLowerCase();
        }
        feat = "rightcontext:" + v;
        ret.add(feat);

        return ret;
    }

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

        options.addOption(help);
        options.addOption(trainpath);
        options.addOption(inputpath);
        options.addOption(outputpath);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // String modelpath = "tmp"; //ParametersForLbjCode.currentParameters.pathToModelFile;
        // if(modelpath.startsWith("tmp") || modelpath.length() == 0){
        //     Random r = new Random();
        //     modelpath = "/tmp/nermodel" + r.nextInt();
        // }

        String train = cmd.getOptionValue("train");
        String inpath = cmd.getOptionValue("input");
        String outpath = cmd.getOptionValue("output");


        // these should have the same
        List<Mention> seed = ReadMentions(train);
        List<Mention> mnts = ReadMentions(inpath);


        List<String> labels = new ArrayList<>();
        labels.add("PER");
        labels.add("ORG");
        labels.add("MISC");
        labels.add("LOC");

        double alpha = 0.1;
        int k = labels.size();
        double pmin = 0.95;


        // initialize rules
        HashMap<Pair<String, String>, Double> seedrules = new HashMap<>();
        for (Mention m : seed) {
            List<String> feats = getSpelling(m);

            for (String feat : feats) {
                Pair<String, String> p = new Pair<>(feat, m.neLabel);
                seedrules.put(p, 0.9999);
            }
        }

        int n = 5;
        int limit = 2500;

        // initialization
        HashMap<Pair<String, String>, Double> spelling = new HashMap<>(seedrules);
        HashMap<Pair<String, String>, Double> context = new HashMap<>();

        while(n < limit){
            HashMap<String, Integer> labeldist = new HashMap<>();
            System.out.println(String.format("On n=%d / %d", n, limit));

            // STEP 3: this annotates data with the spelling rules.
            for (Mention mnt : mnts) {
                double mx = -1;
                for (String feat : getSpelling(mnt)) {
                    // loop over all possible labels.
                    for (String label : labels) {
                        Pair<String, String> p = new Pair<>(feat, label);
                        if (spelling.containsKey(p)) {
                            double score = spelling.get(p);
                            if (score > mx) {
                                mnt.neLabel = label;
                                mx = score;
                            }
                        }
                    }
                }

                int cc = labeldist.getOrDefault(mnt.neLabel, 0);
                labeldist.put(mnt.neLabel, cc+1);
            }

            System.out.println(labeldist);
            System.out.println(spelling.size());

            // STEP 4: this gets context counts.
            // ultimately want a datastructure, looking like:
            // feature, label = probability
            // feature, label = count(feature, label) / count(feature)
            HashMap<Pair<String, String>, Double> counts = new HashMap<>();
            HashMap<String, Integer> featcounts = new HashMap<>();

            for (Mention mnt : mnts) {
                // labeled examples only thank you
                if (mnt.neLabel.equals("MNT")) continue;
                // get all feats
                List<String> feats = getContext(mnt);
                for (String feat : feats) {
                    // check if rule applies.
                    int cnt = featcounts.getOrDefault(feat, 0);
                    featcounts.put(feat, cnt + 1);

                    if(feat.contains("visit")){
//                        System.out.println("we have min");
                    }

                    Pair<String, String> featlab = new Pair<>(feat, mnt.neLabel);
                    double cnt2 = counts.getOrDefault(featlab, 0.);
                    counts.put(featlab, cnt2 + 1.);
                }
            }

            TreeMap<Integer, List<Pair<String, String>>> count2rule = new TreeMap<>();
            for (Pair<String, String> featlab : counts.keySet()) {
                String feat = featlab.getFirst();
                int countx = featcounts.get(feat);
                double countxy = counts.get(featlab);

                double h = (countxy + alpha) / (countx + k * alpha);

                if(featlab.getFirst().contains("visit") && featlab.getSecond().equals("LOC")){
                    //
                    // System.out.println("Context:minister");
                    //System.out.println(countxy + ", " + countx + ", " + h);
                }

                counts.put(featlab, h);
                if (h > pmin) {
                    List<Pair<String, String>> cc = count2rule.getOrDefault(countx, new ArrayList<>());
                    cc.add(featlab);
                    count2rule.put(countx, cc);
                }
            }

            // now we add n*k rules.
            // n will be updated later.
            context = new HashMap<>();
            for (int countx : count2rule.descendingKeySet()) {
                List<Pair<String, String>> toprules = count2rule.get(countx);
                for (Pair<String, String> p : toprules) {

                    // the rule and the score!
                    context.put(p, counts.get(p));

                    if (context.size() >= n*k) {
                        break;
                    }
                }

                if (context.size() >= n*k) {
                    break;
                }
            }

            System.out.println(context.size());

            // STEP 5
            for (Mention mnt : mnts) {
                double mx = -1;
                for (String feat : getContext(mnt)) {
                    // loop over all possible labels.
                    if(feat.contains("visit")){
                        //System.out.println("check here.");
                    }
                    for (String label : labels) {
                        Pair<String, String> p = new Pair<>(feat, label);
                        if (context.containsKey(p)) {
                            double score = context.get(p);
                            if (score > mx) {
                                mnt.neLabel = label;
                                mx = score;
                            }
                        }
                    }
                }
//                if (mx > -1) {
//                    System.out.println("Labeled2: " + mnt);
//                }
            }

            // STEP 6
            // ultimately want a datastructure, looking like:
            // feature, label = probability
            // feature, label = count(feature, label) / count(feature)
            counts = new HashMap<>();
            featcounts = new HashMap<>();

            for (Mention mnt : mnts) {
                // labeled examples only thank you
                if (mnt.neLabel.equals("MNT")) continue;
                // get all feats
                List<String> feats = getSpelling(mnt);
                for (String feat : feats) {
                    // check if rule applies.
                    int cnt = featcounts.getOrDefault(feat, 0);
                    featcounts.put(feat, cnt + 1);

                    Pair<String, String> featlab = new Pair<>(feat, mnt.neLabel);
                    double cnt2 = counts.getOrDefault(featlab, 0.);
                    counts.put(featlab, cnt2 + 1.);
                }
            }

            count2rule = new TreeMap<>();
            for (Pair<String, String> featlab : counts.keySet()) {
                // we only want new rules.
                if(spelling.containsKey(featlab)) continue;


                String feat = featlab.getFirst();
                int countx = featcounts.get(feat);
                double countxy = counts.get(featlab);

                double h = (countxy + alpha) / (countx + k * alpha);
                counts.put(featlab, h);
                if (h > pmin) {
                    List<Pair<String, String>> cc = count2rule.getOrDefault(countx, new ArrayList<>());
                    cc.add(featlab);
                    count2rule.put(countx, cc);
                }
            }

            // now we add n rules.
            // n will be updated later.
            HashMap<Pair<String, String>, Double> newspell = new HashMap<>();
            for (int countx : count2rule.descendingKeySet()) {
                List<Pair<String, String>> toprules = count2rule.get(countx);
                for (Pair<String, String> p : toprules) {

                    // the rule and the score!
                    newspell.put(p, counts.get(p));

                    if (newspell.size() >= n * k) {
                        break;
                    }
                }

                if (newspell.size() >= n * k) {
                    break;
                }
            }

            // update spelling.
            for(Pair<String, String> p : newspell.keySet()){
                spelling.put(p, newspell.get(p));
            }

            // STEP 7
            n += 5;
        }

        // all done!
        // combine rules
        HashMap<Pair<String, String>, Double> allrules = new HashMap(spelling);
        for(Pair<String, String> p : context.keySet()){
            allrules.put(p, context.get(p));
        }

        // Let's annotate now.
        CoNLLNerReader anno = new CoNLLNerReader(inpath);

        List<TextAnnotation> tas = new ArrayList<>();

        int missed = 0;
        while(anno.hasNext()){
            TextAnnotation ta = anno.next();
            View ner = ta.getView(ViewNames.NER_CONLL);
            List<Constituent> toAdd = new ArrayList<>();
            for(Constituent c : ner.getConstituents()){
                String[] toks = c.getTokenizedSurfaceForm().split(" ");

                int start = c.getStartSpan();
                int end = c.getEndSpan();

                String[] left = ta.getTokensInSpan(Math.max(0, start-ctx), start);
                String[] right = ta.getTokensInSpan(end, Math.min(ta.size(), end + ctx));

                Mention mnt = new Mention(toks, left, right, c.getLabel());

                List<String> feats = new ArrayList<>(getSpelling(mnt));
                feats.addAll(getContext(mnt));

                double mx = -1;
                String predLabel = null;
                for (String feat : feats) {
                    // loop over all possible labels.
                    for (String label : labels) {
                        Pair<String, String> p = new Pair<>(feat, label);
                        if (allrules.containsKey(p)) {
                            double score = allrules.get(p);
                            if (score > mx) {
                                predLabel = label;
                                mx = score;
                            }
                        }
                    }
                }

                if(predLabel != null) {
                    // this is what we have to do to change the label.
                    Constituent c2 = new Constituent(predLabel, ViewNames.NER_CONLL, ta, start, end);
                    toAdd.add(c2);
                    ner.removeConstituent(c);
                }else{
                    //System.out.println(c);
                    missed += 1;
                }

            }
            //ner.removeAllConsituents();
            for(Constituent c2 : toAdd){
                ner.addConstituent(c2);
            }
            tas.add(ta);
        }

        System.out.println("Missed: " + missed);
        CoNLLNerReader.TaToConll(tas, outpath);

    }

    public static List<Mention> ReadMentions(String trainpath) throws Exception {


        CoNLLNerReader reader = new CoNLLNerReader(trainpath);

        List<Mention> mnts = new ArrayList<>();
        while(reader.hasNext()){
            TextAnnotation ta = reader.next();
            View ner = ta.getView(ViewNames.NER_CONLL);
            for(Constituent c : ner.getConstituents()){
                String[] toks = c.getTokenizedSurfaceForm().split(" ");

                int start = c.getStartSpan();
                int end = c.getEndSpan();

                String[] left = ta.getTokensInSpan(Math.max(0, start-ctx), start);
                String[] right = ta.getTokensInSpan(end, Math.min(ta.size(), end + ctx));

                Mention mnt = new Mention(toks, left, right, c.getLabel());
                mnts.add(mnt);
            }
        }

        Collections.shuffle(mnts);

        return mnts;

    }
}
