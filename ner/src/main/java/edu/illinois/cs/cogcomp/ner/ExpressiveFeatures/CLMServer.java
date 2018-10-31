package edu.illinois.cs.cogcomp.ner.ExpressiveFeatures;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.illinois.cs.cogcomp.annotation.BasicTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.LbjTagger.*;
import edu.illinois.cs.cogcomp.nlp.tokenizer.StatefulTokenizer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.CharacterLanguageModel.string2list;

public class CLMServer {

    public static HashMap<String, HashSet<String>> seenWords = new HashMap<>();
    public static HashMap<String, CharacterLanguageModel> eclm = new HashMap<>();
    public static HashMap<String, CharacterLanguageModel> neclm = new HashMap<>();
    public static StatefulTokenizer st;

    public static void loadModel(String name, String path, ParametersForLbjCode params) throws Exception {
        System.out.println(name);
        st = new StatefulTokenizer();
        Data trainData = new Data(path, path, "-json", new String[] {}, new String[] {}, params);

        List<List<String>> entities = new ArrayList<>();
        List<List<String>> nonentities = new ArrayList<>();

        HashSet<String> words = new HashSet<>();

        for(NERDocument doc : trainData.documents){
            for(LinkedVector sentence : doc.sentences){
                for(int i = 0; i < sentence.size(); i++) {
                    NEWord word = (NEWord) sentence.get(i);
                    String lowerform = word.form.toLowerCase();
                    words.add(lowerform);
                    if(word.neLabel.equals("O")){
                        nonentities.add(string2list(lowerform));
                    }else {
                        entities.add(string2list(lowerform));
                    }
                }
            }
        }

        CharacterLanguageModel e = new CharacterLanguageModel();
        e.train(entities);

        CharacterLanguageModel ne = new CharacterLanguageModel();
        ne.train(nonentities);

        eclm.put(name, e);
        neclm.put(name, ne);
        seenWords.put(name, words);
    }

    public static void main(String[] args) throws Exception {

        ParametersForLbjCode params = Parameters.readConfigAndLoadExternalData("config/ner.properties", false);

        String trainpath= "/shared/corpora/ner/conll2003/eng-files/Train-json/";
        loadModel("eng", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/amh/All-json/";
        loadModel("amh", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/ara/All-json/";
        loadModel("ara", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/ben/All-json/";
        loadModel("ben", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/fas/All-json/";
        loadModel("fas", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/hin/All-json/";
        loadModel("hin", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/hun/All-json/";
        loadModel("hun", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/orm/All-json/";
        loadModel("orm", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/rus/All-json/";
        loadModel("rus", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/som/All-json/";
        loadModel("som", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/tir/All-json/";
        loadModel("tir", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/uig/All-json/";
        loadModel("uig", trainpath, params);

        trainpath= "/shared/corpora/ner/lorelei-swm-new/yor/All-json/";
        loadModel("yor", trainpath, params);


        int port = 4040;
        System.out.println("Server started on " + port);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {

            URI uri = t.getRequestURI();
            String[] getargs = uri.getQuery().split("&");

            String text = null;
            String views = null;
            for(String arg : getargs){
                //System.out.println(arg);
                String[] toks = arg.split("=");
                if(toks[0].equals("text")){
                    text = toks[1];
                }else if(toks[0].equals("views")){
                    views = toks[1];
                }
            }

            String encoding = "UTF-8";
            t.getResponseHeaders().set("Content-Type", "application/json; charset=" + encoding);
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST");

            if(text != null && views != null) {

                String[] viewsInArray = views.split(",");

                List<String[]> sents = new ArrayList<>();
                sents.add(st.tokenizeTextSpan(text).getTokens());
                TextAnnotation ta = BasicTextAnnotationBuilder.createTextAnnotationFromTokens(sents);
                for (String vuName : viewsInArray) {
                    System.out.println(vuName);
                    if(vuName.startsWith("CLM")){

                        String[] vv = vuName.split("_");
                        String lang = vv[1];

                        if(eclm.containsKey(lang)) {
                            SpanLabelView v = new SpanLabelView(vuName, "", ta, 1.);

                            HashSet<String> words = seenWords.get(lang);

                            for (int i = 0; i < ta.getTokens().length; i++) {
                                String tok = ta.getToken(i).toLowerCase();
                                if(tok.length() >= 3) {

                                    double eppl = eclm.get(lang).perplexity(CharacterLanguageModel.string2list(tok));
                                    double neppl = neclm.get(lang).perplexity(CharacterLanguageModel.string2list(tok));

                                    System.out.println(tok + ": " + eppl + ", " + neppl);

                                    if (eppl < neppl) {
                                        String label;
                                        if(words.contains(tok)){
                                            label = "ENTITY (S)";
                                        }else{
                                            label = "ENTITY (U)";
                                        }
                                        Constituent c = new Constituent(label, vuName, ta, i, i + 1);
                                        v.addConstituent(c);
                                    }
                                }
                            }
                            ta.addView(vuName, v);
                        }
                    }
                }
                String output = SerializationHelper.serializeToJson(ta);

                byte[] bytes = output.getBytes(StandardCharsets.UTF_8);
                t.sendResponseHeaders(200, bytes.length);
                OutputStream os = t.getResponseBody();
                os.write(bytes);
                os.close();
            }else{
                byte[] bytes = "ERROR".getBytes(StandardCharsets.UTF_8);
                t.sendResponseHeaders(500, bytes.length);
                OutputStream os = t.getResponseBody();
                os.write(bytes);
                os.close();
            }
        }
    }

}
