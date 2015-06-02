import subprocess
import matplotlib.pyplot as plt
import sys
import os  

files = [" ./tmshier.p1.pb.anml"," ./tmshier.p2.pb.anml"," ./tmshier.p3.pb.anml", "./tmshier.p4.pb.anml"," ./tmshier.p5.pb.anml"]

def main():
    for i in range(len(files)):
        sf = 0.0
        out = subprocess.check_output(["planner", files[i], "-q", "-p", "htn", "--strats", "hf:ogf:abs:lcf:eogf%dfs"])
        print out
        n = 126
        runtime =""
        while out[n] != ',':
            runtime +=out[n]
            n+=1
        try :
            sf +=float(runtime)
        except :
            print runtime
        l.append(sf)
    plt.plot(l)
    plt.show()

if __name__ == "__main__":
    main()
