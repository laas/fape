#! /usr/bin/env python

from __future__ import division

import itertools
import json
import math
import os
import random
import shutil
import subprocess
import sys

repairKeysDefault = [
                     "survivorPos", 
                     "startingPos", 
                     "newSurvivor",
                     #(2,"removeSurvivor")
                     ]
#repairKeysDefault = ["survivorPos", "startingPos"]

def main():
    
    mainFolder = os.getcwd()
    if len(sys.argv) > 1:
        mainFolder = sys.argv[1]
        
    if os.access(mainFolder, os.R_OK):
        print("Folder %s already exists. Do you want to erase it ? Press enter to continue" % mainFolder)
        raw_input()
        shutil.rmtree(mainFolder)
        
    os.mkdir(mainFolder)
    os.chdir(mainFolder)
        
    #create the database that will be populated with all created tests
    db = []

    #generateDomain("survivors", db)
    #generateDomain("survivors-randomHos", db, nbrHopital=3, randomHospital=True)
    #generateDomain("survivors-explore", db, goalExplore=True, nbrHopital=3, randomHospital=True)
    generateDomain("survivors", db, goalExplore=True, nbrHopital=2, randomHospital=True, repairKeys=repairKeysDefault)
    
    
    #generateDomain("survivors-repair-removeS", db, goalExplore=True, nbrHopital=2, randomHospital=True, 
    #               repairKeys=[(1,"removeSurvivor"), (2,"removeSurvivor")])
     
    #generateDomain("survivors-repair-posS", db, goalExplore=True, nbrHopital=2, randomHospital=True, 
    #               repairKeys=[(1,"survivorPos"), (2,"survivorPos")])
     
    #generateDomain("survivors-repair-startingPos", db, goalExplore=True, nbrHopital=2, randomHospital=True, 
    #               repairKeys=[(1,"startingPos"), (2,"startingPos")])
     
    #generateDomain("survivors-repair-newS", db, goalExplore=True, nbrHopital=2, randomHospital=True, 
    #               repairKeys=[(1,"newSurvivor"), (2,"newSurvivor")])
    
    #util.writeProblemsDatabase(db)
    
    print "Every file has been written. Exiting"

def generateDomain(folderName, db, nbrHopital=1, goalExplore=False, randomHospital=False, repairKeys = None):

    ######  User-defined parameter for the generation  #####
    teamNumer = [1]
    robotNumber = [2] # per team. 2 is an absolute minimum to move survivors
    zoneXNumber = [2]
    zoneYNumber = [2]
    sizeZoneGrid = [2] #grid size (always a square)
    survivorNumber = [2]
    hopitalNumber = range(2, nbrHopital+1)
    repetitionNumber = range(1)

    bzipFiles = False
    ######  No Modification after here  #####

    if(not os.access(folderName, os.W_OK)):
        os.mkdir(folderName)
