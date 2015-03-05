from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

kilns = [[1,10],[1,20]]				#[number of kilns,fire time]
pieces = [[15,3,2],[7,2,2],[5,1,2]]		#[time needed to bake a piece, time needed to treat it, number of this type piece] 
def main():
	s =0
	for i in range(len(pieces)):
		s +=pieces[i][2]
	printProblem("tmsflat." + str(s) + ".pb.anml")
	printProblem("tmshier." + str(s) + ".pb.anml")

def printProblem(filename):
    with open(filename, "w") as f:
        f.write("instance Kiln k" + str(kilns[0][1]) )
        for i in range(1,len(kilns)):
            f.write(", k" + str(kilns[i][1]))
        f.write(";\n")
        for t in range(len(pieces)):
            f.write("instance Piece p" + str(t) + "_0")
            for i in range(1,pieces[t][2]):
                f.write(", p" + str(t) + "_" + str(i))
            f.write(";\n")

        f.write("[end] bakedStructure(p0_0,p1_0) == true;\n")
				
        for i in range(0,len(kilns)):
            f.write("fireTime(k" + str(kilns[i][1]) + ") := " + str(kilns[i][1]) + ";\n")
        for t in range(len(pieces)):
            for i in range(pieces[t][2]):
                f.write("bakeTime(p" + str(t) + "_" + str(i) + ") := " + str(pieces[t][0]) + ";\n")
                f.write("treatTime(p" + str(t) + "_" + str(i) + ") := " + str(pieces[t][1]) + ";\n")
        f.write("[start] {\n\tenergy := true;\n")
        for i in range(0,len(kilns)):
            f.write("\tk" + str(kilns[i][1]) + ".empty := true ;\n")
        f.write("};\n")
       
		


if __name__ == "__main__":
	main() 
