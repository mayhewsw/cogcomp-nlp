package edu.illinois.cs.cogcomp.pos;


import edu.illinois.cs.cogcomp.lbjava.nlp.ColumnFormat;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.nlp.seg.Token;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
//    import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by mayhew2 on 9/13/16.
 */
public class ColumnParser extends ColumnFormat {


    String filename = null;

    public ColumnParser(String file) {
        super(file);
        filename = file;
    }

    public Object next() {
        // System.out.println("next");
        String[] line = (String[]) super.next();
        while (line != null && (line.length == 0 || line[4].equals("-X-")))
            line = (String[]) super.next();
        if (line == null)
            return null;

        LinkedVector res = new LinkedVector();

        Word w = new Word(line[5]);
        Token t = new Token(w, null, line[0]);

        res.add(t);

        for (line = (String[]) super.next(); line != null && line.length > 0; line =
                (String[]) super.next()) {

            Word w2 = new Word(line[5]);
            Token t2 = new Token(w2, null, line[0]);
            res.add(t2);

        }
        if (res.size() == 0)
            return null;

        return res;
    }


}
