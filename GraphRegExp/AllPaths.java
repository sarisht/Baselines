import java.util.*;



public class AllPaths extends Thread
{
    public static Graph g;
    public static RegExp re;
    
    private boolean bRareOpt;
    private int rareCount;
    private boolean bMaxPathOpt;
    private int threadNo;
    private int regNo;
    private int partNum;

    public static SearchExistsThread.NodeState [][]nodeState; // [numThreads][numNodes]
    public static SearchExistsThread.AnalysisPart []parts; // [numSlices]
    public static ArrayList<int[]> []resultBruteForce;
    //public static ArrayList<int[]> resultRare;
    public static long numRaresFound;
    //public static short found[][];
    
    public static boolean bStop;
    public static boolean bRare;
    
    //public static SortedArray sort;
    

    
    AllPaths(int nRareCount,boolean nMaxPathOpt,int nReg)
    {
        bRareOpt = true;
        rareCount = nRareCount;
        bMaxPathOpt = nMaxPathOpt;
        regNo = nReg;
    }
    
    AllPaths(int nThreadNumber,int nReg)
    {
        bRareOpt = false;
        threadNo = nThreadNumber;
        regNo = nReg;
        partNum = nThreadNumber;
        parts[partNum].bProcessed = true;
    }
    
    
    public void run()
    {
        if (bRareOpt) RunRare();
        else RunBruteForce();
    }
    
    
    // Brute force method to find all paths in the graph
    // For all nodes: use the node as start node and
    // search from there for all paths available.
    private void RunBruteForce()
    {
        boolean bContinue;
        long startTime = System.currentTimeMillis();
        
        do {
            for (int i = parts[partNum].startNum; i < parts[partNum].endNum; i++)
            {
                SearchAllDestNodes(g,re,i);
                if (bStop)
                {
                    //System.out.print("T"+threadNo+"("+(System.currentTimeMillis()-startTime)+")Stop ");
                    return;
                }
            }
            bContinue = false;
            for (int i = partNum+1; i < parts.length; i++)
            {
                if (!parts[i].bProcessed)
                {
                    synchronized(resultBruteForce)
                    {
                        if (!parts[i].bProcessed)
                        {
                            partNum = i;
                            parts[partNum].bProcessed = true;
                            bContinue = true;
                            break;
                        }
                    }
                }
            }
        } while (bContinue && !bStop);
        //System.out.print("T"+threadNo+"("+(System.currentTimeMillis()-startTime)+")Finish ");
    }
    
    
    private void SearchAllDestNodes(Graph g,RegExp re,int startNode)
    {
        // Search all reachable nodes through re from the start node.
        // Every combination found is added to the array list.
        
        ArrayList<RegExp.State> fifo = new ArrayList<RegExp.State>(1024);
        int ffPos = 0;
        int numTransitions = 0;
        RegExp.State curState;
        int laufNum = regNo*g.numNodes+startNode;
        
        curState = new RegExp.State(startNode,re.root,null,0);
        curState.pathLength = 0;
        nodeState[threadNo][startNode].AddVisited(laufNum, curState);
        
        while (true)
        {
            numTransitions++;
            
            // Check if the rare search has already finished
            if (bStop)
            {
                return;
            }
            
            // Check if we found a destination node
            if ((curState.pos.bEndsExpression) && (nodeState[threadNo][curState.node].added != laufNum))
            {
                nodeState[threadNo][curState.node].added = laufNum;
                resultBruteForce[threadNo].add(new int[]{startNode,curState.node});
            }
            
            // Continue search
            Node curNode = g.node[curState.node];
            
            if (curState.pos.followEdge.numLabels > 2)
            {
            
                RegExp.JoinResult join = curState.pos.JoinEdges(curNode, true);
                if (join != null)
                {
                    for (int i = 0; i < join.numResults; i++)
                    {
                        int neuNode = join.destNode[i];
                        Item []next = join.destItem[i];
                        for (int j = 0; j < next.length; j++)
                        {
                            if (nodeState[threadNo][neuNode].CycleCheck(laufNum,next[j],0))
                            {
                                RegExp.State st = new RegExp.State(neuNode,next[j],curState,0);
                                st.pathLength = curState.pathLength+1;
                                fifo.add(st);
                                nodeState[threadNo][neuNode].AddVisited(laufNum,st);
                            }
                        }
                    }
                }
            }
            else {
            
                for (int i = 0; i < curNode.numOutEdges; i++)
                {
                    // Match every edge with the regular expression
                    Item []next = curState.pos.MatchEdge(curNode.outLabel[i]);
                    if (next != null)
                    {
                        int neuNode = curNode.outNode[i];
                        for (int j = 0; j < next.length; j++)
                        {
                            // If there is no cycle, add next node
                            if (nodeState[threadNo][neuNode].CycleCheck(laufNum,next[j],0))
                            {
                                RegExp.State st = new RegExp.State(neuNode,next[j],curState,0);
                                st.pathLength = curState.pathLength+1;
                                fifo.add(st);
                                nodeState[threadNo][neuNode].AddVisited(laufNum,st);
                            }
                        }
                    }
                }
            }
            
            if (ffPos < fifo.size())
            {
                curState = fifo.get(ffPos);
                ffPos++;
            } else {
                return;
            }
        }
    }
    
    
    private void RunRare()
    {
        if (bStop) return;
        //long startTime = System.currentTimeMillis();
        
        // 1) Calculate lists of start/end nodes for rare labels
        RareSet rares = RareSet.CalcRares(g,re,rareCount,bMaxPathOpt,true);
        if (rares == null)
        {
            //System.out.print("No rares found! ");
            return;
        }
        if (bStop) return;
        int numRares = rares.numRares;
        //System.out.print("Rare labels: "+numRares+" ");

        
        // 2) Calculate all paths between the rare states (if numRares > 1)
        //    If there is no path between any two rare states, abort search.
        RegExp.ResultWay intPaths[][][] = null;
        if (rares.numRares > 1)
        {
            intPaths = new RegExp.ResultWay[rares.numRares-1][][];
            for (int i = 0; i < rares.numRares-1; i++)
            {
                // ... (exists only)
                intPaths[i] = re.SearchNtoM(rares.endNodes[i].length,rares.endNodes[i],
                        rares.state[i].item,true,rares.startNodes[i+1].length,rares.startNodes[i+1],rares.state[i+1].item);

                if (bStop)
                {
                    //System.out.print("Rare("+(System.currentTimeMillis()-startTime)+")Stop ");
                    return;
                }
                
                // Check if a path was found (quit search otherwise)
                if (!Analysis.AnyPathFound(intPaths[i]))
                {
                    //System.out.print("Rare("+(System.currentTimeMillis()-startTime)+")Finish ");
                    bStop = true;
                    bRare = true;
                    return;
                }
            }
        }

        
        // 3) Calculate internal paths from first to last rare
        //    Remove all start and end nodes that are not on a path
        int numStartNodes = 0;
        int startNodes[] = null;
        int numEndNodes = 0;
        int endNodes[] = null;
        boolean wayBetween[][] = null;

        if (rares.numRares == 1) // start state = end state
        {
            numStartNodes = rares.startNodes[0].length;
            numEndNodes = rares.endNodes[0].length;
            startNodes = rares.startNodes[0];
            endNodes = rares.endNodes[0];
            wayBetween = new boolean[numStartNodes][numEndNodes];
            // Add a path for each connected start and end node
            for (int i = 0; i < numStartNodes; i++)
            {
                for (int j = 0; j < rares.connected[0][i].length; j++)
                {
                    wayBetween[i][rares.connected[0][i][j]] = true;
                }
            }
        }
        else if (rares.numRares == 2) // only start state and end state
        {
            SortedArray st = new SortedArray();
            SortedArray en = new SortedArray();
            // Process every start node for rare state 0
            for (int i = 0; i < rares.startNodes[0].length; i++)
            {
                // Process every end node of start node i
                for (int j = 0; j < rares.connected[0][i].length; j++)
                {
                    // Process every start node for rare state 1
                    for (int k = 0; k < rares.startNodes[1].length; k++)
                    {
                        // Check for a path between end node of state 0 and start node of state 1
                        if (intPaths[0][rares.connected[0][i][j]][k] != null)
                        {
                            // A path has been found, add i as a start node
                            int sPos = st.InsertIfNotInside(rares.startNodes[0][i],new SortedArray());
                            SortedArray con = (SortedArray)st.GetAt(sPos);
                            // Process every end node of start node k
                            for (int l = 0; l < rares.connected[1][k].length; l++)
                            {
                                // A path leads here, add as end node and the connection
                                en.InsertIfNotInside(rares.endNodes[1][rares.connected[1][k][l]],null);
                                con.InsertIfNotInside(rares.endNodes[1][rares.connected[1][k][l]],null);
                            }
                        }
                    }
                }
            }
            // Translate the sorted arrays back to standard arrays
            numStartNodes = st.Size();
            numEndNodes = en.Size();
            startNodes = new int[numStartNodes];
            endNodes = new int[numEndNodes];
            wayBetween = new boolean[numStartNodes][numEndNodes];
            for (int i = 0; i < numStartNodes; i++)
            {
                startNodes[i] = st.GetKeyAt(i);
                SortedArray sa = (SortedArray)st.GetAt(i);
                for (int j = 0; j < sa.Size(); j++)
                {
                    wayBetween[i][en.GetPos(sa.GetKeyAt(j))] = true;
                }
            }
            for (int i = 0; i < numEndNodes; i++)
                endNodes[i] = en.GetKeyAt(i);
        }
        else // numRares > 2
        {
            System.err.print("ERROR:NYI");
            return;
            // ...
        }
        if (bStop)
        {
            //System.out.print("Rare("+(System.currentTimeMillis()-startTime)+")Stop ");
            return;
        }

        // 5. Calculate all shortest paths from first rare nodes backwards
        //    (If the first rare state is not a start state)
        ArrayList<Integer> startPathNodes[] = new ArrayList[numStartNodes];
        int tnS=0;
        for (int i = 0; i < numStartNodes; i++)
        {
            startPathNodes[i] = SearchOneWay_All(g,re,rares.state[0].item,startNodes[i],false);
            if (bStop)
            {
                //System.out.print("Rare("+(System.currentTimeMillis()-startTime)+")Stop ");
                return;
            }
            tnS += startPathNodes[i].size();
        }
        if (tnS == 0)
        {
            //System.out.print("Rare("+(System.currentTimeMillis()-startTime)+")FinishS ");
            bRare = true;
            bStop = true;
            return;
        }
        //System.out.print("startP="+tnS+" ");
        
        // 6. Calculate all paths from last rare nodes to the end
        //    (If the last rare state is not a finish state)
        ArrayList<Integer> endPathNodes[] = new ArrayList[numEndNodes];
        int tnE=0;
        for (int i = 0; i < numEndNodes; i++)
        {
            endPathNodes[i] = SearchOneWay_All(g,re,rares.state[numRares-1].item,endNodes[i],true);
            if (bStop)
            {
                //System.out.print("Rare("+(System.currentTimeMillis()-startTime)+")Stop ");
                return;
            }
            tnE += endPathNodes[i].size();
        }
        if (tnE == 0)
        {
            //System.out.print("Rare("+(System.currentTimeMillis()-startTime)+")FinishE ");
            bRare = true;
            bStop = true;
            return;
        }
        //System.out.print("endP="+tnE+" ");
        
                
        //SortedArray found[] = new SortedArray[g.numNodes];
        //SortedArray found = new SortedArray(256);
        
        // 7. search all paths through the results
        if (numRares == 1)
        {
            // Sort/Merge test version
            int sortStart[][] = new int[tnS][2];
            for (int curS=0, i = 0; i < numStartNodes; i++)
            {
                int n = startPathNodes[i].size();
                for (int j = 0; j < n; j++, curS++)
                {
                    sortStart[curS][0] = startPathNodes[i].get(j);
                    sortStart[curS][1] = i;
                }
            }
            //long tSort = System.currentTimeMillis();
            Arrays.sort(sortStart,Globals.graph);
            //tSort = System.currentTimeMillis() - tSort;
            //if (tSort > 1000) System.out.print(" Sorting:"+tSort+"ms ");
            
            //long tRestore = System.currentTimeMillis();
            int curS = 0, lastNode = -1;
            int used[] = new int[g.numNodes];
            for (int s = 0; s < tnS; s++)
            {
                if (sortStart[s][0] != lastNode)
                {
                    if (bStop) return;
                    curS++;
                    lastNode = sortStart[s][0];
                }
                
                for (int e = 0; e < rares.connected[0][sortStart[s][1]].length; e++)
                {
                    int curEnd = rares.connected[0][sortStart[s][1]][e];
                    int nE = endPathNodes[curEnd].size();
                    
                    for (int j = 0; j < nE; j++)
                    {
                        int end = endPathNodes[curEnd].get(j);
                        if (used[end] != curS)
                        {
                            used[end] = curS;
                            //resultRare.add(new int[]{start,end});
                            numRaresFound++;
                        }
                    }
                }
            }
            //tRestore = System.currentTimeMillis() - tRestore;
            //if (tRestore > 1000) System.out.print(" Fetching:"+tRestore+"ms ");

            
            // Start and end nodes are the same
            /*for (int s = 0; s < numStartNodes; s++)
            {
                int nS = startPathNodes[s].size();
                if (nS == 0) continue;
                
                for (int e = 0; e < rares.connected[0][s].length; e++)
                {
                    int curEnd = rares.connected[0][s][e];
                    int nE = endPathNodes[curEnd].size();
                    if (nE == 0) continue;

                    for (int i = 0; i < nS; i++)
                    {
                        int start = startPathNodes[s].get(i);
                        for (int j = 0; j < nE; j++)
                        {*/
                            // Double byte list version
                            /*int end = endPathNodes[curEnd].get(j);
                            if (found[start][end] != regNo)
                            {
                                found[start][end] = (short)regNo;
                                //resultRare.add(new int[]{start,end});
                                numRaresFound++;
                            }*/
                            
                            // Single list with sorted Arrays version
                            /*if (found[start] == null)
                            {
                                found[start] = new SortedArray(endPathNodes[curEnd].get(j),null);
                                //resultRare.add(new int[]{start,end});
                                numRaresFound++;
                            } else {
                                if (found[start].InsertUnique(endPathNodes[curEnd].get(j),null))
                                {
                                    //resultRare.add(new int[]{start,end});
                                    numRaresFound++;
                                }
                            }*/
                            
                            // Nested SortedArray version
                            /*int pos = found.GetPos(start);
                            if (pos < 0)
                            {
                                found.Insert(start,new SortedArray(endPathNodes[curEnd].get(j),null));
                                numRaresFound++;
                            } else {
                                if (((SortedArray)found.GetAt(pos)).InsertUnique(endPathNodes[curEnd].get(j),null))
                                    numRaresFound++;
                            }
                            //if (numRaresFound%1000000 == 0) System.out.print(".");
                        }
                    }
                }
                if (bStop) return;
            }*/
        } else {
            // Different start and end nodes
            System.err.println("NYI");
            return;
            /*for (int s = 0; s < numStartNodes; s++)
            {
                int nS = startPathNodes[s].size();
                if (nS == 0) continue;
                
                for (int e = 0; e < numEndNodes; e++)
                {
                    int nE = endPathNodes[e].size();
                    if (nE == 0) continue;
                    
                    if (wayBetween[s][e])
                    {
                        for (int i = 0; i < nS; i++)
                        {
                            for (int j = 0; j < nE; j++)
                            {
                                if (!sort.Contains(startPathNodes[s].get(i)*g.numNodes+endPathNodes[s].get(j)))
                                {
                                    sort.Insert(startPathNodes[s].get(i)*g.numNodes+endPathNodes[s].get(j),null);
                                    resultRare.add(new int[]{startPathNodes[s].get(i),endPathNodes[e].get(j)});
                                }
                            }
                        }
                    }
                }
                if (bStop) return;
            }*/
        }
        
        bRare = true;
        bStop = true;
        //System.out.print("Rare("+(System.currentTimeMillis()-startTime)+")EndFinish ");
    }
    
    
    private ArrayList<Integer> SearchOneWay_All(Graph g,RegExp re,Item startState,int startNode,boolean bForward)
    {
        // Search all reachable nodes through re from the start node.
        // Every combination found is added to the array list.
        
        ArrayList<Integer> result = new ArrayList<Integer>(16);
        
        ArrayList<RegExp.State> fifo = new ArrayList<RegExp.State>(1024);
        int ffPos = 0;
        int numTransitions = 0;
        RegExp.State curState;
        RegExp.numLauf++;
        
        curState = new RegExp.State(startNode,startState,null,0);
        g.node[startNode].AddVisited(RegExp.numLauf, curState);
        
        while (true)
        {
            numTransitions++;
            
            // Check if the rare search has already finished
            if (bStop)
            {
                return null;
            }
            
            // Check if we found a destination node
            if (curState.pos.EndsExpression(bForward))
            {
                result.add(new Integer(curState.node));
            }
            
            // Continue search
            Node curNode = g.node[curState.node];
            
            if (curState.pos.followEdge.numLabels > 2)
            {
            
                RegExp.JoinResult join = curState.pos.JoinEdges(curNode, bForward);
                if (join != null)
                {
                    for (int i = 0; i < join.numResults; i++)
                    {
                        int neuNode = join.destNode[i];
                        Item []next = join.destItem[i];
                        for (int j = 0; j < next.length; j++)
                        {
                            if (g.node[neuNode].CycleCheck(bForward,RegExp.numLauf,next[j],0))
                            {
                                RegExp.State st = new RegExp.State(neuNode,next[j],curState,0);
                                st.pathLength = curState.pathLength+1;
                                fifo.add(st);
                                g.node[neuNode].AddVisited(bForward,RegExp.numLauf,st);
                            }
                        }
                    }
                }
            }
            else {
            
                int numEdges = (bForward? curNode.numOutEdges : curNode.numInEdges);
                for (int i = 0; i < numEdges; i++)
                {
                    // Match every edge with the regular expression
                    Item []next = null;
                    if (bForward) next = curState.pos.MatchEdge(curNode.outLabel[i]);
                    else next = curState.pos.MatchBackEdge(curNode.inLabel[i]);
                    if (next != null)
                    {
                        int neuNode = (bForward? curNode.outNode[i] : curNode.inNode[i]);
                        for (int j = 0; j < next.length; j++)
                        {
                            // If there is no cycle, add next node
                            if (g.node[neuNode].CycleCheck(bForward,RegExp.numLauf,next[j],0))
                            {
                                RegExp.State st = new RegExp.State(neuNode,next[j],curState,0);
                                fifo.add(st);
                                g.node[neuNode].AddVisited(bForward,RegExp.numLauf,st);
                            }
                        }
                    }
                }
            }
            
            if (ffPos < fifo.size())
            {
                curState = fifo.get(ffPos);
                ffPos++;
            } else {
                return result;
            }
        }
    }

    
    