# 
#     for f in os.listdir(folderName):
#         os.remove(os.path.join(folderName, f))

    numProb = 1

    domainFilename = folderName + "/" + folderName + ".dom.anml"
    domainHierFilename = folderName + "/" + folderName + "-hier.dom.anml"
    printDomainToFile(domainFilename, bzipFiles)
    printDomainHierToFile(domainHierFilename, bzipFiles)
    print("Domain has been written")
    
    useRepair = True
    if repairKeys is None:
        useRepair = False
        repairKeys = [""]

    parameters = itertools.product(teamNumer,\
                                   robotNumber,\
                                   zoneXNumber,\
                                   zoneYNumber,\
                                   sizeZoneGrid,\
                                   survivorNumber,\
                                   hopitalNumber,\
                                   repetitionNumber, \
                                   repairKeys)

    prbNumber =  len(teamNumer) \
               * len(robotNumber) \
               * len(zoneXNumber) \
               * len(zoneYNumber) \
               * len(sizeZoneGrid) \
               * len(survivorNumber)\
               * len(hopitalNumber)\
               * len(repetitionNumber)\
               * len(repairKeys)

    print("There is %d problems to create" % prbNumber)
    
    for teamNum, robotNum, zoneXNum, zoneYNum, sizeZone, survivorNum, hospitalNum, repetitionNum, repairKey in parameters:
        
        #Prevent a reparing that would remove every element in a plan
        if(type(repairKey) == tuple and repairKey[1] == "startingPos" and teamNum < repairKey[0]):
            continue

        if(type(repairKey) == tuple and repairKey[1] == "removeSurvivor" and survivorNum < repairKey[0]):
            continue

        if(type(repairKey) == tuple and repairKey[1] == "survivorPos" and survivorNum < repairKey[0]):
            continue

        print("Starting creating problem %d, %d%% done" % (numProb, (numProb*100)/prbNumber))
        
        data = {"domainName" : folderName, 
                "domainFile" : domainFilename,
                "prbTeamNum" : teamNum,
                "prbRobotNum" : robotNum,
                "prbZoneXNum" : zoneXNum,
                "prbZoneYNum" : zoneYNum,
                "prbSizeZone" : sizeZone,
                "prbSurvivorNum" : survivorNum,
                "prbHospitalNum" : hospitalNum,
                "prbIndex" : repetitionNum}
        
        #domainName = "domain_p" + str(numProb).zfill(3)
        problemName = str(numProb).zfill(3)
        numProb += 1
        data["prbName"] = problemName
        
        p = problem(problemName, teamNum, robotNum, zoneXNum, zoneYNum, [sizeZone,sizeZone], survivorNum, hospitalNum, goalExplore, randomHospital)

        prbFilename = folderName + "/" + folderName + "." + problemName + ".pb.anml"
        data["prbFile"] = prbFilename
        p.printProblemToFile(prbFilename, bzipFiles)

        helperFilename = folderName + "/" + folderName + "-hier." + problemName + ".pb.anml"
        data["helperFile"] = helperFilename
        p.printProblemToFile(helperFilename, bzipFiles)
        p.printHelperToFile(helperFilename, bzipFiles)
        
        if(useRepair):
            
            data["repair"] = True
            data["repairKey"] = repairKey
            
            p.changeProblemToRepair(repairKey)
            
            filename = folderName + "/" + folderName + "." + problemName + "_broken.pb.anml"
            data["repairPrbBrokenFile"] = filename
            p.printProblemToFile(filename, bzipFiles)

            filename = folderName + "/" + folderName + "-hier." + problemName + "_broken.pb.pddl"
            data["repairPrbHelperFile"] = filename
            p.printHelperToFile(filename, bzipFiles)

        # hierarchical problem
        db.append(data)
        
        
    print("*** Finished creating %s, %d/%d problems ***" % (folderName, numProb-1, prbNumber))

def printDomainToFile(filename, useBzip = True):
    with open(filename, "w") as f:
        f.write("""
type Robot with{
	variable Loc at;
	variable Team team;
};
type Survivor with{
	variable Loc at;
	variable boolean stabilized;
	variable boolean hospitalized;
};
type Loc with {
	variable boolean explored;
};
type Zone;
type Team with{
	variable Zone zone;
};

constant boolean hospital(Loc l);
constant boolean belong(Loc l, Zone z);
constant boolean adjacent (Loc l1, Loc l2);
constant integer distance (Loc l1, Loc l2);
constant integer distanceZone (Zone z1, Zone z2);

action move(Robot r, Loc from, Loc to) {
	duration := distance(from,to);
	from != to;
	[all] r.at == from :-> to;
};

action explore(Robot r, Loc l){
	[all] r.at == l;
	[end] l.explored := true;
};

action moveTeam(Team t, Zone from, Zone to){
	duration := distanceZone(from,to);
	from != to;
	[all]t.zone == from :-> to;
};

action stabilize (Robot r, Survivor s,Loc l){
	[all] r.at == l;
	[all] s.at == l;
	[end] s.stabilized := true;
};

action hospitalize (Survivor s,Loc l){
	[all] s.at == l;
	[all] s.stabilized == true;
	hospital(l) == true;
	[end] s.hospitalized := true;
};

action moveSurvivor (Robot r1, Robot r2, Survivor s, Loc from, Loc to){
	duration := distance(from,to);
	from != to;
	r1 != r2;
	[all] {
		s.stabilized == true;
		r1.at == from :-> to;
		r2.at == from :-> to;
		s.at == from :-> to;
	};
};
""")

        pass #done with the domain
    if(useBzip):
        subprocess.call(["bzip2", filename])

