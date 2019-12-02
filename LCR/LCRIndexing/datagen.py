import random
import sys

N = int(sys.argv[1])
E = int(sys.argv[2])
labels = int(sys.argv[3])


ofile = open("Graph"+str(N)+"kN"+str(E)+"kE"+str(labels)+".txt","w")
N = N*1000
E = E*1000
s = ""

for i in range(E):
    y = 0
    x = 0
    while (y == x):
        y = random.randint(0,N-1)
        x = random.randint(0,N-1)
    label = random.randint(0,labels-1)
    ofile.write(str(x)+" "+str(y)+" "+ str(label)+"\n")

