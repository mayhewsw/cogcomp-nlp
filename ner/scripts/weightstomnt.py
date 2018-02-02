#!/usr/bin/python

#!/home/mayhew2/miniconda3/bin/python
import os,codecs
from collections import defaultdict

## Count unique tokens in a conll folder.

def func(infile, outfile):
        
    with open(infile) as f:
        lines = f.readlines()
    
    outlines = []
    for line in lines:            
        sline = line.split("\t")
        if len(sline) > 5:            
            if "MNT" in sline[0]:
                sline[0] = "O"
                sline[6] = "0.0"
        outlines.append("\t".join(sline))

    with open(outfile, "w") as out:
        for l in outlines:
            out.write(l)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="")

    parser.add_argument("folder",help="")
    parser.add_argument("outfolder",help="")

    args = parser.parse_args()
    
    func(args.folder, args.outfolder)
