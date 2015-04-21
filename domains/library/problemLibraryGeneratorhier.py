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
newShelf = 0

def main():
    global newShelf
    if len(sys.argv) > 1:
        loc[0] = int(sys.argv[1])
    if len(sys.argv) > 2:
        loc[1] = int(sys.argv[2])
    if len(sys.argv) > 3:
        newShelf = int(sys.argv[2])
    goal = init()
    if newShelf :
        printProblem("libraryhier.p" + str(loc[0]) + "_" + str(loc[1]) + "newShelf.pb.anml",goal)
    else :
        printProblem("libraryhier.p" + str(loc[0]) + "_" + str(loc[1]) + "sort.pb.anml",goal)

def init():
    lr =[]
    j =0
    for i in range( loc[0]):
        for j in range(loc[1]):
            r = random.randrange(loc[0])
            n = random.randrange(loc[1])
            while (([r,n]) in lr ):
                r = random.randrange(loc[0])
                n = random.randrange(loc[1])
            lr.append([r,n])
            objects.append(["book" + str(i) + "_" + str(j),"l" + str(r) + "_" + str(n) ])
    g="[all] contains sort();\n"
    if newShelf :
        g+="action newShelf(Location l00"
        for i in range(loc[0]):
            for j in range(loc[1]):
                if not (i == 0 and j == 0):
                    g+=", Location l" + str(i) + str(j) 

        for i in range(loc[0]):
            for j in range(loc[1]):
                g+=", Location le" + str(i) + str(j) 
        g+="){\n"
    
        for i in range(loc[0]):
            for j in range(loc[1]):
                for b in range(j,loc[1]):
                    if not (j == b):
                        g+="\tl" + str(i) + str(j) + " != l" +str(i) + str(b) +";\n"

        for i in range(loc[0]):
            for j in range(loc[1]):
                for b in range(j,loc[1]):
                    if not (j == b):
                        g+="\tle" + str(i) + str(j) + " != le" +str(i) + str(b) +";\n"
 
        for i in range(loc[0]):
            for j in range(loc[1]):
                lr.append([i,j])
                for a in range(loc[0]):
                    for b in range(loc[1]):
                        if ([a,b] not in lr):
                            g+="\tl" + str(i) + str(j) + " != l" +str(a) + str(b) +";\n"
        for i in range(loc[0]):
            g+="\tconstant Position p" + str(i) + ";\n"
            for j in range(loc[1]):
                g+="\tpos(l" + str(i) + str(j) + ") == p" + str(i) + ";\n"
        for i in range(loc[0]):
            g+="\tconstant Position pn" + str(i) + ";\n"
            for j in range(loc[1]):
                g+="\tpos(le" + str(i) + str(j) + ") == pn" + str(i) + ";\n"
        for i in range(loc[0]):
            for j in range(loc[1]-1):
                g+="\tatRightl(l" + str(i) + str(j) +", l" + str(i) + str(j+1) +") == true;\n"

        for i in range(loc[0]):
            for j in range(loc[1]-1):
                g+="\tatRightl(le" + str(i) + str(j) +", le" + str(i) + str(j+1) +") == true;\n"
        for i in range(loc[1]):
            g+="\t[start]le0" + str(i) + ".empty == true;\n"
        for i in range(loc[1]):
            g+="\t[start]l0" + str(i) + ".empty == false;\n"
        g+="\t[all] contains {\n"
        for i in range(loc[1]):
            g+="\t\ts" + str(i) + " : sort1_" + str(loc[1]) + "(l0" + str(i) 
            for j in range(loc[1]):
                g+=", le0" + str(j) 
            g+=", p0, pn0"
            g+=");\n"
        g+="\t};\n"
        for i in range(loc[1]-1):
            g+="\tend(s" + str(i) + ") < start(s" + str(i+1) + ");\n"
        g+="};\n"
        l = loc[1]
        while ( l > 1) :
            g+="action sort1_" + str(l) + "(Location lb"
            for i in range(l):
                g+=", Location l" + str(i) 
            g+=", Position p0, Position pn0){\n"
            g+="\tmotivated;\n"    
            g+="\tconstant Object b;\n"
            g+="\t[start] b.at == lb;\n"
            g+="\t:decomposition {\n"
            g+="\t\tl0.empty == true;\n"
            g+="\t\t[all] contains moveToEmpty(b,lb,l0,p0,pn0);\n"
            g+="\t};\n"#end decomposition

            g+="\t:decomposition {\n"
            g+="\t\tl0.empty == false;\n"
            g+="\t\tconstant Object b0;\n"
            g+="\t\t[start]b0.at == l0;\n"
            g+="\t\t[all] contains helpSort1_" + str(l) + "(b,b0,lb"
            for i in range(l):
                g+=",l" + str(i)
            g+=",p0,pn0);\n"
            g+="\t};\n"#end decomposition

            g+="};\n"#end action

            g+="action helpSort1_" + str(l) + "(Object b, Object b0, Location lb"
            for i in range(l):
                g+=", Location l" + str(i)
            g+=", Position p0, Position pn0){\n"
            g+="\tmotivated;\n"
            g+="\t:decomposition{\n"
            g+="\t\tatRightb(b, b0)==false;\n"
            if l-1 != 1 :
                g+="\t\t[all] contains sort1_" + str(l-1) + "(lb" 
                for i in range(1,l):
                    g+=", l" + str(i)
                g+=",p0,pn0);\n"
            else :
                g+="\t\t[all] contains moveToEmpty(b,lb,l1,p0,pn0);\n" 
            g+="\t};\n"
            g+="\t:decomposition{\n"
            g+="\t\tatRightb(b, b0)==true;\n"
            g+="\t\tconstant Arm a;\n"
            g+="\t\t[all] a.empty == true;\n"
            g+="\t\t[all] contains {\n"
            g+="\t\t\tpi : pick(b0,l0,a,pn0);\n"
            g+="\t\t\tm : moveToEmpty(b,lb,l0,p0,pn0);\n"
            if l-1 !=1:
                g+="\t\t\ts : sort1_" + str(l-1) + "(a" 
                for i in range(1,l):
                    g+=", l" + str(i)
                g+=",robot,pn0);\n"
            else :
                g+="\t\ts : moveToEmpty(b,a,l1,p0,pn0);\n"
            g+="\t\t};\n"
            g+="\t\tend(pi) < start(m);\n"
            g+="\t\tend(pi) < start(s);\n"
            g+="\t};\n"
            g+="};\n"
            l-=1
    else :
        g +="action sortPair(){\n"
        i = 0
        g+="\t[all] contains {\n"
        while i < (loc[1]-1):
            g +="\t\tsort2(l0_" + str(i) + ",l0_" + str(i+1) + ");\n"
            i+=2
        g+="\t};\n"
        g+="};\n"
        g +="action sortOdd(){\n"
        i = 1
        g+="\t[all] contains {\n"
        while i < (loc[1]-1):
            g +="\t\tsort2(l0_" + str(i) + ",l0_" + str(i+1) + ");\n"		
            i+=2
        g+="\t};\n"
        g+="};\n"

    g+="action sort(){\n"
    g+="\t:decomposition{\n"
    if newShelf :
        g+="\t\ttrue ==false;\n/*\n"
    g +="\t\t[all] contains {\n"
    for i in range(loc[1]):
        if i%2 == 0:
            g+="\t\t\ts" + str(i) + " : sortPair();\n"
        else:
            g+="\t\t\ts" + str(i) + " : sortOdd();\n"
    g+="\t\t};\n"
    for i in range(loc[1]-1):
        if i%2 == 0:
            g+="\t\tend(s" + str(i) + ") < start(s" + str(i+1) + ");\n"
        else: 
            g+="\t\tend(s" + str(i) + ") < start(s" + str(i+1) + ");\n"
    if newShelf:
        g+="*/\n"
    g+="\t};\n"
    g+="\t:decomposition{\n"
    if not newShelf :
        g+="\t\ttrue == false;\n/*\n"
    for i in range(loc[1]*2):
        g+="\t\tconstant Location l" + str(i) +";\n"
    g+="\t\t[all]contains newShelf(l0"
    for i in range(1,loc[1]*2):
            g+=", l" + str(i)  
    g+=");\n"
    if not newShelf :
        g+="*/\n"
    g+="\t};\n"
    g+="};\n" 	
    return g



