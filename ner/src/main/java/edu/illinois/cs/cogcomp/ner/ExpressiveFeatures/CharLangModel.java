package edu.illinois.cs.cogcomp.ner.ExpressiveFeatures;

import edu.illinois.cs.cogcomp.core.resources.ResourceConfigurator;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.IO.InFile;
import edu.illinois.cs.cogcomp.ner.LbjTagger.Data;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.ner.LbjTagger.ParametersForLbjCode;
import gnu.trove.map.hash.THashMap;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import net.sf.ehcache.search.expression.Not;
import org.cogcomp.Datastore;
import org.cogcomp.DatastoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Created by Xiaodong Yu on 3/25/18.
 */
public class CharLangModel {
    private static Logger logger = LoggerFactory.getLogger(CharLangModel.class);

    /** the sole instance of this class. */
    private static CharLangModel charlm = null;

    /** used to synchronize initialization. */
    static private final String INIT_SYNC = "Character Language Model Initialization Synchronization Token";

    private static THashMap<String, Double> Entity_U = new THashMap<>();
    private static THashMap<String, Double> NotEntity_U = new THashMap<>();
    private static THashMap<String, Double> Entity_L = new THashMap<>();
    private static THashMap<String, Double> NotEntity_L = new THashMap<>();
    private static THashMap<String, Double> PER = new THashMap<>();
    private static THashMap<String, Double> ORG = new THashMap<>();
    private static THashMap<String, Double> LOC = new THashMap<>();
    private static THashMap<String, Double> MISC = new THashMap<>();
    private static THashMap<String, Double> Arabic = new THashMap<>();
    private static THashMap<String, Double> Russian = new THashMap<>();
    private static THashMap<String, Double> Chinese = new THashMap<>();
    private static THashMap<String, Double> German = new THashMap<>();
    private static THashMap<String, Double> ArabicLOC = new THashMap<>();
    private static THashMap<String, Double> RussianLOC = new THashMap<>();
    private static THashMap<String, Double> GermanLOC = new THashMap<>();
    private static THashMap<String, Double> ChineseLOC = new THashMap<>();


    /**
     * This method should never be called before init, or the gazetteer will not be initialized.
     *
     * @return the singleton instance of the Gazetteers class.
     */
    static public CharLangModel get() {
        synchronized (INIT_SYNC) {
            return charlm;
        }
    }

    static public void set(CharLangModel clm){
        charlm = clm;
    }

    /** ensures singleton-ness. */
    private CharLangModel() {

    }

    private ArrayList<String> resources = null;
    private ArrayList<THashMap<String, String>> wordToPathByResource = null;

