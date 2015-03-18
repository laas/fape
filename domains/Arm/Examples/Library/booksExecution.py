import subprocess
import matplotlib.pyplot as plt
import sys
import os  


occurrence = 10

def main():
    if len(sys.argv) > 1:
        books = int(sys.argv[1])
        if books < 2 :
           books =2
    else :
       books =2
    l = [0.0,0.0]
    for i in range (1,books+1):
        print " books = " + str(i) + " in progess .." 
        sf = 0.0
        for j in range(occurrence):
    	    subprocess.call(["python", "./problemLibraryGenerator.py", "1",str(i)])
            out = subprocess.check_output(["planner", "pecora.library.pb.anml", "-q", "-p", "taskcond", "--strats", "abs:lcf%"])
            n = 126
            runtime =""
            while out[n] != ',':
                runtime +=out[n]
                n+=1
            try :
                sf +=float(runtime)
            except :
                print runtime
        l.append(sf/occurrence)
    plt.plot(l)
    plt.show()

if __name__ == "__main__":
    main()