def printProblem(filename,goal):
    l = []
    for i in range (len(objects)):
        l.append(objects[i][1])
    with open(filename, "w") as f:
        f.write(goal) ##goal
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
        if newShelf :
            f.write("instance Location le0_0")
            for i in range(loc[0]):
                for j in range(loc[1]):
                    if j != 0 or i != 0:
                        f.write(", le" + str(i) + "_" + str(j))
            f.write(";\n")
        f.write("instance Position p0")
        for i in range(1,loc[0]):
            f.write(", p" + str(i))		
        f.write(";\n")

        if newShelf:
            f.write("instance Position pe0")
            for i in range(1,loc[0]):
                f.write(", pe" + str(i))		
            f.write(";\n")
        for i in range(loc[0]):
            for j in range(loc[1]):
                f.write("pos(l" + str(i) + "_" + str(j) + ") := p" + str(i) + ";\n")
        for i in range(nbarm):
            f.write("pos(arm" + str(i) + ") := robot;\n")
        if newShelf:
            for i in range(loc[0]):
                for j in range(loc[1]):
                    f.write("pos(le" + str(i) + "_" + str(j) + ") := pe" + str(i) + ";\n")
        f.write("\n")
	
        
        for i in range(loc[0]):
			for j in range(loc[1]):
				for b in range(loc[1]):
					if b < j:
						f.write("atRightl(l" + str(i) + "_" + str(j) + ", l" + str(i) + "_" + str(b) + ") := false;\n")		
					if b > j:
						f.write("atRightl(l" + str(i) + "_" + str(j) + ", l" + str(i) + "_" + str(b) + ") := true;\n")
        if newShelf :
			for i in range(loc[0]):
				for j in range(loc[1]):
					for b in range(loc[1]):
						if b < j:
							f.write("atRightl(le" + str(i) + "_" + str(j) + ", le" + str(i) + "_" + str(b) + ") := false;\n")		
						if b > j:
							f.write("atRightl(le" + str(i) + "_" + str(j) + ", le" + str(i) + "_" + str(b) + ") := true;\n")

        """
		for i in range(loc[0]):
            for j in range(loc[1]):
				if j+1 < loc[1]:
					f.write("atRightl(l" + str(i) + "_" + str(j) + ") := l" + str(i) + "_" + str(j+1) + ";\n")
				else :
					if i+1 < loc[0]:
						f.write("atRightl(l" + str(i) + "_" + str(j) + ") := l" + str(i+1) + "_" + str(0) + ";\n")
					else:
						f.write("atRightl(l" + str(i) + "_" + str(j) + ") := outOf;\n")
		"""
        for i in range(loc[0]):
            for j in range(loc[1]):
				for b in range(loc[0]):
					for c in range(loc[1]):
						if i < b:
							f.write("atRightb(book" + str(i) + "_" + str(j) + ", book" + str(b) + "_" + str(c) + ") := true;\n")		
						if (i > b):
							f.write("atRightb(book" + str(i) + "_" + str(j) + ", book" + str(b) + "_" + str(c) + ") := false;\n")
						if i == b :
							if j < c :
								f.write("atRightb(book" + str(i) + "_" + str(j) + ", book" + str(b) + "_" + str(c) + ") := true;\n")	
							if j > c:
								f.write("atRightb(book" + str(i) + "_" + str(j) + ", book" + str(b) + "_" + str(c) + ") := false;\n")

        for i in range(loc[0]):
			for j in range(loc[0]):
				if i < j:
					f.write("atRightp(p" + str(i) + ", p" + str(j) + ") := false;\n")
				if i > j:
					f.write("atRightp(p" + str(i) + ", p" + str(j) + ") := true;\n")

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
        if newShelf :
            for i in range(loc[0]):
                for j in range(loc[1]):
                    f.write("\tle" + str(i) + "_" + str(j) + ".empty := true;\n")	
        for i in range(0,len(objects)):	
	        f.write("\t" + objects[i][0] + ".at := " + objects[i][1] + ";\n")
        for i in range(nbarm):
	        f.write("\tarm" + str(i) + ".empty := true;\n")
        f.write("};\n")
       

if __name__ == "__main__":
	main() 
