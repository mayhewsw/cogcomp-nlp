package edu.illinois.cs.cogcomp.ner.LbjTagger;

import java.util.ArrayList;
import java.util.Arrays;

public class Mention {
    public ArrayList<String> prefixes;
    public ArrayList<String> suffixes;
    public String neLabel;
    public String[] words;
    public String[] leftcontext;
    public String[] rightcontext;


    public Mention(String[] words, String[] left, String[] right, String neLabel){
        this.words = words;
        leftcontext = left;
        rightcontext = right;
        this.neLabel = neLabel;

        this.prefixes = new ArrayList<>();

        String first = this.words[0];
        int N = first.length();
        for (int i = 3; i <= 4; ++i) {
            if (N > i)
                prefixes.add(first.substring(0, i));
        }

        this.suffixes = new ArrayList<>();

        String last = this.words[this.words.length-1];
        N = last.length();
        for (int i = 2; i <= 4; ++i) {
            if (N > i)
                suffixes.add(last.substring(N - i));
        }
    }

    @Override
    public String toString() {
        return "Mention{" +
                "neLabel='" + neLabel + '\'' +
                ", words=" + Arrays.toString(words) +
                ", leftcontext=" + Arrays.toString(leftcontext) +
                ", rightcontext=" + Arrays.toString(rightcontext) +
                '}';
    }
}