def printDomainHierToFile(filename, useBzip = True):
    with open(filename, "w") as f:
        f.write("""
type Robot with{
	variable Loc at;
	variable Team team;
};
type Survivor with{
	variable Loc at;
	variable boolean stabilized;
	variable boolean hospitalized;
};
type Loc with {
	variable boolean explored;
};
type Zone;
type Team with{
	variable Zone zone;
};

constant boolean hospital(Loc l);
constant boolean belong(Loc l, Zone z);
constant boolean adjacent (Loc l1, Loc l2);
constant integer distance (Loc l1, Loc l2);
constant integer distanceZone (Zone z1, Zone z2);

action move(Robot r, Loc from, Loc to) {
	motivated;
	duration := distance(from,to);
	from != to;
	[all] r.at == from :-> to;
};

action explore(Robot r, Loc l){
	motivated;
	[all] r.at == l;
	[end] l.explored := true;
};

action moveTeam(Team t, Zone from, Zone to){
	motivated;
	duration := distanceZone(from,to);
	from != to;
	[all]t.zone == from :-> to;
};

action stabilize (Robot r, Survivor s,Loc l){
	motivated;
	[all] r.at == l;
	[all] s.at == l;
	[end] s.stabilized := true;
};

action hospitalize (Survivor s,Loc l){
	motivated;
	[all] s.at == l;
	[all] s.stabilized == true;
	hospital(l) == true;
	[end] s.hospitalized := true;
};

action moveSurvivor (Robot r1, Robot r2, Survivor s, Loc from, Loc to){
	motivated;
	duration := distance(from,to);
	from != to;
	r1 != r2;
	[all] {
		s.stabilized == true;
		r1.at == from :-> to;
		r2.at == from :-> to;
		s.at == from :-> to;
	};
};
""")

        pass #done with the domain
    if(useBzip):
        subprocess.call(["bzip2", filename])

