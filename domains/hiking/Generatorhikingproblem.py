from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

loc = 3
hiker =2
car = 2

def main():
	global loc
	global hiker
	global car
	if len(sys.argv) > 1:
		loc = int(sys.argv[1])
	if len(sys.argv) > 2:
		hiker = int(sys.argv[2])
	if len(sys.argv) > 3:
		car = int(sys.argv[3])
	assert (loc > 1) ,"not enough locations"
	printProblem("hikingflat.p" + str(loc) + ".pb.anml")

def printProblem(filename):
	with open(filename, "w") as f:
		f.write(problem())

def problem():
	f=""
	f+="instance Location l0"
	for i in range(1,loc):
		f+=", l" + str(i)	
	f+=";\n"
	f+="instance Car c0"
	for i in range(1,loc):
		f+=", c" + str(i)	
	f+=";\n"
	f+="instance Hiker h0"
	for i in range(1,loc):
		f+=", h" + str(i)	
	f+=";\n"
	f+="[all] contains {\n"
	for l in range(loc-1):
		for h in range (hiker):
			f+="\tw" + str(l) + "h" + str(h) + " : walk(h" + str(h) + ",l" + str(l) + ",l" + str(l+1) + ");\n"
			f+="\ts" + str(l) + "h" + str(h) + " : sleep(h" + str(h) + ",tent,l" + str(l+1) + ");\n"
	f+="};\n"
	for l in range(loc-1):
		for h in range (1,hiker):
			f+="start(w" + str(l) + "h0) = start(w" + str(l) + "h" + str(h) + ");\n"
	for l in range(1,loc-1):
		f+="end(w" + str(l-1) + "h0) < start(w" + str(l) + "h0);\n"
	f+="[start] {\n\ttent.at := l0;\n"
	for i in range (car):
		f+="\tc" + str(i) + ".at := l0;\n"
	for i in range (hiker):
		f+="\th" + str(i) + ".at := l0;\n"
		f+="\th" + str(i) + ".canWalk := true;\n"
	f+="};\n"
	return f
	
if __name__ == "__main__":
    main()
