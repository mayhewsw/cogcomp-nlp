#!/usr/bin/python
import os
from collections import defaultdict

def func(fname):
    with open(fname) as f:
        lines = f.readlines()

    outlines = []
    
    for line in lines:
        sline = line.split("\t")
        if len(sline) > 5:
            oval = sorted(sline[6].split(","))[2]
            if sline[0] == "O":            
                sline[6] = oval.split(":")[1]
                #sline[6] = "1.0"
            else:
                sline[6] = "1.0"
            
                        
        outlines.append("\t".join(sline))
                

    
    with open(fname + ".weights", "w") as out:
        for line in outlines:
            out.write(line)
        
    
    
if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="")

    parser.add_argument("fname",help="")
    
    args = parser.parse_args()
    
    func(args.fname)
