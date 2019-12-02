import numpy as np
import pandas as pd
inps = ["label30edges100", "label30edges133", "label30edges150", "label30edges166", "label30edges175","label30edges200", "label25edges200","label20edges200","label15edges200","label10edges200","label5edges200"]
b = open("runRandom.sh", 'w')
for inp1 in inps:
    a = open("../../twitter/subgraphs/"+inp1+"queries.txt",'r')
    rin = pd.read_csv("../../twitter/subgraphs/"+inp1+".txt", delimiter = " ", header = None)
    l = a.readline()
    for i in range(10):
        ofile = open("../../twitter/subgraphs/"+inp1+"queries"+str(i)+".txt",'w')
        for j in range(10):
            ofile.write(l)
            l = a.readline()
        ofile.close()

    b.write('echo '+inp1+'>>rare.txt\n')
    for i in range(100):
        start = int(rin[0][np.random.randint(len(rin[0]))])
        end = int(rin[1][np.random.randint(len(rin[1]))])
        regex = np.random.randint(10)
        b.write('java Graphregexp -nogui -e ../../twitter/subgraphs/'+inp1+'.txt -ex '+'../../twitter/subgraphs/'+inp1+'queries'+str(regex)+".txt"+' -start '+str(start)+' -end '+str(end)+' -exist >> rare.txt\n')
    