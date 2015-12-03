from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

nbarm = 2
loc = [1,2] #shelf,books per shelf
objects = []

def init():
    lr =[]
    j =0
    for i in range( loc[0]):
        for j in range(loc[1]):
            r = random.randrange(loc[0])
            n = random.randrange(loc[1])
            while ([r,n]) in lr:
                r = random.randrange(loc[0])
                n = random.randrange(loc[1])
            lr.append([r,n])	
            objects.append(["book" + str(i) + "_" + str(j),"l" + str(r) + "_" + str(n) ])
    g=""
    for j in range(loc[0]):
        for i in range(1,loc[1]):
            g +="constant Location l0" +  str(j) + str(i) + ";\n"
            g +="constant Location l1" +  str(j) + str(i) + ";\n"
            g +="[end] book" +  str(j) + "_" + str(i-1) + ".at == l0"  +  str(j) + str(i) + ";\n"
            g +="[end] book" +  str(j) + "_" + str(i) + ".at == l1"  +  str(j) + str(i) + ";\n"
            g +="atRight(l0"  +  str(j) + str(i) + ") == l1"  +  str(j) + str(i) + ";\n"	
    return g

def main():
	global loc
	if len(sys.argv) > 1:
		loc[0] = int(sys.argv[1])
	if len(sys.argv) > 2:
		loc[1] = int(sys.argv[2])
	goal = init()
	printProblem("libraryflat.p" + str(loc[0]) + "_" + str(loc[1]) + ".pb.anml",goal)

def printProblem(filename,goal):
    l = []
    for i in range (len(objects)):
        l.append(objects[i][1])
    with open(filename, "w") as f:
        f.write("instance Object " + objects[0][0] )
        for i in range(1,len(objects)):
            f.write(", " + objects[i][0])
        f.write(";\n")
        f.write("instance Arm arm0")
        for i in range(1,nbarm):
            f.write(", arm" + str(i) )
        f.write(";\n")
        f.write("instance Location l0_0")
        for i in range(loc[0]):
            for j in range(loc[1]):
                if j != 0 or i != 0:
                    f.write(", l" + str(i) + "_" + str(j))
        f.write(";\n")
		
        f.write("instance Position p0")
        for i in range(1,loc[0]):
            f.write(", p" + str(i))		
        f.write(";\n")
		
        for i in range(loc[0]):
            for j in range(loc[1]):
                f.write("pos(l" + str(i) + "_" + str(j) + ") := p" + str(i) + ";\n")
        f.write("\n")
	
        for i in range(loc[0]):
			for j in range(loc[1]):
				if j+1 < loc[1]:
					f.write("atRight(l" + str(i) + "_" + str(j) + ") := l" + str(i) + "_" + str(j+1) + ";\n")						
		
        f.write("[start] {\n")
        f.write("\trobotPos := p0;\n")
        o = 0
		
        for i in range(loc[0]):
            for j in range(loc[1]):
                if ("l" + str(i) + "_" + str(j)) not in l:
                    f.write("\tl" + str(i) + "_" + str(j) + ".empty := true;\n")	
                else :
                    f.write("\t" + objects[o][1] + ".empty := false;\n")	
                    o+=1
        for i in range(0,len(objects)):	
	        f.write("\t" + objects[i][0] + ".at := " + objects[i][1] + ";\n")
        for i in range(nbarm):
	        f.write("\tarm" + str(i) + ".empty := true;\n")
        f.write("};\n")
        f.write(goal) ##goal
       

if __name__ == "__main__":
	main() 
