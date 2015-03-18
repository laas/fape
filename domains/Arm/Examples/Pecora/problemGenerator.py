from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

nbarm = 1
loc = [3,3]
objects = [["fork","l0_0"],["knife","l0_1"],["cup","l0_2"]]

def init():
	g="""
constant Location l01;
constant Location l11;
[end] fork.at == l01;
[end] cup.at == l11;
atRight(l01) == l11;
constant Location l02;
constant Location l12;
[end] cup.at == l02;
[end] knife.at == l12;
atRight(l02) == l12;"""
	return g

def main():
	global books
	global loc
	if len(sys.argv) > 1:
		books = int(sys.argv[1])
		loc = [1,int(sys.argv[1])]
	goal = init()
	#printProblem("pecora." + str((loc[0]*loc[1])+1) + ".pb.anml")
	printProblem("pecora." + str(000) + ".pb.anml",goal)

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
				f.write(", l" + str(i) + "_" + str(j))
		f.write(";\n")

		
		for i in range(loc[0]):
			for j in range(loc[1]):
				if j+1 < loc[1]:
					f.write("atRight(l" + str(i) + "_" + str(j) + ") := l" + str(i) + "_" + str(j+1) + ";\n")						
		
		f.write("[start] {\n")
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