    /*public static boolean ExecQuery(Graph g,RegExp re,int rareCount,boolean bOptimize,boolean bAtLeastOne,int curNum)
    {
        // Search all paths in a graph - procedure
        // 1. calculate rare states
        // 1a. (numRare > 1) calculate all paths between rares
        // 1b. (numRare > 1) remove start / end rares which are not on a path
        // 2a. (numRare >= 1) calculate paths from rare#1 backwards to start (if start != rare#1)
        // 2b. (numRare >= 1) calculate paths from rare#last to end (if end != rare#last)
        // 3. (numRare >= 1) search a path through the results
        // 4. (numRare = 0) full-scale end-to-end search (expensive)
        boolean bDebugOutput = false;
        long startTime = 0;
        if (bDebugOutput) startTime = System.nanoTime();

        // Debug output
        if (bDebugOutput)
        {
            System.out.println("*** "+curNum+" *** "+re.expString);
        }
        //System.out.print(curNum+" ");

        // 1. calculate rare states
        RegExp.Unique rares[] = re.GetRareStates(rareCount, bOptimize);
        if ((rares == null) && (bAtLeastOne))
        {
            // No rares found with current settings, but we really do want at least one
            // Use the label with least occurrences (up to 1/10 of edges)
            RegExp.Unique all[] = re.root.GetMandatoryUniques(g.numEdges/10);
            if (all != null)
            {
                int lowOcc = all[0].count;
                int lowNum = 0;
                for (int i = 1; i < all.length; i++)
                {
                    if (all[i].count < lowOcc)
                    {
                        lowOcc = all[i].count;
                        lowNum = i;
                    }
                }
                rares = new RegExp.Unique[]{all[lowNum]};
            }
        }
        int numRares = 0;
        if (rares != null) numRares = rares.length;
        
        // Calculate the corresponding start and end nodes for the rare states
        int [][][]rareNodes = new int[numRares][2][];
        for (int i = 0; i < numRares; i++)
        {
            if (rares[i].item.dest == Item.DEST_NODE)
            {
                rareNodes[i][0] = rareNodes[i][1] = g.GetNodesByLabel(rares[i].item.symbol);
            } else {
                rareNodes[i][0] = g.GetEdgeStartNodes(rares[i].item.symbol);
                rareNodes[i][1] = g.GetEdgeEndNodes(rares[i].item.symbol);
            }
        }
        
        // Debug output
        if (bDebugOutput)
        {
            System.out.print("Rare states: "+numRares);
            if (numRares == 1)
            {
                System.out.print(" ("+rareNodes[0][0].length+")");
                if (rares[0].count > rareCount) System.out.print(" [forced]");
            } else if (numRares > 1)
            {
                int minRare, maxRare;
                minRare = maxRare = rares[0].count;
                for (int i = 1; i < numRares; i++)
                {
                    if (rares[i].count < minRare) minRare = rares[i].count;
                    else if (rares[i].count > maxRare) maxRare = rares[i].count;
                }
                System.out.print(" (min: "+minRare+", max: "+maxRare+")");
            }
            System.out.println();
        }
        
        
        // 1a. calculate all paths between the rare states (if numRares > 1)
        RegExp.ResultWay intPaths[][][] = null;
        int wayBetween[][] = null;
        int numStartNodes = 0;
        int startNodes[][] = null;
        int numEndNodes = 0;
        int endNodes[][] = null;
        
        if (numRares > 1)
        {
            numMoreThanOne++;
            startNodes = new int[2][rareNodes[0][0].length];
            endNodes = new int[2][rareNodes[numRares-1][1].length];
            
            intPaths = new RegExp.ResultWay[numRares-1][][];
            for (int i = 0; i < numRares-1; i++)
            {
                intPaths[i] = re.SearchNtoM(rareNodes[i][1].length, rareNodes[i][1], rares[i].item, true, rareNodes[i+1][0].length, rareNodes[i+1][0], rares[i+1].item);
                
                // Check if a path was found (quit search otherwise)
                boolean bStop = true;
outerLoop:      for (int j = 0; j < rareNodes[i][1].length; j++)
                {
                    for (int k = 0; k < rareNodes[i+1][0].length; k++)
                    {
                        if (intPaths[i][j][k] != null)
                        {
                            bStop = false;
                            break outerLoop;
                        }
                    }
                }
                if (bStop)
                {
                    if (bDebugOutput) System.out.println("No path in between - fast negative!");
                    return false;
                }
            }
        } else if (numRares == 1)
        {
            numStartNodes = numEndNodes = rareNodes[0][0].length;
            startNodes = new int[2][numStartNodes];
            endNodes = new int[2][numEndNodes];
            for (int i = 0; i < numStartNodes; i++)
            {
                startNodes[0][i] = rareNodes[0][0][i];
                startNodes[1][i] = i;
            }
            for (int i = 0; i < numEndNodes; i++)
            {
                endNodes[0][i] = rareNodes[0][1][i];
                endNodes[1][i] = i;
            }
        }
        
        
        // 1b. remove start / end rares which are not on a path
        if (numRares > 1)
        {
            // Check all start nodes of first path
            for (int i = 0; i < rareNodes[0][1].length; i++)
            {
                for (int j = 0; j < rareNodes[1][0].length; j++)
                {
                    if (intPaths[0][i][j] != null)
                    {
                        startNodes[0][numStartNodes] = rareNodes[0][0][i];
                        startNodes[1][numStartNodes] = i;
                        numStartNodes++;
                        break;
                    }
                }
            }
            
            // Check all end nodes of last path
            for (int i = 0; i < rareNodes[numRares-1][0].length; i++)
            {
                for (int j = 0; j < rareNodes[numRares-2][1].length; j++)
                {
                    if (intPaths[numRares-2][j][i] != null)
                    {
                        endNodes[0][numEndNodes] = rareNodes[numRares-1][1][i];
                        endNodes[1][numEndNodes] = i;
                        numEndNodes++;
                        break;
                    }
                }
            }
            
            // Now check the real paths start -> end
            wayBetween = new int[numStartNodes][numEndNodes];
            if (numRares == 2)
            {
                for (int i = 0; i < numStartNodes; i++)
                    for (int j = 0; j < numEndNodes; j++)
                        if (intPaths[0][i][j] != null) wayBetween[i][j] = intPaths[0][i][j].wayLength;
                        else wayBetween[i][j] = -1;
            } else if (numRares > 2)
            {
                // ...
            }
            
            // Debug output
            if (bDebugOutput)
            {
                System.out.println("Reduced start/end: ("+rareNodes[0][0].length+","+rareNodes[numRares-1][1].length+
                        ") -> ("+numStartNodes+","+numEndNodes+")");
            }
        }
        
        
        // 2a. Calculate all shortest paths from first rare nodes backwards
        //     (If the first rare state is not a start state)
        RegExp.numLauf++;
        RegExp.ResultWay startPaths[] = null;
        if (numRares > 0)
        {
            if (!rares[0].item.bBackEnds)
            {
                if (bDebugOutput) System.out.print("Executing start-node search...");
                startPaths = SearchOneWay(g, re, rares[0].item, numStartNodes, startNodes[0], false);
                if (bDebugOutput) System.out.println(" done ("+lastTransitions+","+lastFifo+")");
                RegExp.globalTransitions += lastTransitions;
                RegExp.globalFifo += lastFifo;
            }
        }
        
        
        // 2b. Calculate all paths from last rare nodes to the end
        //     (If the last rare state is not a finish state)
        RegExp.numLauf++;
        RegExp.ResultWay endPaths[] = null;
        if (numRares > 0)
        {
            if (!rares[numRares-1].item.bEndsExpression)
            {
                if (bDebugOutput) System.out.print("Executing end-node search...");
                endPaths = SearchOneWay(g, re, rares[numRares-1].item, numEndNodes, endNodes[0], true);
                if (bDebugOutput) System.out.println(" done ("+lastTransitions+","+lastFifo+")");
                RegExp.globalTransitions += lastTransitions;
                RegExp.globalFifo += lastFifo;
            }
        }
        
        
        // 3. search a path through the results
        boolean bFound = false;
        if (numRares > 0)
        {
            int shortWay = 1000000;
            int shortStart = -1;
            int shortEnd = -1;
            
            // Simple implementation: calculate all paths and search the shortest
            for (int i = 0; i < numStartNodes; i++)
            {
                int len = 0;
                if (startPaths != null) // First rare not start state
                {
                    if (startPaths[i] != null) len += startPaths[i].wayLength;
                    else continue; // No path to start state found
                }
                
                if (numRares == 1) // Start and end nodes are the same
                {
                    if (endPaths != null)
                    {
                        if (endPaths[i] != null) len += endPaths[i].wayLength;
                        else continue;
                    }
                    
                    if (len < shortWay)
                    {
                        shortWay = len;
                        shortStart = shortEnd = i;
                    }
                }
                
                else { // Different start and end nodes
                    for (int j = 0; j < numEndNodes; j++)
                    {
                        int len2 = len + wayBetween[i][j];
                        if (wayBetween[i][j] >= 0)
                        {
                            if (endPaths != null)
                            {
                                if (endPaths[j] != null) len2 += endPaths[j].wayLength;
                                else continue;
                            }
                            
                            if (len2 < shortWay)
                            {
                                shortWay = len2;
                                shortStart = i;
                                shortEnd = j;
                            }
                        }
                    }
                }
            }
            bFound = (shortStart >= 0);
        }
        
        
        // 4. No rare states found, have to use brute force
        if (numRares == 0)
        {
            numBruteForce++;
            if (bDebugOutput) System.out.print("Using brute force ...");
            RegExp.ResultWay res = SearchPath_BruteForce(g,re);
            bFound = res != null;
            if (bDebugOutput) 
            {
                System.out.print(" "+(bFound?"found! ["+res.wayLength+"]":"not found."));
                System.out.println(" ("+lastTransitions+" / "+lastFifo+")");
            }
        }

        if (bDebugOutput)
        {
            long time = System.nanoTime() - startTime;
            System.out.println("Search finished: "+(bFound?"found!":"not found.")+" - "+Globals.TimeTextNano(time));
        }
        
        // Finished. Evaluation...
        // ...
        return bFound;
    }*/
    
    
    
    
    // nacheinander suchen - gleichzeitig bringt keinen Vorteil 
    /*public static boolean[] SearchOneWay_Exists(Graph g,RegExp re,Item startState,int numStartNodes,int startNodes[],boolean bForward)
    {
        // Assertions: startState != root/eof, numStartNodes > 0, RegExp.numLauf set correctly.
        
        boolean bFound[] = new boolean[numStartNodes];
        ArrayList<RegExp.State> fifo = new ArrayList<RegExp.State>(1024);
        int ffPos = 0;
        int numTransitions = 0;
        int ffSize = 0;
        RegExp.State curState;
        
        for (int step = 0; step < numStartNodes; step++)
        {
            if (step%100 == 1) System.out.print(".");
            
            // Initialize start position
            RegExp.numLauf++;
            ffSize += fifo.size();
            fifo.clear();
            ffPos = 0;
            curState = new RegExp.State(startNodes[step],startState,null,step);
            fifo.add(curState);
            g.node[startNodes[step]].AddVisited(bForward,RegExp.numLauf,curState);
            ffPos++;
            
            // one-way search
            while (true)
            {
                numTransitions++;
                
                // Check for completed path
                if (curState.pos.EndsExpression(bForward))
                {
                    bFound[step] = true;
                    break;
                }
                
                // Make one step forward
                Node curNode = g.node[curState.node];
                int numEdges = (bForward? curNode.numOutEdges : curNode.numInEdges);
                for (int i = 0; i < numEdges; i++)
                {
                    // Match every edge with the regular expression
                    Item []next = null;
                    if (bForward) next = curState.pos.MatchEdge(curNode.outLabel[i]);
                    else next = curState.pos.MatchBackEdge(curNode.inLabel[i]);
                    if (next != null)
                    {
                        for (int j = 0; j < next.length; j++)
                        {
                            // If there is no cycle, add next node
                            int neuNode = (bForward? curNode.outNode[i] : curNode.inNode[i]);
                            if (g.node[neuNode].CycleCheck(bForward,RegExp.numLauf,next[j],curState.pathNum))
                            {
                                RegExp.State st = new RegExp.State(neuNode,next[j],curState,curState.pathNum);
                                fifo.add(st);
                                g.node[neuNode].AddVisited(bForward,RegExp.numLauf,st);
                            }
                        }
                    }
                }
                
                // Prepare the next step
                if (ffPos < fifo.size())
                {
                    curState = fifo.get(ffPos);
                    ffPos++;
                } else {
                    bFound[step] = false;
                    break;
                }
            }
        }
        lastTransitions = numTransitions;
        lastFifo = ffSize+fifo.size();
        return bFound;
    }*/
    
    
    
}

