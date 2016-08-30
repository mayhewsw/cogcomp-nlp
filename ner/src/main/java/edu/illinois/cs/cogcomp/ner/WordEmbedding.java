package edu.illinois.cs.cogcomp.ner;

import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;


/**
 * Created by ctsai12 on 10/10/15.
 */
public class WordEmbedding {


    public static ConcurrentNavigableMap<String, Double[]> mono_vec;
    public static ConcurrentNavigableMap<String, Double[]> multi_vec_en;
    public static ConcurrentNavigableMap<String, Double[]> multi_vec_lang;

    public static Map<String, ConcurrentNavigableMap<String, Double[]>> multi_vecs = new HashMap<>();


    public static DB mono_db;
    private static DB multi_db;
    public static int dim;
    private static Map<String, Double[]> vec_cache = new HashMap<>();
    private static boolean use_mcache = false;
    public String lang;

    public static String dbpath = "/shared/preprocessed/ctsai12/multilingual/mapdb";

    public WordEmbedding() {

    }

    public void closeDB() {
        if (multi_db != null && !multi_db.isClosed()) {
            multi_db.commit();
            multi_db.close();
        }
        if (mono_db != null && !mono_db.isClosed()) {
            mono_db.commit();
            mono_db.close();
        }
    }


    public static void loadMonoDBNew(String lang) {
        System.out.println("Loading mono vectors "+lang);
        if(mono_db != null && !mono_db.isClosed())
            mono_db.close();
        mono_db = DBMaker.newFileDB(new File(dbpath+"/mono-embeddings", lang))
                .cacheSize(100000)
                .transactionDisable()
                .closeOnJvmShutdown()
                .make();
        mono_vec = mono_db.createTreeMap(lang)
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
    }


    public static void setMonoVecsNew(String lang) {
        if(!lang.equals("en")) {
            loadMonoDBNew(lang);
            multi_vecs.put(lang, mono_vec);
        }
        loadMonoDBNew("en");
        multi_vecs.put("en", mono_vec);
        dim = multi_vecs.get("en").get("obama").length;
    }

    public static void loadMultiDBNew(String lang) {
        System.out.println("Loading "+lang+" multi vectors...");
        File f = new File(dbpath, "multi-embeddings/"+lang+"/"+lang);
        multi_db = DBMaker.newFileDB(f)
                .cacheSize(100000)
                .transactionDisable()
                .closeOnJvmShutdown()
                .make();
        multi_vec_en = multi_db.createTreeMap("en")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        multi_vec_lang = multi_db.createTreeMap("lang")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        multi_vecs.put("en", multi_vec_en);
        multi_vecs.put(lang, multi_vec_lang);
        if(multi_vec_en.containsKey("obama"))
            dim = multi_vec_en.get("obama").length;
        System.out.println("done");
    }



    public static Double[] getVectorFromWords(List<String> words, String lang){
        List<Double[]> vecs = new ArrayList<>();
        for(String word: words){
            Double[] vec = getWordVector(word, lang);
            if(vec != null)
                vecs.add(vec);
        }
        return averageVectors(vecs);
    }

    public Double[] getVectorFromWords(String[] words, String lang){
        return getVectorFromWords(Arrays.asList(words), lang);
    }

    public Double[] zeroVec(){
        Double[] ret = new Double[dim];
        Arrays.fill(ret, 0.0);
        return ret;
    }

    public static Double[] averageVectors(List<Double[]> vecs){
        Double[] ret = new Double[dim];
        Arrays.fill(ret, 0.0);
        for(Double[] v: vecs){
            for(int i = 0; i < v.length; i++)
                ret[i] += v[i];
        }
        if(vecs.size()>0)
            for(int i = 0; i < dim; i++)
                ret[i] /= vecs.size();
        return ret;
    }

    public Double[] averageVectors(List<Double[]> vecs, List<Double> weights){
        Double[] ret = new Double[dim];
        Arrays.fill(ret, 0.0);

        double sum = 0;
        for(int i = 0; i < vecs.size(); i++){
            double w = weights.get(i);
            sum += w;
            for(int j = 0; j < vecs.get(i).length; j++)
                ret[j] += w*vecs.get(i)[j];
        }

        if(vecs.size()>0 && sum > 0)
            for(int i = 0; i < dim; i++)
                ret[i] /= sum;
        return ret;
    }


    public static Double[] getVector(String query, String lang){
        if(use_mcache && vec_cache.containsKey(query))
            return vec_cache.get(query);
        if((!multi_vecs.get(lang).containsKey(query))) {
            if(use_mcache) vec_cache.put(query, null);
            return null;
        }

        Double[] vec = multi_vecs.get(lang).get(query);
        if(use_mcache) vec_cache.put(query, vec);
        return vec;
    }

    public static Double[] getWordVector(String word, String lang){
        if(!multi_vecs.containsKey(lang)){
            System.err.println("Couldn't find word embeddings for "+lang);
            System.exit(-1);
        }
        word = word.toLowerCase();
        Double[] vec = getVector(word, lang);
        return vec;
    }

    public Double[] getTitleVector(String title, String lang){
        if(title == null || title.startsWith("NIL")) {
            return null;
        }

        title = "title_"+title.replaceAll(" ", "_").toLowerCase();
        Double[] vec = getVector(title, lang);
//        if(vec == null) return zeroVec();
        return vec;
    }

    public double cosine(Double[] v1, Double[] v2) {
        if (v1.length != v2.length) {
            System.out.println("Array size don't match");
            System.exit(-1);
        }

        double n1 = 0, n2 = 0, in = 0;
        for (int i = 0; i < v1.length; i++) {
            in += v1[i] * v2[i];
            n1 += v1[i] * v1[i];
            n2 += v2[i] * v2[i];
        }

        if (n1 == 0 || n2 == 0)
            return 0;

        return in / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    public static void main(String[] args) {

        WordEmbedding.loadMultiDBNew("bn");

    }
}
