package edu.illinois.cs.cogcomp.ner;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.lbjava.learn.Lexicon;
import edu.illinois.cs.cogcomp.lbjava.learn.SparseNetworkLearner;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import org.apache.commons.lang3.StringUtils;

import javax.xml.soap.Text;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by stephen on 7/19/16.
 */
public class Sandbox {


    /**
     * Lang must be the 3-letter code used by the lexicons.
     * @param lang
     * @return
     * @throws IOException
     */
    public static HashMap<String, List<String>> translate(String lang) throws IOException {
        String path = "/shared/experiments/mayhew2/lexicons/";
        String fullpath = path + lang + "-eng.masterlex.txt.gz";

        List<String> lines = LineIO.readGZip(fullpath);

        HashMap<String, List<String>> e2f = new HashMap<>();
        HashMap<String, List<String>> f2e = new HashMap<>();

        for(String line : lines){
            String[] sline = line.split("\\t");
            String forn = sline[0];
            String eng = sline[5];

            if(forn.length() == 1 || eng.length() == 1) continue;

            if(!e2f.containsKey(eng)){
                e2f.put(eng, new ArrayList<String>());
            }

            if(!f2e.containsKey(forn)){
                f2e.put(forn, new ArrayList<String>());
            }

            e2f.get(eng).add(forn);
            f2e.get(forn).add(eng);

        }
        return e2f;
    }


    public static void getFeatureWeights(SparseNetworkLearner c) throws Exception {
        ByteArrayOutputStream sout = new ByteArrayOutputStream();
        PrintStream out;
        out = new PrintStream(sout);
        c.write(out);
        String s = sout.toString();

        List<String> labels = new ArrayList<>();
        List<HashMap<String, Double>> featlist = new ArrayList<>();

        String[] lines = s.split("\n");
        Lexicon lexicon = c.getLexicon();

        List<String> lexlines = new ArrayList<>();
        for(int l = 0; l < lexicon.size(); l++){
            lexlines.add(lexicon.lookupKey(l).toStringNoPackage() + "");
        }
        LineIO.write("lexicon.txt", lexlines);

        // this is the label line.
        int i = 4;

        while(true) {
            if(lines[i].startsWith("End")){
                // then it's really the end;
                break;
            }

            String label = lines[i].split(" ")[1];
            labels.add(label);
            System.out.println("Reading " + label);
            i += 3;

            HashMap<String, Double> feats = new HashMap<String, Double>();
            String line;
            int startval = i;
            while (!(line = lines[i]).startsWith("End")) {
                //System.out.println(line);
                String featid = lexicon.lookupKey(i - startval).toStringNoPackage(); // .getStringIdentifier();
                feats.put(featid, Double.parseDouble(line));
                i++;
            }
            featlist.add(feats);

            // will get you to next label, or last line.
            i++;
        }

        System.out.println("DONE");
        List<String> outlines = new ArrayList<>();
        double thresh = 1;
        for(int j = 0; j < labels.size(); j++){
            String label = labels.get(j);
            System.out.println(label);
            outlines.add(label);
            HashMap<String, Double> feats = featlist.get(j);

            List<Pair<String, Double>> pairs = new ArrayList<>();
            for(String key : feats.keySet()){
                if(Math.abs(feats.get(key)) > thresh){
                    pairs.add(new Pair<>(key, feats.get(key)));
                }
            }

            pairs.sort(Comparator.comparing(Pair::getSecond));


            for(Pair<String, Double> p : pairs) {
                System.out.println(p.getFirst() + " : " + p.getSecond());
                outlines.add(label + "\t" + p.getFirst() + " : " + p.getSecond());
            }

            outlines.add("\n");
            System.out.println();
        }

        LineIO.write("feats.txt", outlines);


    }


    public static void hashdata() throws IOException {
        List<String> lines = LineIO.read("/shared/corpora/ner/conll2003/eng/Test/eng.testb.conll");
        // convert every word to it's hash.

        List<String> outlines = new ArrayList<>();

        for(String line : lines){
            String[] sline = line.split("\\t");
            if(sline.length > 5) {
                sline[5] = "hash" + sline[5].hashCode() + "";
            }
            outlines.add(StringUtils.join(sline, "\t"));
        }

        LineIO.write("HashTest/test.conll", outlines);

    }

