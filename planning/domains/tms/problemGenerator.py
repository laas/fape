from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

kilns = [[1,8],[1,20]]				#[number of kilns,fire time]
pieces = [[15,3,5],[7,2,5],[5,1,5]]		#[time needed to bake a piece, time needed to treat it, number of this type piece] 
nbgoal =1
goalf=""
goalh=""

def main():
    global nbgoal
    if len(sys.argv) > 1:
        nbgoal = int(sys.argv[1])
    n = 0
    for i in range(len(pieces)):
        n+=pieces[i][2]
    assert (n/2 >= nbgoal),"Not enough pieces to this goal, please add some more on problemGenerator.py (line 13)"
    goal()
    printProblem("tmsflat.p" + str(nbgoal) + ".pb.anml",0)
    printProblem("tmshier.p" + str(nbgoal) + ".pb.anml",1)

def printProblem(filename,version):
    with open(filename, "w") as f:
        f.write("instance Kiln k0_" + str(kilns[0][1]) )
        for i in range(len(kilns)):
            for j in range(kilns[i][0]):
                if (i!=0 or j != 0):
                    f.write(", k" + str(j) + "_" + str(kilns[i][1]))
        f.write(";\n")
        for t in range(len(pieces)):
            f.write("instance Piece p" + str(t) + "_0")
            for i in range(1,pieces[t][2]):
                f.write(", p" + str(t) + "_" + str(i))
            f.write(";\n")
        if version:
            f.write(goalh)
	else:
            f.write(goalf)			
        for i in range(len(kilns)):
            for j in range(kilns[i][0]):
                f.write("fireTime(k" + str(j) + "_" + str(kilns[i][1]) + ") := " + str(kilns[i][1]) + ";\n")
        for t in range(len(pieces)):
            for i in range(pieces[t][2]):
                f.write("bakeTime(p" + str(t) + "_" + str(i) + ") := " + str(pieces[t][0]) + ";\n")
                f.write("treatTime(p" + str(t) + "_" + str(i) + ") := " + str(pieces[t][1]) + ";\n")
        f.write("[start] {\n\tenergy := true;\n")
        for i in range(len(kilns)):
            for j in range(kilns[i][0]):
                f.write("\tk" + str(j) + "_" + str(kilns[i][1]) + ".empty := true;\n")
        for t in range(len(pieces)):
			for i in range(pieces[t][2]):
				f.write("\tp" + str(t) + "_" + str(i) +".baked := false;\n")
				f.write("\tp" + str(t) + "_" + str(i) +".treated := false;\n")
				for t1 in range(len(pieces)):
					for i1 in range(pieces[t][2]):
						if not ((t == t1 ) and (i == i1)) :
							f.write("\tstructured(p" + str(t) + "_" + str(i) +",p"+ str(t1) + "_" + str(i1) + ") := false;\n")
							f.write("\tbakedStructure(p" + str(t) + "_" + str(i) +",p"+ str(t1) + "_" + str(i1) + ") := false;\n")
        f.write("};\n")
       
def goal():
	global goalf
	global goalh
	h = ""
	f = ""
	for i in range( nbgoal):
		h+="constant Kiln k" + str(i) + ";\n"
	h+= "[all] contains {\n"
	rn =[]
	for i in range( nbgoal):
		r = random.randrange(len(pieces))
		n = random.randrange(pieces[r][2])
		nr1="p" + str(r) + "_" + str(n)
		while(nr1 in rn):
			r = random.randrange(len(pieces))
			n = random.randrange(pieces[r][2])
			nr1="p" + str(r) + "_" + str(n)	
		rn.append(nr1)		
		r = random.randrange(len(pieces))
		n = random.randrange(pieces[r][2])
		nr2="p" + str(r) + "_" + str(n)
		while(nr2 in rn):
			r = random.randrange(len(pieces))
			n = random.randrange(pieces[r][2])
			nr2="p" + str(r) + "_" + str(n)		
		rn.append(nr2)		
		h+="\tbakeStructureH(" + nr1 + "," + nr2 + ",k" + str(i) + ");\n"
		f+="[end]bakedStructure(" + nr1 + "," + nr2 + ") == true;\n"
	h+="};\n"
	goalf = f
	goalh = h



if __name__ == "__main__":
	main() 
