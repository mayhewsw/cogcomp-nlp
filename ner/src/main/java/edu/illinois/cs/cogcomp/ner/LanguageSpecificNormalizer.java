package edu.illinois.cs.cogcomp.ner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mayhew2 on 7/4/16.
 */
public class LanguageSpecificNormalizer {

    // this is created from a python script that simply counts common 3-letter suffixes.
    //static String[] tr_suffixes = {"ini","dan","ını","nin","den","nın","lar","ler","ine","nda","yor","ına","arı","eri","nde","aki","rin","yla","ası","eki"};
    static String[] tr_suffixes = {"üyor", "uyor", "iyor", "ıyor", "nden", "nın", "nün", "la", "ya", "yı", "ne", "de", "te", "den", "ten", "da", "ta", "dan", "tan", "a", "e", "ye", "i","ı","u","ü","yi","yı","yu","yü","ni","nı","nu","nü", "nda", "nde", "le", "la", "yle", "yla", "siz","sız","suz","süz", "ler"};


    public static String removesuffixes(String w){
        boolean goagain = false;
        for(String tr_s : tr_suffixes){
            if(w.endsWith(tr_s)){
                w = w.substring(0,w.length()-tr_s.length());
                goagain = true;
                break;
            }
        }
        if(goagain){
            return removesuffixes(w);
        }else {
            if(w.length() == 0){
                return "[EMPTY]";
            }else {
                return w;
            }
        }

    }


    public static List<String> turkish(String w){
        List<String> ret = new ArrayList<>();

        // split on punctuation, or a common character in Turkish.
        String[] res = w.split("[\\p{Punct}ʼ]+");
        if(res.length > 1) {
            ret.add(res[0]);
        }

        ret.add(removesuffixes(w));

        return ret;

    }

    public static void main(String[] args) {

        String name = "domateslerde";
        System.out.println(LanguageSpecificNormalizer.removesuffixes(name));

//        String text = "Emre Zeynepʼe selam veriyor olabilir .";
//        for(String s : text.split(" ")){
//            System.out.println(LanguageSpecificNormalizer.turkish(s));
//        }
    }


}