    public static void init(String path) {
        try {
            synchronized (INIT_SYNC) {
                charlm = new CharLangModel();
                charlm.resources = new ArrayList<>();

                InputStream is = new FileInputStream(path+"/ngram_Entity_L_processed.out");
                InFile in = new InFile(is);
                String line = in.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    Entity_L.put(splited[0], Double.parseDouble(splited[1]));
                    line = in.readLine();
                }

                InputStream is2 = new FileInputStream(path+"/ngram_NotEntity_L_processed.out");
                InFile in2 = new InFile(is2);
                line = in2.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    NotEntity_L.put(splited[0], Double.parseDouble(splited[1]));
                    line = in2.readLine();
                }

//                InputStream is3 = new FileInputStream(path+"/ngram_PER_processed.out");
//                InFile in3 = new InFile(is3);
//                line = in3.readLine();
//                while (line != null) {
//                    String[] splited = line.replace("\n","").split("\\t");
////                    System.out.println(splited[0]);
////                    System.out.println(Double.parseDouble(splited[1]));
//                    PER.put(splited[0], Double.parseDouble(splited[1]));
//                    line = in3.readLine();
//                }
//
//                InputStream is4 = new FileInputStream(path+"/ngram_ORG_processed.out");
//                InFile in4 = new InFile(is4);
//                line = in4.readLine();
//                while (line != null) {
//                    String[] splited = line.replace("\n","").split("\\t");
////                    System.out.println(splited[0]);
////                    System.out.println(Double.parseDouble(splited[1]));
//                    ORG.put(splited[0], Double.parseDouble(splited[1]));
//                    line = in4.readLine();
//                }
//
//                InputStream is5 = new FileInputStream(path+"/ngram_LOC_processed.out");
//                InFile in5 = new InFile(is5);
//                line = in5.readLine();
//                while (line != null) {
//                    String[] splited = line.replace("\n","").split("\\t");
////                    System.out.println(splited[0]);
////                    System.out.println(Double.parseDouble(splited[1]));
//                    LOC.put(splited[0], Double.parseDouble(splited[1]));
//                    line = in5.readLine();
//                }

                in.close();
                in2.close();
//                in3.close();
//                in4.close();
//                in5.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void init() {

        try {
            synchronized (INIT_SYNC) {
                charlm = new CharLangModel();
                charlm.resources = new ArrayList<>();

                InputStream is = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_Entity_L.out");
                InFile in = new InFile(is);
                String line = in.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    Entity_L.put(splited[0], Double.parseDouble(splited[1]));
                    line = in.readLine();
                }

                InputStream is2 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_NotEntity_L.out");
                InFile in2 = new InFile(is2);
                line = in2.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    NotEntity_L.put(splited[0], Double.parseDouble(splited[1]));
                    line = in2.readLine();
                }

                is = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_Entity_U.out");
                in = new InFile(is);
                line = in.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    Entity_U.put(splited[0], Double.parseDouble(splited[1]));
                    line = in.readLine();
                }

