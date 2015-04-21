from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

brickGround = 6
bricks = 100
worker = 3
sizeWall = 2
truck = 2
def goal ():
	g = "[all]contains {\n\tbuild" + str(sizeWall) + "(b0"
	for i in range(1,sumf(sizeWall)):
		g+=", b" + str(i) 
	g+=");\n};\n"
	return g

def main():
    global sizeWall
    global brickGround
    global bricks
    if len(sys.argv) > 1:
        sizeWall = int(sys.argv[1])
        brickGround = sizeWall +1
    bricks = sumf(sizeWall);
    assert (sizeWall + 1 <= brickGround )," not enough none bricks"
    printProblem("wall." + str(sizeWall) + ".pb.anml")


def printProblem(filename):
    with open(filename, "w") as f:
        for i in range(1,sizeWall+1):
            f.write(brickline(i))
        f.write("action build" + str(sizeWall) + "(Brick b0")
        for i in range(1,sumf(sizeWall)):
            f.write (", Brick b" + str(i) )
        f.write("){\n")
        for i in range(sumf(sizeWall)):
		    for j in range(i+1,sumf(sizeWall)):
			    f.write("\tb" + str(i) + " != b" + str(j) +";\n")
        f.write("\t[start] {\n")
        for i in range(sumf(sizeWall)):
            f.write("\t\tb" + str(i) + ".loc == t0;\n")
        f.write("\t};\n")
        f.write("\t[all] contains {\n")
        b = 0;
        s = 1;
        for i in range (1,sizeWall):
            f.write("\t\tl" + str(i) + " : line" + str(i) + "(")
            for j in range(i):
                if j == 0 :
                    f.write("b" + str(b))
                else :
                    f.write(", b" + str(b))
                b+=1
            for j in range(i+1):
                f.write(", b" + str(s))
                s+=1
            f.write(");\n")
        f.write("\t\tl" + str(sizeWall) + " : line" + str(sizeWall) + "( b" + str(b) )
        b+=1
        for j in range(sizeWall-1):
            f.write(", b" + str(b) )
            b+=1
        for j in range(sizeWall+1):
            f.write(", none" + str(j) )
        f.write(");\n")
        f.write("\t};\n")
        i = sizeWall
        while(i > 1):
           f.write("\tend(l" + str(i) + ") < start(l" + str(i-1) + ");\n") 
           i-=1
        f.write("};\n")
        f.write("instance Brick b0" )
        for i in range(1,bricks):
            f.write(", b" + str(i))
        f.write(";\n")
        f.write("instance Worker w0" )
        for i in range(1,worker):
            f.write(", w" + str(i))
        f.write(";\n")
        f.write("instance Brick none0" )
        for i in range(1,brickGround):
            f.write(", none" + str(i))
        f.write(";\n")
        f.write("instance Truck t0" )
        for i in range(1,truck):
            f.write(", t" + str(i))
        f.write(";\n")

        f.write("[start] {\n")
        for i in range(brickGround):
            f.write("\tnone" + str(i) + ".loc := wall;\n")
            f.write("\tnone" + str(i) + ".cemented := false;	\n")
        f.write("\thaveCement := false;\n")
        for i in range(bricks):
            f.write("\tb" + str(i) +".cemented := false; \n")
            f.write("\tb" + str(i) +".loc := t0; \n")
        for i in range(worker):
            f.write("\tw" + str(i) + ".at := t0;\n")
            f.write("\tw" + str(i) + ".available := true;\n")
        f.write("};\n")
	f.write(goal())

def brickline(nb):
	g= ""
	g+="action line" + str(nb) + "(Brick b0 "
	for i in range(1,nb):
		g+=", Brick b" + str(i) 
	for i in range(nb+1):
		g+=", Brick sb" + str(i)
	g+="){\n"
	for i in range((nb*2)+1):
		g+="\tconstant Worker w" + str(i) +";\n"
	for i in range(nb):
		g+="\tconstant Truck t" + str(i) + ";\n"
	g+="\t[all] contains {\n"
	w=0
	for i in range(nb):
		g+="\t\tpi" + str(i) + " : pick(w" + str(w) + ", b" + str(i) + ", t" + str(i) + ");\n"
		g+="\t\tg" + str(i) + " : goto(w" + str(i) + ", t" + str(i) + ",  wall);\n"
		g+="\t\tpl" + str(i) + " : place(w" + str(w) + ", b" + str(i) + ", sb" + str(i) + ", sb" + str(i+1) + ");\n"
		w+=1
	for i  in range(nb+1):
		g+="\t\t c" + str(i) + " : putCementBrick(w" +str(w) + ", sb" + str(i) + ");\n"
		w+=1
	g+="\t};\n"
	for i in range(nb):
		g+="\tend(pi" + str(i) + ") < start(g" + str(i) + ");\n"  
		g+="\tend(g" + str(i) + ") < start(pl" + str(i) + ");\n"               
		g+="\tend(c" + str(i) + ") < start(pl" + str(i) + ");\n"
		g+="\tend(c" + str(i+1) + ") < start(pl" + str(i) + ");\n"
		g+="\tend(pl" + str(i) + ") < end(c" + str(i) + ") + 10;\n"
		g+="\tend(pl" + str(i) + ") < end(c" + str(i+1) + ") + 10;\n"
	g+="};\n"
	return g

def sumf(num):
	if num < 2:
		return 1
	else :
		return num + sumf(num-1)

if __name__ == "__main__":
    main()
