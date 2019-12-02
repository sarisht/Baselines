#!/bin/bash

function thousands {
    sed -re ' :restart ; s/([0-9])([0-9]{3})($|[^0-9])/\1,\2\3/ ; t restart '
}

a=`find experiments/graphs/ | egrep '\.edge$' | sed 's:\.edge::'`
#for i in $a; do echo "\"$i\","; done;

# shell script to get all statistics of graphs for latex document

t=$(find experiments/graphs/ | egrep '.csv$' | egrep -v 'querystats.csv|Clustered');

for i in $t
do
    #echo "i="$i;

    N=$(cat $i | egrep '^N,' | egrep -o '[0-9\_]*' | thousands);
    M=$(cat $i | egrep '^M,' | egrep -o '[0-9\_]*' | thousands);
    L=$(cat $i | egrep '^L,' | egrep -o '[0-9\_]*' | thousands);

    #echo $i" "$N" "$M" "$L;

    maxSCC=$(cat $i | egrep '^Largest SCC \#nodes\/N,' | egrep -o '[0-9\.\_]*' | cut -c 1-4);
    tr=$(cat $i | egrep '^Number of triangles,' | egrep -o '[0-9\.\_]*' | thousands);
    di=$(cat $i | egrep '^Diameter,' | egrep -o '[0-9\.\_]*');
    #qd=$(echo $i | sed 's:csv:querylog:g' | xargs cat | egrep -o 'actualDiff=[0-9]*' | sed 's:actualDiff=\([0-9]*\):\1:p' | awk 'BEGIN { K=0; } { K+=1; s+=$1; } END { s1=s/K; j=index(s1, "."); print substr(s1, 0, j+2); }')
    mod="pa";
    if [[ $i == *"ER"* ]]; then
        mod="er";
    elif [[ $i == *"ff"* ]]; then
        mod="ff";
    elif [[ $i == *"pl"* ]]; then
        mod="pl";
    elif [[ $i == *"real"* ]]; then
        if [[ $i == *"L8exp"* ]]; then
            mod="r2";
        else
            mod="r1";
        fi
    fi
    name=$(echo $i | sed 's/.*\///' | sed 's/\.csv//g' );


    echo "textbf{"$name"} "$N" "$M" "$L";
    #"$mod" "$maxSCC" "$tr" "$di;
done | sort -n -k 2 | sed 's:\ :\ \&\ :g' | sed 's:$:\ \\\\:g'


#t=$(find experiments/graphs/ | egrep '.edge$');
#for i in $t
#do
#    stats=$(cat $i | awk '{ if( $3 in freqs ) { freqs[$3] += 1; } else { freqs[$3]=1; } } END { for(i in freqs) { print i,freqs[i]; } }');
#    echo $stats
#done