class problem:
    def __init__(self, problemName, nbrTeam, nbrRobot, nbrXZone, nbrYZone, sizeGrid, nbrSurvivor, nbrHospital, goalExplore, randomHospital):
        self.problemName = problemName
        self.nbrTeam = nbrTeam
        self.nbrRobot = nbrRobot
        self.nbrXZone = nbrXZone
        self.nbrYZone = nbrYZone
        self.sizeGrid = sizeGrid
        self.nbrSurvivor = nbrSurvivor
        self.goalExplore = goalExplore
        self.randomHospital = randomHospital
        
        self.hospitals = []
        if not randomHospital:
            self.hospitals = [(0,0)]
        else:
            self.hospitals = random.sample(self.getLocList(), nbrHospital)
                
        #initial position of each survivor, indexed by its index
        self.survivorPos = {}
        for s in self.getSurvivorList():
            self.survivorPos[s] = self.getRandomLoc()
            
        #initial position of each team, indexed by its index
        self.startPos = {}
        self.startZone = {}
        for t in self.getTeamList():
            pos = (0,0)
            zone = self.getZoneOfLoc(pos)
            
            self.startZone[t] = zone
            for robot in self.getRobotsInTeam(t):
                self.startPos[robot] = pos
        
        #self.computeAbstractAction(self.getTeamName(1), self.getZoneName(1))

    def getTeamName(self, team):
        return "team" + str(team)
    
    def getRobotName(self, robot):
        return "r" + str(robot[0]) + "_" + str(robot[1])
    
    def getSurvivorName(self, s):
        return "s" + str(s)

    def getTeamList(self):
        return [team for team in range(1, self.nbrTeam + 1)]
    
    def getTeamListName(self):
        return [self.getTeamName(team) for team in self.getTeamList()]
            
    def getRobotsInTeam(self, team):
        return [(team, robot) for robot in range(1, self.nbrRobot + 1)]
            
    def getRobotList(self):
        return [(team, robot) for team,robot in itertools.product(range(1, self.nbrTeam + 1), range(1, self.nbrRobot + 1))]
 
    def getRobotListName(self):
        return [self.getRobotName((team, robot)) for team,robot in self.getRobotList()]

    def getSurvivorList(self):
        return [s for s in range(1, self.nbrSurvivor + 1)]

    def getSurvivorListName(self):
        return [self.getSurvivorName(s) for s in self.getSurvivorList()]

    def getZoneName(self, zone):
        return "z" + str(zone[0]) + "_" + str(zone[1])

    def getZoneList(self):
        result = []
        for x in range(0, self.nbrXZone):
            for y in range(0, self.nbrYZone):
                result.append( (x, y) )
        return result
    
    def getLocName(self, loc):
        return "l" + str(loc[0]) + "_" + str(loc[1])

    def getLocList(self):
        result = []
       
        for i in range(0, self.nbrXZone * self.sizeGrid[0]):
            for j in range(0, self.nbrYZone * self.sizeGrid[1]):
                result.append( (i, j) )
        return result
    
    def getLocsInZone(self, zone):
        #first compute corner
        start = ( zone[0] * self.sizeGrid[0], zone[1] * self.sizeGrid[1] )
        
        result = []
        for i in range(0, self.sizeGrid[0]):
            for j in range(0, self.sizeGrid[1]):
                result.append( (start[0] + i, start[1] + j))
        return result
    
    def getZoneOfLoc(self, loc):
        return ( loc[0] // self.sizeGrid[0], loc[1] // self.sizeGrid[1], )
    
    #each pos is a tuple (x, y)
    def getDistBetweenLoc(self, pos1, pos2):
        distX = pos1[0] - pos2[0]
        distY = pos1[1] - pos2[1]
        
        #manhattan distance
        return abs(distX) + abs(distY)
    
        #euclidian distance
        #return math.sqrt(distX*distX + distY*distY)
    
    #each zone is a tuple (x, y)
    def getDistBetweenZones(self, z1, z2):
        if z1 == z2:
            return 0
        else:
            distX = z1[0] - z2[0]
            distY = z1[1] - z2[1]
            return 1 + round((math.sqrt(distX*distX + distY*distY)-1)*self.sizeGrid[0], 2)
        
    def getRandomLoc(self):
        return random.choice(self.getLocList())
    
    def getHospitalList(self):
        return self.hospitals
    
    #return a list of adjacent node, starting with f and ending with t
    def computePath(self, f, t):
        result = [f]
        
        xDir = t[0] - f[0]
        if(xDir != 0):
            xDir = xDir / abs(xDir) #xDir = 1 if f is left of t, -1 else
        
        while(result[-1][0] != t[0]):
            result.append( (result[-1][0] + xDir, result[-1][1]) )
            
        yDir = t[1] - f[1]
        if(yDir != 0):
            yDir = yDir / abs(yDir) #yDir = 1 if f is left of t, -1 else
        
        while(result[-1][1] != t[1]):
            result.append( (result[-1][0], result[-1][1]  + yDir) )
            
        return result
        
        

    def printProblemToFile(self, filename, useBzip = True):
        with open(filename, "w") as f:
            f.write("instance Robot "+ self.getRobotListName()[0])
            for robot in range(1,len(self.getRobotListName())):
                f.write(", " + self.getRobotListName()[robot])
            f.write(";\n")
            
            if(self.getSurvivorListName()):
                f.write("instance Survivor " + self.getSurvivorListName()[0])
                for s in range(1, len(self.getSurvivorListName())):
                    f.write(", " + self.getSurvivorListName()[s])
                f.write(";\n")

            f.write("instance Team " + self.getTeamListName()[0])
            for team in range(1, len(self.getTeamListName())):
                f.write(", " + self.getTeamListName()[team])
            f.write(";\n")

            f.write("instance Loc " + self.getLocName(self.getLocList()[0]))
            for loc in range(1, len(self.getLocList())):
                f.write(", " + self.getLocName(self.getLocList()[loc]))
            f.write(";\n")

            f.write("instance Zone " + self.getZoneName(self.getZoneList()[0]))
            for z in range(1, len(self.getZoneList())):
                f.write(", " + self.getZoneName(self.getZoneList()[z]))
            f.write(";\n")

            #print init state

            # belong relations
            for zone in self.getZoneList():
                for loc in self.getLocsInZone(zone):
                    f.write("belong(" + self.getLocName(loc) + "," + self.getZoneName(zone) + ") := true;\n")
            f.write("\n")
            f.write("[start] {\n")
            # part-of relations
            for team in self.getTeamList():
                for robot in self.getRobotsInTeam(team):
                    f.write("\t" + self.getRobotName(robot) + ".team := " + self.getTeamName(team) + ";\n")
            f.write("\n")

            #starting position
            for robot in self.getRobotList():
                f.write("\t" + self.getRobotName(robot) + ".at := " + self.getLocName( self.startPos[robot] ) + ";\n")
            f.write("\n")

            #starting team position
            for team in self.getTeamList():
                f.write("\t" + self.getTeamName(team) + ".zone := " + self.getZoneName( self.startZone[team] ) + "; ")
            f.write("\n")
            
            #starting survivor position
            for s in self.getSurvivorList():
                f.write("\t" + self.getSurvivorName(s) + ".at := " + self.getLocName(self.survivorPos[s]) + ";\n ")
            f.write("\n")
            f.write("};\n")
            
            #hospital position
            for loc in self.getHospitalList():
                f.write("hospital(" + self.getLocName(loc) + ") := true;\n ")
            f.write("\n")
            """
            #adjacency
            for loc in self.getLocList():
                for move  in [[0,1], [0,-1], [1,0], [-1,0]]:
                    neighboor = [loc[0] + move[0], loc[1] + move[1]]
                    if neighboor[0] < 0 or neighboor[0] >= self.nbrXZone*self.sizeGrid[0] or neighboor[1] < 0 or neighboor[1] >= self.nbrYZone*self.sizeGrid[0]:
                        pass
                    else:
                        dist = self.getDistBetweenLoc(loc, neighboor)
                        f.write("\t\t(adjacent " + self.getLocName(loc) + " " + self.getLocName(neighboor) + ")\n")
                        f.write("\t\t(= (distance " + self.getLocName(loc) + " " + self.getLocName(neighboor) + ") " + str(dist) + ")\n")
            f.write("\n\n")
            """
            
            
            #print distances
            for pos1 in self.getLocList():
                for pos2 in self.getLocList():
                    #if(pos1 == pos2):
                    #    continue
                    locFrom = self.getLocName(pos1)
                    locTo = self.getLocName(pos2)
                    dist = self.getDistBetweenLoc(pos1, pos2)
                    dist = float(int(dist*1000))/1000
                    f.write("distance (" + locFrom + "," + locTo + ") := " + str(dist) + ";\n")
            
            
            for z1 in self.getZoneList():
                for z2 in self.getZoneList():
                    dist = self.getDistBetweenZones(z1, z2)
                    dist = float(int(dist*1000))/1000
                    f.write("distanceZone (" + self.getZoneName(z1) + "," + self.getZoneName(z2) + ") :=" + str(dist) + ";\n")

                    
            f.write("\n")


            #print goal state
            f.write("[end] {\n")

            if self.goalExplore:
                for loc in self.getLocList():
                    f.write("\t" + self.getLocName(loc) + ".explored == true;\n")
                f.write("\n")
            for s in self.getSurvivorList():
                f.write("\t" + self.getSurvivorName(s) + ".hospitalized ==true;\n ")

            f.write("};")

        if(useBzip):
            subprocess.call(["bzip2", filename])

    def printHelperToFile(self, filename, useBzip = True):
        with open(filename, "a") as f:
            # A team treat a survivor

            f.write("\n")
            for hospital in self.getHospitalList():
                hospitalName = self.getLocName(hospital)
                hospitalZone = self.getZoneOfLoc(hospital)
                hospitalZoneName = self.getZoneName(hospitalZone)
                for team in self.getTeamList():
                    teamName = self.getTeamName(team)
                    firstRobot = self.getRobotsInTeam(team)[0]

                    f.write("\n")
                    f.write("action treatSurvivor" + teamName + hospitalName + "(Survivor s, Loc l, Zone z) {\n")
                    f.write("\t[all] contains {\n")
                    f.write("\t\tstab : stabilize (" + self.getRobotName(firstRobot) + ", s, l);\n")
                    f.write("\t\tm : moveToHospital" + teamName + "(s, l, " + hospitalName + ", z, " +hospitalZoneName  + ");\n")
                    f.write("\t\thos : hospitalize(s, " + hospitalName + ");\n\t};\n")
                    f.write("\tbelong(l,z);\n") 
                    f.write("\t[start] {\n")
                    f.write("\t\ts.at == l;\n")      
                    f.write("\t\t" + teamName +".zone == z;\n\t};\n")
                    f.write("\tstart = start(stab);\n")             
                    f.write("\tend(stab) < start(m);\n")               
                    f.write("\tend(m) < start(hos);\n") 
                    f.write("\tend = end(hos);\n")
                    f.write("\t[end] " + teamName + ".zone := " + hospitalZoneName + ";\n};")       
            f.write("\n\n")
            for team in self.getTeamList():
                        
                teamName = self.getTeamName(team)
                # A team moves a survivor to an hospital
                f.write("action moveToHospital" + teamName + "(Survivor s, Loc from, Loc to, Zone z1, Zone z2) {\n" )
                f.write("\tduration := distance(from,to);\n")
                f.write("\thospital(to) == true;\n")
                f.write("\tbelong(from,z1) == true;\n")
                f.write("\tbelong(to,z2) == true;\n")
                f.write("\t[start] {\n")
                f.write("\t\ts.at == from ;\n")
                f.write("\t\ts.stabilized == true;\n")
                f.write("\t\t" + teamName + ".zone == z1;\n\t};\n")
                #TODO what to do with team final position ?
                             
                for sameZone in [True, False]:
                
                    robot1,robot2 = self.getRobotsInTeam(team)[0:2]
                    robot1Name = self.getRobotName(robot1)
                    robot2Name = self.getRobotName(robot2)
                    f.write("\t:decomposition { \n") 
                    f.write("\t\t/* move-to-hospital " + str(teamName) + " " + str(sameZone) + "*/\n")
                    f.write("\t\t[all] contains {\n")                          
                    f.write("\t\t\tm : moveSurvivor (" + robot1Name + ", " + robot2Name + ", s, from, to);\n") 
                    if not sameZone:
                        f.write("\t\t\tmteam : moveTeam(" + str(teamName) + ", z1, z2);\n")
                    f.write("\t\t};\n")
                    f.write("\t\tfrom != to;\n")
                    f.write("\t\t" + robot1Name  + ".at == from;\n")
                    f.write("\t\t" + robot2Name  + ".at == from;\n")
                    if sameZone:
                        f.write("\t\tz1 == z2;\n")
                    else:
                        f.write("\t\tz1 != z2;\n")
                    
                    f.write("\t};\n")
            f.write("};\n")
    
                   
            
            
            if(self.goalExplore):
                # A team explore a zone
                for zone in self.getZoneList():
                    for team in self.getTeamList():
                    
                        zoneName = self.getZoneName(zone)
                        teamName = self.getTeamName(team)
                    
                        f.write("\naction explore" + zoneName + teamName + "(){\n")
                        f.write("\t[start]" + teamName + ".zone == " + zoneName + ";\n")
                        
                        
                        patrolList = self.getPatrolList(zone, self.nbrRobot)
                        #list of actions
                        f.write("\t[all] contains {\n")
                        for r in range(1, self.nbrRobot + 1):
                            robotName = self.getRobotName((team,r))
                            for i,loc1,loc2 in zip(range(len(patrolList[r-1])-1),patrolList[r-1][:-1], patrolList[r-1][1:]):
                                f.write("\t\tmove_" + str(i) + "_" + str(r) + " : move (" + robotName + ", " + loc1 + ", " + loc2 + ");\n")
                            for i,loc in zip(range(len(patrolList[r-1])),patrolList[r-1]):
                                f.write("\t\texplore_" + str(i) + "_" + str(r) + " : explore ( " + robotName + ", " + loc + ");\n")
                                
                        
                        f.write("\t};\n\tduration := " + str(2*len(patrolList[0]) -1) + ";\n")
        
                        for r in range(1, self.nbrRobot + 1):
                            robotName = self.getRobotName((team,r))
                            #f.write("\t\t\t\t(:init move-%d-%d (at-r %s %s))\n" % (0, r, robotName, patrolList[r-1][0]))
                            #f.write("\t\t\t\t(:init explore-%d-%d (at-r %s %s))\n" % (0, r, robotName, patrolList[r-1][0]))
                            
                            for i, loc in zip(range(1,len(patrolList[r-1])),patrolList[r-1][1:-1]):
                                f.write("\tend(move_" + str(i-1) + "_" + str(r) + ") < start(explore_" + str(i) + "_" + str(r) + ");\n") 
                                f.write("\tend(move_" + str(i-1) + "_" + str(r) + ") < start(move_" + str(i) + "_" +  str(r) + ");\n") 
                            
                            i = len(patrolList[r-1])-1
                            f.write("\tend(move_" + str(i-1) + "_" + str(r) + ") < start(explore_" + str(i) + "_" + str(r) + ");\n")  
						# last move to last explore
                        #f.write(indent + "\t(stab m (stabilized ?s))")
                        f.write("\n")
                        
                        #temporal links
                        for r in range(1, self.nbrRobot + 1):
                            for i in range(len(patrolList[r-1])-1):
                                f.write("\tend(explore_" + str(i) + "_" + str(r) + ") < start(move_" + str(i) + "_" + str(r) + ");\n")
                                
                        #f.write(indent + "\t(stab m) ")
                        f.write("};\n")

        if(useBzip):
            subprocess.call(["bzip2", filename])

    def getPatrolList(self, zone, nbrVehicle, invertAxis = False):
        start = ( zone[0] * self.sizeGrid[0], zone[1] * self.sizeGrid[1] )
        patrolList = [] #list of all the locs where successive elements are neighboors

        for i in range(0, self.sizeGrid[0]):
            for j in range(0, self.sizeGrid[1], 1) if (i%2==1) else range(self.sizeGrid[1] -1, -1, -1):
                loc = (start[0] + i, start[1] + j) if not invertAxis else (start[1] + j, start[0] + i)
                patrolList.append(self.getLocName(loc))
        numLoc = len(patrolList)

        vehNumToExplore = [int(numLoc/nbrVehicle) for robot in range(nbrVehicle)] #number of loc to explore for each vehicle
        diff = numLoc - sum(vehNumToExplore)
        for i in range(diff):
            vehNumToExplore[i] = vehNumToExplore[i] + 1
        patrolListCopy = patrolList[:]
        vehList = []
        for num in vehNumToExplore:
            vehList.append(patrolListCopy[:num])
            del patrolListCopy[:num]
            
        return vehList
    
    def changeProblemToRepair(self, key):
        if(type(key) == tuple):
            nbr,key = key
        else:
            nbr = 1
            
        
        if(key == "survivorPos"):
            for s in random.sample(self.getSurvivorList(),nbr):
                self.survivorPos[s] = self.getRandomLoc()
        elif(key == "startingPos"):
            for team in random.sample(self.getTeamList(),nbr):
                newPos = random.choice(self.getLocList())
                newZone = self.getZoneOfLoc(newPos)
                
                self.startZone[team] = newZone
                for robot in self.getRobotsInTeam(team):
                    self.startPos[robot] = newPos
        elif(key == "newSurvivor"):
            for _ in range(nbr):
                self.nbrSurvivor += 1
                self.survivorPos[self.getSurvivorList()[-1]] = self.getRandomLoc()
        elif key == "removeSurvivor":
            self.nbrSurvivor -= nbr
        else:
            print "Unrecognized key : %s in changeProblemToRepair" % key
            exit(1)

if __name__ == "__main__":
    main()