    /**
     * Gets the shortest phrase from a list of phrases
     * @param words
     * @return
     */
    public static String getshortest(List<String> words){
        String best = words.get(0);
        int shortest = best.split(" ").length;

        for(String w : words){
            int len = w.split(" ").length;
            if(len < shortest){
                best = w;
                shortest = len;
            }
        }
        return best;
    }


//    public static void fixlexicon(SparseNetworkLearner c) throws IOException {
//        Lexicon lexicon = c.getLexicon();
//
//        HashMap<String, List<String>> map = translate("spa");
//
//        HashMap<String, Integer> options = new HashMap<>();
//        HashSet<String> missed = new HashSet<>();
//
//        for(int i = 0; i < lexicon.size(); i++) {
//            Feature f =  lexicon.lookupKey(i);
//
//            if(f.getClass().equals(DiscretePrimitiveStringFeature.class)){
//                DiscretePrimitiveStringFeature df = (DiscretePrimitiveStringFeature) f;
//
//                // This is where the dictionary substitute happens.
//                String val = f.getStringValue();
//                if(map.containsKey(val)) {
//                    val = getshortest(map.get(val));
//
//                    //map.get(val)
//
//                }else if(map.containsKey(val.toLowerCase())){
//                    val = getshortest(map.get(val.toLowerCase()));
//                }else{
//                    missed.add(val);
//                }
//
//                df.setStringValue(val);
//
//                lexicon.put(df, i);
//            }
//        }
//
//        System.out.println("Total: " +lexicon.size());
//        System.out.println("This many missed: " + missed.size());
//
//        System.out.println(missed);
//
//        System.out.println("Writing to models/hash/mine.model.level1{.lex}");
//        c.write("models/hash/mine.model.level1", "models/hash/mine.model.level1.lex");
//    }

    public static void main(String[] args) throws Exception {
//        Parameters.readConfigAndLoadExternalData("config/tacl/ti.config", false);
//        ParametersForLbjCode cp = ParametersForLbjCode.currentParameters;
//        String model = cp.pathToModelFile;
//        //String model = "models/trans-ug/mono";
//

//        List<String> lines = LineIO.read("ner/data.test");
//        for(String line: lines){
//            String[] terms = line.split("\\s+");
//
//            String classStr = terms[0];
//            // In SVMLight +1 and 1 are the same label.
//            // Adding a special case to normalize...
//            if (classStr.equals("+1")) {
//                classStr = "1";
//            }
//
//
//            // the rest are feature-value pairs
//            ArrayList<Integer> indices = new ArrayList<Integer>();
//            ArrayList<Double> values = new ArrayList<Double>();
//            for (int termIndex = 1; termIndex < terms.length; termIndex++) {
//                if (!terms[termIndex].equals("")) {
//                    String[] s = terms[termIndex].split(":");
//                    if (s.length != 2) {
//                        System.out.println(line);
//
//                        throw new RuntimeException("invalid format: " + terms[termIndex] + " (should be feature:value)");
//                    }
//                    String feature = s[0];
//                }
//            }
//        }


//        dd();
//        System.exit(-1);
//
//        String model = "/tmp/nermodel-127794978";

//        OnonOtagger tagger1 = new OnonOtagger(model + ".level1", model + ".level1.lex");
//        NETaggerLevel2 tagger2 = new NETaggerLevel2(cp.pathToModelFile + ".level2", cp.pathToModelFile + ".level2.lex");
//
//        getFeatureWeights(tagger1);
//        System.exit(0);


//        TextAnnotation ta;
//
//        String indir = "/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/conll-setE-ct-cp3-4/";
//        String outdir = "/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/conll-setE-ct-cp3-4-rules/";
//
//        String namesfile = "/shared/corpora/corporaWeb/lorelei/evaluation-20170804/il6/set1/data/monolingual_text/allnames.tab";
//
//        ArrayList<String> lines = LineIO.read(namesfile);
//
//        HashMap<String, Integer> counts = new HashMap<>();
//        for(String line : lines){
//            String[] sline = line.split("\t");
//            String mention = sline[2];
//            String type = sline[5];
//            String key = mention + "|||" + type;
//
//            int currcount = counts.getOrDefault(key, 0);
//            counts.put(key, currcount +1);
//        }
//
//        HashMap<String, String> mention2label = new HashMap<>();
//        for(String key : counts.keySet()){
//            String[] kk = key.split("\\|\\|\\|");
//            String mention = kk[0];
//            String label = kk[1];
//
//            if(counts.get(key) > 2){
//                mention2label.put(mention, label);
//            }
//        }
//
//        System.out.println(mention2label);
//
//        File f = new File(indir);
//
//        for(File ff : f.listFiles()){
//
//            CoNLLNerReader cnr = new CoNLLNerReader(ff.getAbsolutePath());
//            ta = cnr.next();
//
//            View ner = ta.getView(ViewNames.NER_CONLL);
//
//            for(String mention : mention2label.keySet()){
//                List<IntPair> spans = ta.getSpansMatching(mention);
//
//                String label = mention2label.get(mention);
//
//                for(IntPair span : spans){
//                    List<Constituent> cons = ner.getConstituentsCoveringSpan(span.getFirst(), span.getSecond());
//                    if(cons.size() == 0){
//                        Constituent newc = new Constituent(label, ViewNames.NER_CONLL, ta, span.getFirst(), span.getSecond());
//                        ner.addConstituent(newc);
//                        System.out.println("Adding cons! " + mention);
//                    }
//
//                }
//            }
//
//            CoNLLNerReader.TaToConll(Collections.singletonList(ta), outdir);
//        }

//        fixlexicon(tagger1);


//        hashdata();

        //translate();



    }


}
