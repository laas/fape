import subprocess
import matplotlib.pyplot as plt
import sys
import os  

def main():
    if len(sys.argv) > 1:
        nbDepth = int(sys.argv[1])
        if nbDepth < 2 :
           nbDepth =2
    else :
       nbDepth =2

    l =[0.0,0.0]
    m =[0.0,0.0]
    for i in range (2,nbDepth+1):
        print " nbDepth = " + str(i) + " in progess .." 
        sf = 0.0
        sh = 0.0
        subprocess.call(["python", "./depthGenerator.py", str(i)])
        for j in range(10):
            outflat = subprocess.check_output(["fape-planner", "./depth/depth-flat" + str(i) + ".dom.anml", "-q", "-p", "taskcond"])
            outhier = subprocess.check_output(["fape-planner", "./depth/depth-hier" + str(i) + ".dom.anml", "-q", "-p", "taskcond"])
            n = 126
            runtimeflat =""
            runtimehier=""
            while outflat[n] != ',':
                runtimeflat +=outflat[n]
                n+=1
            n = 126
            while outhier[n] != ',':
                runtimehier +=outhier[n]
                n+=1
            sf +=float(runtimeflat)
            sh +=float(runtimehier)
        l.append(sf/10)
        m.append(sh/10)
    plt.plot(l)
    plt.plot(m)
    plt.show()

if __name__ == "__main__":
    main()
