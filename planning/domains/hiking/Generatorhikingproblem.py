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
	printProblem()

def printProblem():
	with open("hikingflat.p" + str(loc) + ".pb.anml", "w") as f:
		f.write(problemflat())
	with open("hikinghier.p" + str(loc) + ".pb.anml", "w") as f:
		f.write(problemhier())

def problemflat():
	f=""
	f+="instance Location l0"
	for i in range(1,loc):
		f+=", l" + str(i)	
	f+=";\n"
	f+="instance Car c0"
	for i in range(1,car):
		f+=", c" + str(i)	
	f+=";\n"
	f+="instance Hiker h0"
	for i in range(1,hiker):
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

def problemhier():
	f=""
	f+="instance Place loc0"
	for i in range(1,loc):
		f+=", loc" + str(i)	
	f+=";\n"
	f+="instance Car car0"
	for i in range(1,car):
		f+=", car" + str(i)	
	f+=";\n"
	f+="instance Hiker hik0"
	for i in range(1,hiker):
		f+=", hik" + str(i)	
	f+=";\n"
	f+="[start] {\n\ttent.at := loc0;\n"
	for i in range (car-1):
		f+="\tcar" + str(i) + ".at := loc0;\n"
	f+="\tcar" + str(car-1) + ".at := loc1;\n"
	for i in range (hiker):
		f+="\thik" + str(i) + ".at := loc0;\n"
		f+="\thik" + str(i) + ".canWalk := true;\n"
	f+="};\n"
	f+="[all] contains {\n"
	for i in range(loc-2):
		if (i%2 == 0):
			f+="\to" + str(i) + " : oneStep(tent,loc" + str(i) + ",loc" + str(i+1) + ",loc" + str(i+2) +",hik0,hik1,car0,car1);\n"
		else:
			f+="\to" + str(i) + " : oneStep(tent,loc" + str(i) + ",loc" + str(i+1) + ",loc" + str(i+2) +",hik0,hik1,car1,car0);\n"
	f+="};\n"
	for i in range (loc-3):
		f+="end(o" + str(i) + ") = start(o" + str(i+1) + ");\n" 
	return f
	
if __name__ == "__main__":
    main()
