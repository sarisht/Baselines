#!/bin/sh
datasets=`find /Users/sarisht/Desktop/MTP/Baselines/LCR/LCRIndexing/experiments/graphs/ | egrep '\.edge$' | sed 's:\.edge::g'`;
for ds in $datasets;
do
    echo $ds
    dsp=`echo $ds | sed 's/.*\///'`;
    L=`cat ${ds}.edge | awk '{ if( $3 in used ){ } else { used[$3]=1; print $3 } }' | wc -l`;
    L1=`expr $L / 4`;
    L2=`expr $L / 2`;
    L3=`expr $L - 2`;

    if [ ! -f "${ds}.querylog" ];
    then
        echo "L=${L}";
        echo "build/default/genQuery ${ds}.edge 3 1000 ${L1} ${L2} ${L3}";
       /Users/sarisht/Desktop/MTP/Baselines/LCR/LCRIndexing/build/default/genQuery ${ds}.edge 3 1000 ${L1} ${L2} ${L3} > abc.querylog
    fi
done;
