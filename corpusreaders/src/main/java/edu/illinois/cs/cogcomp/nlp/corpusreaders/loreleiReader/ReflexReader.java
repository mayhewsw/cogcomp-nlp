package edu.illinois.cs.cogcomp.reader.lorelei;

import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.reader.commondatastructure.XMLException;
import edu.illinois.cs.cogcomp.reader.example.EntityCorpusGenerator;
import edu.illinois.cs.cogcomp.reader.util.IOManager;
import edu.illinois.cs.cogcomp.reader.util.SimpleXMLParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static edu.illinois.cs.cogcomp.reader.lorelei.LoreleiReader.nerdir;

/**
 * This is an almost duplicate of {@link LoreleiReader}. Some of the REFLEX data packages
 * needed different formatting, so instead of making error-prone conditional changes to LoreleiReader,
 * I just copied it.
 *
 * Notably: the laf files index annotations by token, not by char.
 *
 * This works for bengali
 *
 * Created by mayhew2 on 3/12/16.
 */
@SuppressWarnings("Duplicates")
public class ReflexReader {

    private static Logger logger = LoggerFactory.getLogger( LoreleiReader.class );


    static String kickoff = "/shared/corpora/corporaWeb/lorelei/20150908-kickoff-release/";

//    static String basedir = kickoff + "REFLEX_Bengali_LDC2015E13_V2.1/";
//    static String lang = "ben";

//    static String basedir = kickoff + "REFLEX_Tamil_LDC2015E83_V1.1/";
//    static String lang = "tam";

    //static String basedir = kickoff + "REFLEX_Tagalog_LDC2015E90_V1.1/";
    //static String lang = "tgl";

    //static String basedir = kickoff + "REFLEX_Yoruba_LDC2015E91_V1.1/";
    //static String lang = "yor";

    //static String basedir = kickoff + "REFLEX_Thai_LDC2015E84_V1.1/";
    //static String lang = "tha";

//    static String basedir = kickoff + "REFLEX_Hungarian_LDC2015E82_V1.1/";
//    static String lang = "hun";
//    static String shortlang = "hu";

    static String basedir = kickoff + "REFLEX_Amharic_NMSU_LDC2015G02_V1.1/";
    static String lang = "amh";
    static String shortlang = "am";


    static String nerdir =  basedir + "data/annotation/entity_annotation/simple/";
//    static String nerdir =  basedir + "data/annotation/entity/";
    static String dtdpath = basedir + "dtds/";


    static String[] ltfdirs = {nerdir, basedir + "data/monolingual_text/ltf/", basedir + "data/translation/from_eng/news/"+lang+"/ltf/"};

    static String outdir = "/shared/corpora/ner/lorelei/" + shortlang + "/All/";


    private static List<String> excludetags = Arrays.asList("TTL", "TIME", "NAN");


    public static void readFiles(String outdir) throws XMLException, IOException {

        if(lang.equals("yor")){
            nerdir += "without_tone/";
            ltfdirs[0] += "without_tone/";
        }

        logger.debug("laf directory: " + nerdir);
        String[] laf_files = IOManager.listDirectory(nerdir);

        logger.debug("Number of laf files: " + laf_files.length);

        for(String laf_fname : laf_files) {
            if(!laf_fname.contains("laf")){
                continue;
            }

            logger.debug(laf_fname);
            Document laf = SimpleXMLParser.getDocument(new File(nerdir + laf_fname), dtdpath);

            String docname = FilenameUtils.removeExtension(FilenameUtils.removeExtension(laf_fname));

            HashMap<String, String> annotationmap = new HashMap<>();
            NodeList els = laf.getElementsByTagName("ANNOTATION");
            for(int i = 0 ; i < els.getLength(); i++){
                Node annotation = els.item(i);
                NamedNodeMap nnmap = annotation.getAttributes();

                String label;
                if(nnmap.getNamedItem("type") != null) {
                    label = nnmap.getNamedItem("type").getNodeValue();
                }else{
                    NodeList t = ((DeferredElementImpl) annotation).getElementsByTagName("TAG");
                    Node tag = t.item(0);
                    label = tag.getTextContent();
                }

                Node extent = annotation.getFirstChild();
                while(extent.getNodeName() != "EXTENT"){
                    extent = extent.getNextSibling();
                }

                String start_token_id = nnmap.getNamedItem("start_token").getNodeValue();
                String end_token_id = nnmap.getNamedItem("end_token").getNodeValue();

                // fix these by using laf_fname

                String[] startsplit = start_token_id.split("-");
                startsplit[0] = docname;
                start_token_id = StringUtils.join(startsplit, "-");

                String[] endsplit = end_token_id.split("-");
                endsplit[0] = docname;
                end_token_id = StringUtils.join(endsplit, "-");

                if(startsplit.length != 3 || endsplit.length != 3){
                    continue;
                }

                int start_token = Integer.parseInt(startsplit[2]);
                int end_token = Integer.parseInt(endsplit[2]);

                if(!excludetags.contains(label)) {
                    annotationmap.put(start_token_id, "B-" + label);

                    String[] idsplit = start_token_id.split("-");
                    String idnotoken = idsplit[0] + "-" + idsplit[1] + "-";

                    for (int j = start_token + 1; j <= end_token; j++) {
                        annotationmap.put(idnotoken + j, "I-" + label);
                    }
                }
            }


            // Get the document name from the file.
            NodeList doc = laf.getElementsByTagName("DOC");
            if (doc.getLength() != 1) {
                logger.error("WHAT IS WRONG WITH THIS DOCUMENT??? Has more than one doc tag.");
                continue;
            }


            File ltf_file = null;
            for(String dir : ltfdirs){
                String fulldoc = dir + docname + ".ltf.xml";
                ltf_file = new File(fulldoc);

                if(ltf_file.exists()){
                    break;
                }

            }

            if(ltf_file == null || !ltf_file.exists()) {
                logger.debug("skipping...");
                continue;
            }


            // look for it elsewhere...


            Document ltf = SimpleXMLParser.getDocument(ltf_file, dtdpath);

            List<String> conlllines = new ArrayList<>();
            conlllines.add("O       0       0       O       -X-     -DOCSTART-      x       x       0");

            // each segment is a sentence.
            NodeList segs = ltf.getElementsByTagName("SEG");
            for (int s = 0; s < segs.getLength(); s++) {
                Node seg = segs.item(s);
                NamedNodeMap segattr = seg.getAttributes();

                NodeList segchildren = seg.getChildNodes();
                for (int n = 0; n < segchildren.getLength(); n++) {
                    Node child = segchildren.item(n);

                    if (child.getNodeName() == "TOKEN") {

                        String word = child.getTextContent();
                        NamedNodeMap childattr = child.getAttributes();
                        String id = childattr.getNamedItem("id").getNodeValue();

                        String[] idsplit = id.split("-");
                        int tokenid = Integer.parseInt(idsplit[2]);

                        //int end_char = Integer.parseInt(childattr.getNamedItem("end_char").getNodeValue());

                        // use these to get the annotations
                        String label = "O";
                        if(annotationmap.containsKey(id)){
                            label = annotationmap.get(id);
                        }

                        conlllines.add(LoreleiReader.conllline(label, tokenid, word));
                    }
                }

                // empty line between sentences.
                conlllines.add("");
            }

            LineIO.write(outdir + docname + ".conll", conlllines);

        }

    }

    public static void main(String[] args) throws XMLException, IOException {
        BasicConfigurator.configure();
        readFiles(outdir);
    }
}
