package edu.illinois.cs.cogcomp.nlp.corpusreaders.loreleiReader;

import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * This is intended to get plain text from ltf files in order to get parallel alignments.
 * Created by mayhew2 on 2/22/16.
 */
public class GetParallel {
    private static Logger logger = LoggerFactory.getLogger( GetParallel.class );

    //static String basedir = "/shared/corpora/corporaWeb/lorelei/LDC2015E70_BOLT_LRL_Hausa_Representative_Language_Pack_V1.2/";
    //    static String otherlang = "ha";

//    static String basedir = "/shared/corpora/corporaWeb/lorelei/LDC2016E29_BOLT_LRL_Uzbek_Representative_Language_Pack_V1.0/";
//    static String otherlang = "uz";

//    static String basedir = "/shared/corpora/corporaWeb/lorelei/LDC2014E115_BOLT_LRL_Turkish_Representative_Language_Pack_V2.2/";
//    static String otherlang = "tr";

//    static String basedir = "/shared/corpora/corporaWeb/lorelei/20150908-kickoff-release/REFLEX_Tagalog_LDC2015E90_V1.1/";
//    /**
//     * This needs to be the value that the folder is called in the LORELEI directory structure (usually 3 letters)
//     */
//    static String otherlang = "tgl";

     static String basedir = "/shared/corpora/corporaWeb/lorelei/20150908-kickoff-release/REFLEX_Bengali_LDC2015E13_V2.1/";
     static String otherlang = "ben";
     static String otherlang_short = "bn";
    

//    static String basedir = "/shared/corpora/corporaWeb/lorelei/20150908-kickoff-release/REFLEX_Thai_LDC2015E84_V1.1/";
//    static String otherlang = "tha";
//    static String otherlang_short = "th";
    

//    static String basedir = "/shared/corpora/corporaWeb/lorelei/20150908-kickoff-release/REFLEX_Tamil_LDC2015E83_V1.1/";
//    static String otherlang = "tam";
//    static String otherlang_short = "ta";

//    static String basedir = "/shared/corpora/corporaWeb/lorelei/20150908-kickoff-release/REFLEX_Yoruba_LDC2015E91_V1.1/";
//    static String otherlang = "yor";
//    static String otherlang_short = "yo";

//    static String basedir = "/shared/corpora/corporaWeb/lorelei/dryrun/LDC2016E56_LORELEI_Year1_Dry_Run_Evaluation_IL2_V1.1/set0/";
//    static String otherlang = "cmn";
//    static String otherlang_short = "ch";


    static String dtdpath = basedir + "dtds/";
    static String transdir = "data/translation/";

    static String outdir = "/shared/corpora/ner/parallel/" + otherlang_short + "/";