                is2 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_NotEntity_U.out");
                in2 = new InFile(is2);
                line = in2.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    NotEntity_U.put(splited[0], Double.parseDouble(splited[1]));
                    line = in2.readLine();
                }


                InputStream is3 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_PER.out");
                InFile in3 = new InFile(is3);
                line = in3.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    PER.put(splited[0], Double.parseDouble(splited[1]));
                    line = in3.readLine();
                }

                InputStream is4 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_ORG.out");
                InFile in4 = new InFile(is4);
                line = in4.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    ORG.put(splited[0], Double.parseDouble(splited[1]));
                    line = in4.readLine();
                }

                InputStream is5 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_LOC.out");
                InFile in5 = new InFile(is5);
                line = in5.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    LOC.put(splited[0], Double.parseDouble(splited[1]));
                    line = in5.readLine();
                }

                InputStream is6 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_MISC.out");
                InFile in6 = new InFile(is6);
                line = in6.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    MISC.put(splited[0], Double.parseDouble(splited[1]));
                    line = in6.readLine();
                }

                InputStream is7 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_Arabic.out");
                InFile in7 = new InFile(is7);
                line = in7.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    Arabic.put(splited[0], Double.parseDouble(splited[1]));
                    line = in7.readLine();
                }

                InputStream is8 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_Russian.out");
                InFile in8 = new InFile(is8);
                line = in8.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    Russian.put(splited[0], Double.parseDouble(splited[1]));
                    line = in8.readLine();
                }

                InputStream is9 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_Chinese.out");
                InFile in9 = new InFile(is9);
                line = in9.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    Chinese.put(splited[0], Double.parseDouble(splited[1]));
                    line = in9.readLine();
                }

                InputStream is10 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_German.out");
                InFile in10 = new InFile(is10);
                line = in10.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    German.put(splited[0], Double.parseDouble(splited[1]));
                    line = in10.readLine();
                }

                InputStream is11 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_Arabic_LOC.out");
                InFile in11 = new InFile(is11);
                line = in11.readLine();
                while (line != null) {
                     String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    ArabicLOC.put(splited[0], Double.parseDouble(splited[1]));
                    line = in11.readLine();
                }

                InputStream is12 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_Russian_LOC.out");
                InFile in12 = new InFile(is12);
                line = in12.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    RussianLOC.put(splited[0], Double.parseDouble(splited[1]));
                    line = in12.readLine();
                }

                InputStream is13 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_Chinese_LOC.out");
                InFile in13 = new InFile(is13);
                line = in13.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    ChineseLOC.put(splited[0], Double.parseDouble(splited[1]));
                    line = in13.readLine();
                }

                InputStream is14 = new FileInputStream("/Users/wu/Documents/UIUC/Dan_research/illinois-cogcomp-nlp/ner/conll_char_result_German_LOC.out");
                InFile in14 = new InFile(is14);
                line = in14.readLine();
                while (line != null) {
                    String[] splited = line.replace("\n","").split("\\t");
//                    System.out.println(splited[0]);
//                    System.out.println(Double.parseDouble(splited[1]));
                    GermanLOC.put(splited[0], Double.parseDouble(splited[1]));
                    line = in14.readLine();
                }



                in.close();
                in2.close();
                in3.close();
                in4.close();
                in5.close();
                in6.close();
                in7.close();
                in8.close();
                in9.close();
                in10.close();
                in11.close();
                in12.close();
                in13.close();
                in14.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the resource names array.
     */
    final public ArrayList<String> getResources() {
        return resources;
    }

    final public Double getEntity(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = Entity_L.get(w.form);
        if (ppl != null)
            return Entity_L.get(w.form);
        else
            return 100.0;
    }

    final public Double getNotEntity(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(NotEntity.get(w.form));
        Double ppl = NotEntity_L.get(w.form);
        if (ppl != null)
            return NotEntity_L.get(w.form);
        else
            return 1.0;
    }

    final public Double getEntity_L(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = Entity_L.get(w.form);
        if (ppl != null)
            return Entity_L.get(w.form);
        else
            return 100.0;
    }

    final public Double getNotEntity_L(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(NotEntity.get(w.form));
        Double ppl = NotEntity_L.get(w.form);
        if (ppl != null)
            return NotEntity_L.get(w.form);
        else
            return 1.0;
    }

    final public Double getEntity_U(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = Entity_U.get(w.form);
        if (ppl != null)
            return Entity_U.get(w.form);
        else
            //System.out.println(w.form);
            return 100.0;
    }

    final public Double getNotEntity_U(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(NotEntity.get(w.form));
        Double ppl = NotEntity_U.get(w.form);
        if (ppl != null)
            return NotEntity_U.get(w.form);
        else
            return 1.0;
    }


    final public Double getPER(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = PER.get(w.form);
        if (ppl != null)
            return ppl;
        else
            return 100.0;
    }

    final public Double getORG(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = ORG.get(w.form);
        if (ppl != null)
            return ppl;
        else
            return 100.0;
    }

    final public Double getLOC(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = LOC.get(w.form);
        if (ppl != null)
            return ppl;
        else
            return 100.0;
    }

    final public Double getMISC(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        return MISC.get(w.form);
    }

    final public Double getArabic(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = Arabic.get(w.form);
        if (ppl != null)
            return ppl;
        else
            return 100.0;
    }

    final public Double getRussian(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = Russian.get(w.form);
        if (ppl != null)
            return ppl;
        else
            return 100.0;
    }

    final public Double getChinese(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = Chinese.get(w.form);
        if (ppl != null)
            return ppl;
        else
            return 100.0;
    }

    final public Double getGerman(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        Double ppl = German.get(w.form);
        if (ppl != null)
            return ppl;
        else
            return 100.0;    }

    final public Double getArabicLOC(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        return ArabicLOC.get(w.form);
    }

    final public Double getRussianLOC(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        return RussianLOC.get(w.form);
    }

    final public Double getChineseLOC(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        return ChineseLOC.get(w.form);
    }

    final public Double getGermanLOC(NEWord w) {
//        System.out.println(w.form);
//        System.out.println(Entity.get(w.form));
        return GermanLOC.get(w.form);
    }

    public static void main(String[] args) {
        CharLangModel.init();
    }
}
