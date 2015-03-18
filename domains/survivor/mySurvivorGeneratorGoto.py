from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

hospital = ["l3_3"]
survivor = ["l2_1","l3_0"]
robot = 2
robotLoc =["l0_3","l0_3"]
loc = [4,4] #[height,width]

def main():
    mainFolder = "mySurvivor"    
    if not os.path.exists(mainFolder):
        subprocess.call(["mkdir", mainFolder])
        
    filename = "mySurvivor"
    printDomain(mainFolder + "/" + filename + ".dom.anml")
    printProblem(mainFolder + "/" + filename + "." + str(len(survivor)) + ".pb.anml")
    
    
    #print "Every file has been written. Exiting"

def printDomain(filename):
    with open(filename, "w") as f:
        f.write("type Robot with {\n")
	f.write("\tvariable Loc at;\n")
	f.write("};\n")
        f.write("type Survivor with{\n")
	f.write("\tvariable Loc at;\n")
	f.write("\tvariable boolean stabilized;\n")
	f.write("\tvariable boolean hospitalized;\n")
        f.write("};\n")
        f.write("type Loc with {\n")
        f.write("\tvariable boolean explored;\n")
        f.write("};\n")

        f.write("constant boolean hospital(Loc l);\n")

        f.write("\naction goto(Robot r, Loc from, Loc to) {\n")
        f.write("\tfrom != to;\n")
        f.write("\t[all] r.at == from :-> to;\n")
        f.write("};\n")

        f.write("action explore(Robot r, Loc l){\n")
        f.write("\tduration := 1;\n")
        f.write("\t[all] r.at == l;\n")
        f.write("\t[end] l.explored := true;\n")
        f.write("};\n")

        
        f.write("action stabilize (Robot r, Survivor s, Loc l){\n")
        f.write("\tduration := 1;\n")
	f.write("\tmotivated;\n")
        f.write("\t[all] r.at == l;\n")
        f.write("\t[all] s.at == l;\n")
        f.write("\t[end] s.stabilized := true;\n")
        f.write("};\n")

        f.write("action hospitalize (Survivor s,Loc l){\n")
        f.write("\tduration := 1;\n")
	f.write("\tmotivated;\n")
        f.write("\t[all] s.at == l;\n")
        f.write("\t[all] s.stabilized == true;\n")
        f.write("\thospital(l) == true;\n")
        f.write("\t[end] s.hospitalized := true;\n")
        f.write("};\n")

        f.write("action moveSurvivor (Robot r1, Robot r2, Survivor s, Loc from, Loc to){\n")
        f.write("\tr1!=r2;\n")
        f.write("\t[all] {\n")
        f.write("\t\ts.stabilized == true;\n")
        f.write("\t\ts.at == from :-> to;\n")
        f.write("\t};\n")#end all
        f.write("\t[all]  m1 : goto(r1, from, to);\n")  
        f.write("\t[all]  m2 : goto(r2, from, to);\n") 
        f.write("\tstart(m1) = start(m2);\n") 
        f.write("};\n")#end action

        f.write("action treatSurvivor (Robot r1, Robot r2, Survivor s, Loc ls, Loc lh){\n")
        f.write("\t[all] contains {\n") 
        f.write("\t\tstab : stabilize (r0 , s, ls);\n")
        f.write("\t\thos : hospitalize(s, lh );\n")
        f.write("\t};")
        f.write("\tstart = start(stab);\n")             
        f.write("\tend(stab) < start(hos);\n")           
        f.write("\tend = end(hos);\n")    
        f.write("};\n")#end action

        
def printProblem(filename):
    with open(filename, "w") as f:
       f.write("instance Robot r0")
       for i in range(1,robot):
            f.write(", r" + str(i) )
       f.write(";\n")
       f.write("instance Survivor s0")
       for i in range(1,len(survivor)):
            f.write(", s" + str(i) )
       f.write(";\n")
       f.write("instance Loc l0_0")
       for i in range(loc[0]):
             for j in range(loc[1]):
                 if (i == 0 and j ==0):
                     continue;
                 f.write(", l" + str(i) + "_" + str(j) )
       f.write(";\n")


       for i in hospital:
           f.write("hospital(" + i +") := true;\n")
      

       f.write("[start] {\n")
       for i in range(robot):
           f.write("\tr" + str(i) + ".at := " +  robotLoc[i]+ ";\n")
       for i in range(len(survivor)):
           f.write("\ts" + str(i) + ".at := " + survivor[i] + ";\n")
       
       f.write("};\n")

       f.write("[end] {\n")
       f.write("\ts0.hospitalized == true;\n")
       f.write("};\n")








if __name__ == "__main__":
    main()