    private static List<String> engsents = new ArrayList<>();
    private static List<String> fornsents = new ArrayList<>();
    private static List<String> fmap = new ArrayList<>();

    
    /**
     * Given a path, this returns all files from that path+ltf, and filters social media.
     * @param path
     * @return
     */
    public static File[] readPath(String path){

        File f = new File(path + "/ltf");

        File[] flist = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.startsWith("SN");
            }
        });

        if(f.exists()) {
            logger.debug("Length: " + flist.length);
        }else{
            System.out.printf("whoops, file " + f + " does not exist.");
        }

        return flist;

    }

    /**
     * Given the first child of a SEG branch, iterate through and
     * gather the text in each of the tokens. Concatenate these with whitespace.
     * @param firstChild the first child of a SEG branch in an LTF file.
     * @return all tokens in this segment concatenated with a space
     */
    public static String getTokenizedLine(Node firstChild){
        List<String> toks = new ArrayList<>();
        while((firstChild = firstChild.getNextSibling()) != null){
            if(firstChild.getNodeName().equals("TOKEN")){
                toks.add(firstChild.getTextContent());
            }
        }
        return StringUtils.join(toks, " ");
    }


    /**
     * This gets the monolingual text.
     * @param basedir
     */
    public static void getmonotext(String basedir) throws IOException, XMLException {


        File[] fnames = (new File(basedir)).listFiles();

        List<String> fornlines = new ArrayList<>();

        for(File fname : fnames){
            Document fornltf = SimpleXMLParser.getDocument(fname, dtdpath);
            NodeList fsegs = fornltf.getElementsByTagName("SEG");

            for(int s = 0; s < fsegs.getLength(); s++){
                Node fseg = fsegs.item(s);
                String fline = getTokenizedLine(fseg.getFirstChild());
                fornlines.add(fline);
            }

        }

        logger.info("Writing to file...");
        LineIO.write(outdir + "/mono.txt", fornlines);


    }

    /**
     * Given two files, this checks if they are the same (using ID), and then reads them, line by line into the
     * private static data structures.
     * @param eng
     * @param forn
     * @return
     */
    public static void readParallel(File eng, File forn) throws XMLException, IOException {
        // perhaps do some kind of comparison of file names to make sure they are the same???
        String engid = eng.getName().split("\\.")[0];
        String fornid = forn.getName().split("\\.")[0];

        if(!engid.equals(fornid)){
            System.out.println("NOT THE SAME! " + eng.getName() + " : " + forn.getName());
        }else {

            Document engltf = SimpleXMLParser.getDocument(eng, dtdpath);
            Document fornltf = SimpleXMLParser.getDocument(forn, dtdpath);
            NodeList esegs = engltf.getElementsByTagName("SEG");
            NodeList fsegs = fornltf.getElementsByTagName("SEG");

            boolean comp = false;
            
            int limit;
            if(esegs.getLength() != fsegs.getLength()){
                logger.error("Diff size files? " + eng + ", " + forn);
                logger.error(esegs.getLength() + " : " + fsegs.getLength());
                limit = Math.min(esegs.getLength(), fsegs.getLength());
                // this is just going to be too difficult...
                comp = true;
            }else{
                limit = esegs.getLength();
            }

            // if this is only comparable...
            if(comp){
                List<String> engcomp = new ArrayList<>();
                
                // write out to comparable
                for(int s = 0; s < esegs.getLength(); s++){
                    Node eseg = esegs.item(s);

                    String esegid = ((DeferredElementImpl) eseg).getAttribute("id");
                    String eline = getTokenizedLine(eseg.getFirstChild());
                    engcomp.add(eline);
                }

                List<String> forncomp = new ArrayList<>();
                for(int s = 0; s < fsegs.getLength(); s++){
                    Node fseg = fsegs.item(s);

                    String fsegid = ((DeferredElementImpl) fseg).getAttribute("id");
                    String fline = getTokenizedLine(fseg.getFirstChild());
                    forncomp.add(fline);

                }

                // notice that engid is the same for each (check is above)
                logger.info("Writing comp to file...");
                LineIO.write(outdir + "/comp/" + engid + ".eng", engcomp);
                LineIO.write(outdir + "/comp/" + engid + "." + otherlang_short, forncomp);
                

                
            }else{
                for(int s = 0; s < limit; s++){
                    Node eseg = esegs.item(s);
                    Node fseg = fsegs.item(s);

                    // FIXME: check that ID is the same.
                    String esegid = ((DeferredElementImpl) eseg).getAttribute("id");
                    String fsegid = ((DeferredElementImpl) fseg).getAttribute("id");

                    if(!esegid.equals(fsegid)){
                        //logger.error("Segment ids don't match up: " + esegid + " : " + fsegid);
                    }

                    String eline = getTokenizedLine(eseg.getFirstChild());
                    String fline = getTokenizedLine(fseg.getFirstChild());

                    engsents.add(eline);
                    fornsents.add(fline);

                    fmap.add(engid + ":" + esegid);

                }
            }


            

        }
    }

    /**
     * Walk directories, with certain heuristics.
     * @param path
     */
    public static void walk(String path) throws XMLException,IOException {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;


        for ( File f : list ) {
            if ( f.isDirectory() ) {
                File[] locallist = f.listFiles();
                HashMap<String, File> names = new HashMap<>();
                for(File localf : locallist){
                    names.put(localf.getName(), localf);
                }

                // if this contains just eng/forn directories, good.
                if(locallist.length == 2 && names.containsKey("eng")){
                    logger.debug("Reading: " + f);


                    // this returns all files (except twitter files).
                    File[] eng = readPath(names.get("eng").getAbsolutePath());
                    File[] forn = readPath(names.get(otherlang).getAbsolutePath());

                    //if(eng.length != forn.length){
                    //    logger.error("WE HAVE A PROBLEM! " + names.get("eng") + " and " + names.get(otherlang) + " have diff lengths.");
                    //}else {
                        // make sure they are the same...
                        Arrays.sort(eng);
                        Arrays.sort(forn);
                        
                        // TODO: somehow align them here...
                        
                        int limit = Math.min(eng.length, forn.length);
                        
                        for (int i = 0; i < limit; i++) {
                            readParallel(eng[i], forn[i]);
                        }

                        logger.debug("successfully read files.");
                        //}

                }else {
                    // otherwise, continue looking.
                    walk(f.getAbsolutePath());
                }
            }
        }

    }

    public static void main(String[] args) throws XMLException, IOException {

        walk(basedir + transdir);
//
//        System.out.println(engsents.size());
//        System.out.println(fornsents.size());
//
        LineIO.write(outdir + "eng.txt", engsents);
        LineIO.write(outdir + otherlang_short + ".txt", fornsents);
        LineIO.write(outdir + "map.txt", fmap);

//        getmonotext(basedir + "/data/monolingual_text/ltf/");

        
    }

}