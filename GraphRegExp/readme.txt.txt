Graphregexp by Andre Koschmieder

Usage:
> javac *.java
> java Graphregexp
for non-gui version: > java Graphregexp -nogui

GUI explanation:

Top:
- load graph from edge file (format: Node1 Node2 EdgeLabel)
- load from nodes and edge file: list of nodes and list of edges

Analysis settings:
- start/end: search a path from given start to end node fulfilling the regexp
- any path: evaluate if there exists a path in the graph fulfilling the regexp
- all paths: find all paths in the graph fulfilling the regexp
  (can take some time!)

Bottom line:
- Rare count parameter (number of occurrences allowed for rare labels)
- Max path optimization (see paper)
- Verbose output

Evaluate query set:
Start the evaluation using the given query set file. See samplequery.txt
Nodes names are enclosed by << >> for parsing.

Exp file:
Create a number of queries for the current graph
and export them to a file. The queries follow
certain parameters (see paper).
