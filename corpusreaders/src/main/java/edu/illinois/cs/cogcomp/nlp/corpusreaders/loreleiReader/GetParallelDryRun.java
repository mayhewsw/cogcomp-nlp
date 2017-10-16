package edu.illinois.cs.cogcomp.nlp.corpusreaders.loreleiReader;

import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.reader.commondatastructure.XMLException;
import edu.illinois.cs.cogcomp.reader.util.SimpleXMLParser;
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
import java.util.HashMap;
import java.util.List;

/**
 * Created by mayhew2 on 6/3/16.
 */
@SuppressWarnings("Duplicates")
public class GetParallelDryRun {

    /**
     * This is intended to get plain text from ltf files in order to get parallel alignments.
     * Created by mayhew2 on 2/22/16.
     */

        private static Logger logger = LoggerFactory.getLogger( GetParallel.class );

     static String basedir = "/shared/corpora/corporaWeb/lorelei/evaluation-20160705/LDC2016E57_LORELEI_IL3_Incident_Language_Pack_for_Year_1_Eval/set0/";
    ///shared/corpora/corporaWeb/lorelei/dryrun/LDC2016E56_LORELEI_Year1_Dry_Run_Evaluation_IL2_V1.1/set0/";
        static String otherlang = "il3";
        static String otherlang_short = "ug";


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
        public static HashMap<String, File> readPath(String path){

            File f = new File(path);

            File[] flist = f.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !name.startsWith("SN");
                }
            });


            HashMap<String, File> filemap = new HashMap<>();
            for(File ff : flist){

                String id = ff.getName().split("\\.")[0];

                filemap.put(id, ff);
            }


            if(f.exists()) {
                logger.debug("Length: " + flist.length);
            }else{
                System.out.printf("whoops, file " + f + " does not exist.");
            }

            return filemap;

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
         * Given two files, this checks if they are the same (using ID), and then reads them, line by line into the
         * private static data structures.
         * @param eng
         * @param forn
         * @return
         */
        public static void readParallel(File eng, File forn, SentenceAlignments sa) throws XMLException {
            // perhaps do some kind of comparison of file names to make sure they are the same???
            String engid = eng.getName().split("\\.")[0];
            String fornid = forn.getName().split("\\.")[0];

            logger.info("In read parallel... with " + eng);

            Document engltf = SimpleXMLParser.getDocument(eng, ""); //dtdpath);
            Document fornltf = SimpleXMLParser.getDocument(forn, ""); // dtdpath);
            NodeList esegs = engltf.getElementsByTagName("SEG");
            NodeList fsegs = fornltf.getElementsByTagName("SEG");

            for(Pair<String, String> p : sa.alignments){

                //logger.info("Reading alignments " + p);

                // source refers to forn
                String sourcesegments = p.getFirst();
                // translation refers to English
                String translationsegments = p.getSecond();

                String eline = "";
                String fline = "";
                boolean allgood = true;

                for(String sourceseg : sourcesegments.split("\\s+")){
                    sourceseg = sourceseg.replace("segment-", "");
                    int ss = Integer.parseInt(sourceseg);
                    Node fseg = fsegs.item(ss);
                    if(fseg == null){
                        allgood = false;
                        break;
                    }
                    fline += getTokenizedLine(fseg.getFirstChild());
                }

                for(String transseg : translationsegments.split("\\s+")){
                    transseg = transseg.replace("segment-", "");
                    int ts = Integer.parseInt(transseg);
                    Node eseg = esegs.item(ts);
                    if(eseg == null){
                        allgood = false;
                        break;
                    }
                    eline += getTokenizedLine(eseg.getFirstChild());
                }

                if(allgood) {
                    engsents.add(eline);
                    fornsents.add(fline);
                    fmap.add(sa.alignfile + ":" + translationsegments + ":" + sourcesegments);
                }

                //

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
                    if(locallist.length == 3 && names.containsKey("eng") && names.containsKey("sentence_alignment")){
                        logger.info("Reading: " + f);


                        // this returns all files (except twitter files).
                        HashMap<String, File> eng = readPath(names.get("eng").getAbsolutePath() + "/ltf/");
                        HashMap<String, File> forn = readPath(names.get(otherlang).getAbsolutePath() + "/ltf/");
                        HashMap<String, File> sentence_alignment = readPath(names.get("sentence_alignment").getAbsolutePath());

                        int i =1;

                        for (String safilename : sentence_alignment.keySet()) {

                            // intended to keep only the Koran data.
                            if(!safilename.contains("G0040E9WQ")) continue;


                            if(i%100 == 0) {
                                System.out.println("on " + i + " out of " + sentence_alignment.size());
                            }
                            i++;
                            File safile = sentence_alignment.get(safilename);

                            // from here, I should be able to get the corresponding file ids
                            // and the map of segment ids.
                            SentenceAlignments sa = new SentenceAlignments(safile);

                            readParallel(eng.get(sa.sourceid), forn.get(sa.translationid), sa);
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

            System.out.println(engsents.size());
            System.out.println(fornsents.size());

            LineIO.write(outdir + "eng.txt", engsents);
            LineIO.write(outdir + otherlang_short + ".txt", fornsents);
            LineIO.write(outdir + "map.txt", fmap);




        }


    private static class SentenceAlignments {

        private String alignfile;
        private String sourceid;
        private String translationid;
        private List<Pair<String, String>> alignments;

        private SentenceAlignments(File sa) throws XMLException {

            alignfile = sa.getName();

            logger.info("Reading: " + alignfile);

            alignments = new ArrayList<>();

            Document alignfile = SimpleXMLParser.getDocument(sa);

            Node alignmentstag = alignfile.getElementsByTagName("alignments").item(0);
            sourceid = ((DeferredElementImpl) alignmentstag).getAttribute("source_id").split("\\.")[0];
            translationid = ((DeferredElementImpl) alignmentstag).getAttribute("translation_id").split("\\.")[0];


            NodeList sourcetags = alignfile.getElementsByTagName("source");
            NodeList translationtags = alignfile.getElementsByTagName("translation");

            if(sourcetags.getLength() != translationtags.getLength()){
                System.err.println("DIFFERENT LENGTH LISTS OF TAGS IN SENTENCE_ALIGNMENT FILE!");
            }

            for(int a = 0; a < sourcetags.getLength(); a++){
                Node source = sourcetags.item(a);
                Node translation = translationtags.item(a);

                String sourcesegments = ((DeferredElementImpl) source).getAttribute("segments");
                String translationsegments = ((DeferredElementImpl) translation).getAttribute("segments");

                alignments.add(new Pair<>(sourcesegments, translationsegments));
            }

        }
    }



}
