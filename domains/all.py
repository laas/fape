import subprocess
import matplotlib.pyplot as plt
import sys
import os  
import numpy as np

folder = "./"
characteristic = [6,2] # generated-states,time
minimumState = 100

def main():
	files = findFiles()
	res1= []
	for i in range (len(files)-2):
		print files[i]
		stdout = subprocess.Popen(["planner","" + files[i] + "","-p","taskcond","-q","--strats","abs:lcf%"],stdout=subprocess.PIPE)
		stdout_value = stdout.communicate()[0] 	
		analyze (res1,stdout_value)

	raw_input("Do your modification")

	res2= []
	for i in range (len(files)-2):
		print files[i]
		stdout = subprocess.Popen(["planner","" + files[i] + "","-p","taskcond","-q","--strats","abs:lcf%"],stdout=subprocess.PIPE)
		stdout_value = stdout.communicate()[0] 	
		analyze (res2,stdout_value)
	for i in range(len(characteristic)):
		trace(res1,res2,characteristic[i])

def trace(res1,res2,charac):
	version1=[]
	version2=[]
	name1=[]
	selectChara(version1,name1,res1,res1,charac)
	selectChara(version2,[],res2,res1,charac)
	fig = plt.figure()
	ax = fig.add_subplot(111)
	## necessary variables
	ind = np.arange(len(version1))                # the x locations for the groups
	width = 0.35                      # the width of the bars

	## the bars
	rects1 = ax.bar(ind, version1, width,color='black')
	rects2 = ax.bar(ind+width, version2, width, color='red')

	# axes and labels
	xTickMarks = [name1[i] for i in range(len(name1))]
	ax.set_xticks(ind+width)
	xtickNames = ax.set_xticklabels(xTickMarks)
	plt.setp(xtickNames,  fontsize=10)
	ax.legend( (rects1[0], rects2[0]), ('fisrt', 'second') )
	ax.set_title("characteristic" + str(charac))
	plt.show()

def analyze(res,out):
	if (len(out) != 0):
		i = 0
		while( out[i] !="\n"):
			i+=1
		i+=1
		f = [""]
		nf =0
		while (i < len(out) ):
			if (out[i] != ","):
				f[nf] = f[nf] + out[i]
			else :
				nf+=1
				f.append("")
			i+=1
		res.append(f)
		
def findFiles():
	out = subprocess.check_output(["find",folder,"-type","f","-name","*anml"])
	f=0    
	files = [""]
	for i in range (len(out)):
		if (out[i] == "\n"):
			f+=1
			files.append("")
			continue
		if f >= 0:
			files[f] = files[f] + out[i]
	return files

def selectChara(charac,name,res,resRef,characteristi):
	
	for i in range(len(resRef)):
		if int(resRef[i][6]) > minimumState:
			charac.append(float(res[i][characteristi]))
			n = res[i][4]
			constName =""
			for j in range(len(n)):
				if n[j] != "/":
					constName = constName + n[j]
				else :
					constName=""
			name.append(constName)





if __name__ == "__main__":
    main()
