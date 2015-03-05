from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

kilns = [[1,10],[1,20]]	#[number of kilns,fire time]
pieces = [[15,3],[7,2],[5,1]]	#[time needed to bake a piece,time needed to treat it] 
#for now problemGeneratorflat.py is working for only 3 types of pieces
typePiece = 3

def main():
	n = [1,1,1]
	if len(sys.argv) > 1:
		n[0] = int(sys.argv[1])
	if len(sys.argv) > 2:
		n[1] = int(sys.argv[2])
	if len(sys.argv) > 3:
		n[2] = int(sys.argv[3])
	s =0
	for i in range(len(n)):
		s +=n[i]
	printProblem("tmsflat." + str(s) + ".pb.anml",n)
	printProblem("tmshier." + str(s) + ".pb.anml",n)

def printProblem(filename,n):
    with open(filename, "w") as f:
        f.write("instance Kiln k" + str(kilns[0][1]) )
        for i in range(1,len(kilns)):
            f.write(", k" + str(kilns[i][1]))
        f.write(";\n")
        for t in range(typePiece):
            f.write("instance Piece p" + str(t) + "_0")
            for i in range(1,n[t]):
                f.write(", p" + str(t) + "_" + str(i))
            f.write(";\n")

        f.write("[end] bakedStructure(p0_0,p1_0) == true;\n")
				
        for i in range(0,len(kilns)):
            f.write("fireTime(k" + str(kilns[i][1]) + ") := " + str(kilns[i][1]) + ";\n")
        for t in range(typePiece):
            for i in range(n[t]):
                f.write("bakeTime(p" + str(t) + "_" + str(i) + ") := " + str(pieces[t][0]) + ";\n")
                f.write("treatTime(p" + str(t) + "_" + str(i) + ") := " + str(pieces[t][1]) + ";\n")
        f.write("[start] {\n\tenergy := true;\n")
        for i in range(0,len(kilns)):
            f.write("\tk" + str(kilns[i][1]) + ".empty := true ;\n")
        f.write("};\n")
       
		


if __name__ == "__main__":
	main() 
