from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

durationA = str(5)
durationB = str(4)
durationC = str(1)

def main():
    
    if len(sys.argv) > 1:
        nbDepth = int(sys.argv[1])
        if nbDepth < 2 :
           nbDepth =2
    else :
       nbDepth =2
    mainFolder = "depth"    
    if not os.path.exists(mainFolder):
        subprocess.call(["mkdir", mainFolder])
        

    generateDomain("depth", nbDepth)
    
    
    #print "Every file has been written. Exiting"

def generateDomain(folderName, nbDepth):

    domainFilename = folderName + "/" + folderName + "-flat" + str(nbDepth) + ".dom.anml"
    printDomainToFile(domainFilename, nbDepth)
    domainFilename = folderName + "/" + folderName + "-hier" + str(nbDepth) + ".dom.anml"
    printDomainHierToFile(domainFilename, nbDepth)

def printDomainToFile(domainFilename, nbDepth):
    with open(domainFilename, "w") as f:
        for i in range(0, nbDepth):
            f.write("predicate a" + str(i+1) +"();\n")
            f.write("predicate b" + str(i+1) +"();\n")
            f.write("predicate c" + str(i+1) +"();\n")
            f.write("predicate d" + str(i+1) +"();\n")
            f.write("predicate e" + str(i+1) +"();\n")

            f.write("\naction An" + str(i+1) + " () {\n")
            f.write("\tduration := " + durationA + ";\n")
            if i > 0:
                f.write("\t[start] {\n")
                f.write("\t\tb"+ str(i) +" == true;\n")
                f.write("\t\td"+ str(i) +" == true;\n")
                f.write("\t\te"+ str(i) +" == true;\n")
                f.write("\t};\n")
            f.write("\t[start] a" + str(i+1) + " := true;\n")
            f.write("\t[end] {\n")
            f.write("\t\ta" + str(i+1) + " := false;\n")
            f.write("\t\tb" + str(i+1) + " := true;\n")
            f.write("\t\td" + str(i+1) + " := false;\n")
            f.write("\t};\n")
            f.write("};\n")

            f.write("\naction Bn" + str(i+1) + " () {\n")
            f.write("\tduration := " + durationB + ";\n")
            f.write("\t[start] a" + str(i+1) + " == true;\n")
            f.write("\t[start] c" + str(i+1) + " := true;\n")
            f.write("\t[end] {\n")
            f.write("\t\tc" + str(i+1) + " := false;\n")
            f.write("\t\td" + str(i+1) + " := true;\n")
            f.write("\t};\n")
            f.write("};\n")

            f.write("\naction Cn" + str(i+1) + " () {\n")
            f.write("\tduration := " + durationC + ";\n")
            f.write("\t[start] c" + str(i+1) + " == true;\n")
            f.write("\t[end] {\n")
            f.write("\t\tb" + str(i+1) + " := false;\n")
            f.write("\t\te" + str(i+1) + " := true;\n")
            f.write("\t};\n")
            f.write("};\n")

########################            problem              ###############
        f.write("\n/*******Problem************/\n")
        f.write("[all] contains{\n")
        f.write("\tCn" + str(nbDepth) +"();\n")
        f.write("};")

def printDomainHierToFile(domainFilename, nbDepth):
    with open(domainFilename, "w") as f:
        for i in range(0, nbDepth):
            if i == 0:
                f.write("\naction An" + str(i+1) + " () {\n")
                f.write("\tmotivated;\n")
                f.write("\tduration := " + durationA + ";\n")
                f.write("};\n")  
            else:               
                f.write("\naction An" + str(i+1) + " () {\n")
                f.write("\tmotivated;\n")
                f.write("\tduration := " + durationA + ";\n")
                f.write("\ta : ABC" + str(i) + "();\n")
                f.write("\t end(a) < start;\n")
                f.write("};\n")

            f.write("\naction Bn" + str(i+1) + " () {\n")
            f.write("\tduration := " + durationB + ";\n")
            f.write("\tmotivated;\n")
            f.write("};\n")

            f.write("\naction Cn" + str(i+1) + " () {\n")
            f.write("\tduration := " + durationC + ";\n")
            f.write("\tmotivated;\n")
            f.write("};\n")

            f.write("\naction ABC" + str(i+1) + " () {\n")
            f.write("\t[all] contains {\n")
            f.write("\t\t b" + str(i+1) + " : An" + str(i+1) + "();\n")
            f.write("\t\t d" + str(i+1) + " : Bn" + str(i+1) + "();\n")
            f.write("\t\t e" + str(i+1) + " : Cn" + str(i+1) + "();\n")
            f.write("\t};\n")
            f.write("\tstart(b" + str(i+1) + ") < start(d" + str(i+1) + ");\n")
            f.write("\tend(d" + str(i+1) + ") < end(b" + str(i+1) + ");\n")
            f.write("\tstart(d" + str(i+1) + ") < start(e" + str(i+1) + ");\n")
            f.write("\tend(e" + str(i+1) + ") < end(d" + str(i+1) + ");\n")
            f.write("};\n")
####################      problem        #############
        f.write("\n/*******Problem************/\n")
        f.write("[all] contains{\n")
        f.write("\tCn" + str(nbDepth) +"();\n")
        f.write("};")


if __name__ == "__main__":
    main()
