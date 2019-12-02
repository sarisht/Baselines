import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Analysis
{

    
    
    // Analysis method #1: Find the shortest path from given start node (optionally, to given end node)
    // Simply use unchanged DA code here
    public static void RunStartEnd(File regFile,int rareCount,boolean bOptimize,int startNode,int endNode)
    {
        RegExp []exp = ParseRegExpFile(regFile);
        if (exp == null) return;
        Writer.WriteLine("Starting analysis of shortest-path from given start node...");
        Writer.WriteLine("Start node: "+Globals.graph.GetNodeName(startNode)+", "+(endNode>=0?"end node: "+Globals.graph.GetNodeName(endNode)+", ":"")+"rareCount="+rareCount+", optimize="+bOptimize);
        int numRuns = 1;//Math.min(Globals.graph.numNodes-1,1000);
        Progress.StartProgress(exp.length*numRuns);
        
        int numFound=0, numTransitions=0, numFifo=0;
        int succTrans=0, succFifo=0;
        RegExp.numRealSearch = 0;
        long startTime = System.currentTimeMillis();
        
        // Temp. impl: use many start nodes
        for (int glob = 1; glob <= numRuns; glob++)
        {
            for (int i = 0; i < exp.length; i++)
            {
                Progress.SetValue((glob-1)*exp.length+i+1);
            
                // Current version: not multithreaded.
                if (exp[i].ExecQuery(startNode, endNode, rareCount, bOptimize, false))
                //if (exp[i].ExecQuery(glob, -1, rareCount, bOptimize, false))
                {
                    numFound++;
                    succTrans += RegExp.globalTransitions;
                    succFifo += RegExp.globalFifo;
                }
                numTransitions += RegExp.globalTransitions;
                numFifo += RegExp.globalFifo;
            }
        }
        
        long time = System.currentTimeMillis()-startTime;
        String dauer = Globals.TimeText(time);
        
        Writer.WriteLine("\nFinished! Time: "+dauer+" Found: "+numFound+"/"+exp.length*numRuns+" ("+(numFound*100/(exp.length*numRuns))+"%)");
        Writer.WriteLine("Search calls: "+RegExp.numRealSearch+"; Transitions: "+numTransitions+"; Fifo: "+numFifo);
        Writer.WriteLine("Success: "+(numFound*100/(exp.length*numRuns))+"%: Transitions: "+succTrans+"; Fifo: "+succFifo);
        double dSuccTr = (double)succTrans / (double)numFound;
        double dSuccFi = (double)succFifo / (double)numFound;
        double dUnsuTr = (double)(numTransitions-succTrans) / (double)(exp.length*numRuns-numFound);
        double dUnsuFi = (double)(numFifo-succFifo) / (double)(exp.length*numRuns-numFound);
        Writer.WriteLine(String.format("Success: Trans/call: %.2f; Fifo/call: %.2f; NOT Success: Trans/call: %.2f; Fifo/call: %.2f.",dSuccTr,dSuccFi,dUnsuTr,dUnsuFi));
    }

    
    
    // Analysis method #2: Check if any path exists in the graph
    // Run brute force and rare label search at the same time
    // More than 3 threads for brute force did not prove effective.
    public static void RunExists(File regFile,int rareCount,boolean bOptimize)
    {
        RegExp []exp = ParseRegExpFile(regFile);
        if (exp == null) return;
        Writer.WriteLine("Starting analysis: check if any given path exists in the graph...");
        
        Graph g = Globals.graph;
        int numFound = 0;
        Progress.StartProgress(exp.length);
        int numOverallThreads = Globals.GetNumThreads();
        if (numOverallThreads > 10) numOverallThreads = 10;
        SearchExistsThread.numBruteForce = 0;
        SearchExistsThread.numRare = 0;
        SearchExistsThread.rejectRare = 0;

        int rareThreads = 3;//numOverallThreads/2;
        int bruteThreads = 3;//numOverallThreads/2;
        int slices = 4;
        SearchExistsThread.numRareThreads = rareThreads;
        Writer.WriteLine("rareCount="+rareCount+", optimize="+bOptimize+" --- RareThreads="+rareThreads+", BruteForceThreads="+bruteThreads+", SlicesPerThread="+slices);
        
        SearchExistsThread.nodeState = new SearchExistsThread.NodeState[bruteThreads][g.numNodes];
        for (int n = 0; n < bruteThreads; n++)
        {
            for (int i = 0; i < g.numNodes; i++)
            {
                SearchExistsThread.nodeState[n][i] = new SearchExistsThread.NodeState();
            }
        }
        SearchExistsThread.parts = new SearchExistsThread.AnalysisPart[bruteThreads*slices];
        for (int i = 0; i < bruteThreads*slices; i++)
        {
            SearchExistsThread.parts[i] = new SearchExistsThread.AnalysisPart(i*g.numNodes/bruteThreads/slices,(i<bruteThreads*slices-1?(i+1)*g.numNodes/bruteThreads/slices:g.numNodes));
        }
        RareStartEndSearch.nodeState = new SearchExistsThread.NodeState[rareThreads][g.numNodes];
        for (int n = 0; n < rareThreads; n++)
        {
            for (int i = 0; i < g.numNodes; i++)
            {
                RareStartEndSearch.nodeState[n][i] = new SearchExistsThread.NodeState();
            }
        }


        long startTime = System.currentTimeMillis();
        
        for (int curReg = 0; curReg < exp.length; curReg++)
        {
            Progress.SetValue(curReg+1);
            //System.err.print("\n"+curReg+": ");

            // Parallel search: Rare optimization and brute force parallel
            SearchExistsThread.bFound = false;
            SearchExistsThread.bStop = false;
            for (int i = 0; i < bruteThreads*slices; i++)
            {
                SearchExistsThread.parts[i].bProcessed = false;
            }
            SearchExistsThread rareThread = null;
            if (rareCount > 0)
            {
                rareThread = new SearchExistsThread(-1,curReg,g,exp[curReg],-1,rareCount,bOptimize);
                rareThread.start();
            }
            SearchExistsThread []thread = new SearchExistsThread[bruteThreads];
            for (int i = 0; i < bruteThreads; i++)
            {
                thread[i] = new SearchExistsThread(i,curReg,g,exp[curReg],i,0,false);
                thread[i].start();
            }
            try {
                for (int i = 0; i < bruteThreads; i++)
                {
                    thread[i].join();
                }
                SearchExistsThread.bStop = true;
                if (rareCount > 0) rareThread.join();
            } catch (Exception e)
            {
                System.err.println("Join error: "+e.toString());
            }
            if (SearchExistsThread.bFound) numFound++;
        }
        
        long time = System.currentTimeMillis()-startTime;
        String dauer = Globals.TimeText(time);
        
        Writer.WriteLine("\nFinished! Time: "+dauer+" Found: "+numFound+"/"+exp.length+" ("+(numFound*100/exp.length)+"%)");
        Writer.WriteLine("Execution details: Rare success="+SearchExistsThread.numRare+", Brute force success="+SearchExistsThread.numBruteForce);
        Writer.WriteLine("Rejected: Rare="+SearchExistsThread.rejectRare+", bruteForce="+(exp.length-numFound-SearchExistsThread.rejectRare));
    }
    
    
    
    // Analysis method #3: Find all paths in the graph
    public static void RunAllPaths(File regFile,int rareCount,boolean bOptimize)
    {
        RegExp []exp = ParseRegExpFile(regFile);
        if (exp == null) return;
        Writer.WriteLine("Starting analysis: find all given path in the graph...");
        
        Graph g = Globals.graph;
        AllPaths.g = g;
        long numFound = 0, allFound = 0, numRare = 0, overall = 0;
        //AllPaths.found = new short[g.numNodes][g.numNodes];
        Progress.StartProgress(exp.length);
        
        int numThreads = Globals.GetNumThreads();
        numThreads = 3;
        int slices = 4;
        Writer.WriteLine("rareCount="+rareCount+", optimize="+bOptimize+" --- RareThreads="+(rareCount>0?1:0)+", BruteForceThreads="+numThreads+", SlicesPerThread="+slices);
        
        AllPaths.resultBruteForce = new ArrayList[numThreads];
        AllPaths.nodeState = new SearchExistsThread.NodeState[numThreads][g.numNodes];
        for (int n = 0; n < numThreads; n++)
        {
            for (int i = 0; i < g.numNodes; i++)
            {
                AllPaths.nodeState[n][i] = new SearchExistsThread.NodeState();
            }
        }
        AllPaths.parts = new SearchExistsThread.AnalysisPart[numThreads*slices];
        for (int i = 0; i < numThreads*slices; i++)
        {
            AllPaths.parts[i] = new SearchExistsThread.AnalysisPart(i*g.numNodes/numThreads/slices,(i<numThreads*slices-1?(i+1)*g.numNodes/numThreads/slices:g.numNodes));
        }

        long startTime = System.currentTimeMillis();
        //System.out.println("Nodes: "+g.numNodes);
        
        for (int curReg = 0; curReg < exp.length; curReg++)
        {
            Progress.SetValue(curReg+1);
            AllPaths.re = exp[curReg];
            System.out.print("\n"+curReg+": ");
            long startms = System.currentTimeMillis();
            
            AllPaths.bStop = false;
            AllPaths.bRare = false;
            AllPaths.numRaresFound = 0;
            //AllPaths.resultRare = new ArrayList<int[]>(64);
            //AllPaths.sort = new SortedArray();
            
            for (int i = 0; i < numThreads*slices; i++)
            {
                AllPaths.parts[i].bProcessed = false;
            }
            AllPaths rareThread = null;
            if (rareCount > 0)
            {
                rareThread = new AllPaths(rareCount,bOptimize,curReg+1);
                rareThread.start();
            }
            AllPaths []thread = new AllPaths[numThreads];
            for (int i = 0; i < numThreads; i++)
            {
                AllPaths.resultBruteForce[i] = new ArrayList<int[]>(64);
                thread[i] = new AllPaths(i,curReg);
            }
            for (int i = 0; i < numThreads; i++)
            {
                thread[i].start();
            }
            try {
                for (int i = 0; i < numThreads; i++)
                {
                    thread[i].join();
                }
                AllPaths.bStop = true;
                if (rareCount > 0)
                {
                    rareThread.join(1000);
                    if (rareThread.isAlive())
                    {
                        rareThread.join();
                    }
                }
            } catch (Exception e)
            {
                System.err.println("Join error: "+e.toString());
            }
            
            int sumBrute = 0;
            for (int i = 0; i < numThreads; i++) sumBrute += AllPaths.resultBruteForce[i].size();
            if (AllPaths.bRare)
            {
                numRare++;
                long curFound = AllPaths.numRaresFound; //AllPaths.resultRare.size();
                allFound += curFound;
                overall += curFound + sumBrute;
                if (curFound > 0)
                {
                    numFound++;
                }
                System.out.print(" ["+curFound+"] ");
            } else {
                allFound += sumBrute;
                overall += sumBrute + AllPaths.numRaresFound; //AllPaths.resultRare.size();
                if (sumBrute > 0) numFound++;
                System.out.print(" <"+sumBrute+"> ");
            }
            System.out.print((long)(System.currentTimeMillis()-startms)+"ms.");
        }
        
        long time = System.currentTimeMillis()-startTime;
        String dauer = Globals.TimeText(time);
        
        Writer.WriteLine("\nFinished! Time: "+dauer+". Pairs Found: "+allFound+" -- "+numFound+"/"+exp.length+" ("+(numFound*100/exp.length)+"%)");
        Writer.WriteLine("Finished by Rare: "+numRare+"; BruteForce: "+(exp.length-numRare)+"; overall results found: "+overall);
    }
    
    
    
    
    private static RegExp[] ParseRegExpFile(File regFile)
    {
        ArrayList<String> inputReg = new ArrayList<String>();
        String line;
        int numReg = 0;
        RegExp []exp;

        try {
            BufferedReader file = new BufferedReader(new FileReader(regFile));

            do {
                line = file.readLine();
                if (line != null)
                {
                    line = line.trim();
                    if (line.length() == 0) continue;

                    if ((line.charAt(0) == '<') || (line.charAt(0) == '[') || (line.charAt(0) == '('))
                    {
                        inputReg.add(line);
                        numReg++;
                    }
                }
            } while (line != null);
            file.close();
        }
        catch (Exception e)
        {
            Writer.WriteLine("Error (Expression #"+(numReg)+"): "+e.toString());
            e.printStackTrace();
            return null;
        }

        if (numReg == 0)
        {
            Writer.WriteLine("\nAutomated Execution: Invalid regular expression file found!");
            return null;
        }

        Writer.WriteLine("\nAutomated Execution: Parsing "+numReg+" regular expressions...");

        exp = new RegExp[numReg];
        for (int i = 0; i < numReg; i++)
        {
            exp[i] = new RegExp(inputReg.get(i));
            if (!exp[i].Parse(false)) return null;
        }

        Writer.Write("done. ");
        return exp;
    }
    
    
    public static boolean AnyPathFound(RegExp.ResultWay paths[][])
    {
        for (int j = 0; j < paths.length; j++)
        {
            for (int k = 0; k < paths[j].length; k++)
            {
                if (paths[j][k] != null)
                {
                    return true;
                }
            }
        }
        return false;
    }
}





class SearchExistsThread extends Thread
{
    private int threadNo;
    private int regNo;
    private Graph g;
    private RegExp re;
    private int partNum;
    private int rareCount;
    private boolean bOptimize;

    public static NodeState [][]nodeState;
    
    public static AnalysisPart []parts;
    public static boolean bFound;
    public static boolean bStop;
    
    public static int numBruteForce;
    public static int numRare;
    public static int rejectRare;
    public static int numRareThreads;
    
    public static class AnalysisPart
    {
        public int startNum;
        public int endNum;
        public boolean bProcessed;
        
        AnalysisPart(int start,int end)
        {
            startNum = start;
            endNum = end;
            bProcessed = false;
        }
    }
    
    public static class NodeState
    {
        public int visited = -1;
        public SortedArray visitStates = null;
        public int backVisited = -1;
        public SortedArray backVisitStates = null;
        public int added = -1;
        
        public void AddVisited(int lauf,RegExp.State state)
        {
            if (visited != lauf)
            {
                visitStates = new SortedArray(Node.CycleHash(state.pos.orderNum,state.pathNum),state);
                visited = lauf;
            }
            else visitStates.Insert(Node.CycleHash(state.pos.orderNum,state.pathNum),state);
        }
        
        public boolean CycleCheck(int lauf,Item state,int pathNum)
        {
            if (visited == lauf)
            {
                return (!visitStates.Contains(Node.CycleHash(state.orderNum, pathNum)));
            }
            return true; // no cycle
        }

    }
    
    
    SearchExistsThread(int number,int regN,Graph ng,RegExp nre,int initialPart,int nRare,boolean nOpt)
    {
        threadNo = number;
        regNo = regN;
        g = ng;
        re = nre;
        rareCount = nRare;
        bOptimize = nOpt;
        partNum = initialPart;

        if (initialPart >= 0)
        {
            parts[partNum].bProcessed = true;
        }
    }
    
    public void run()
    {
        if (partNum >= 0) runBruteForce();
        else runRare();
    }

    public void runBruteForce()
    {
        boolean bContinue;
        
        do {
            for (int i = parts[partNum].startNum; i < parts[partNum].endNum; i++)
            {
                if (SearchSingleExists(g,re,i))
                {
                    bStop = true;
                    synchronized(this)
                    {
                        if (!bFound)
                        {
                            bFound = true;
                            numBruteForce++;
                        }
                    }
                    return;
                }
                if (bStop) return;
            }
            bContinue = false;
            for (int i = partNum+1; i < parts.length; i++)
            {
                if (!parts[i].bProcessed)
                {
                    synchronized(this)
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
    }
    
    
    private boolean SearchSingleExists(Graph g,RegExp re,int startNode)
    {
        // Search the shortest path fulfilling re from the start node.
        // Only search paths until any path has been found.
        
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
            
            // Check for completed path
            if (curState.pos.bEndsExpression || bStop)
            {
                return !bStop;
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
                return false;
            }
        }
    }


    public void runRare()
    {
        if (bStop) return;
        
        // 1) Calculate lists of start/end nodes for rare labels
        RareSet rares = RareSet.CalcRares(g,re,rareCount,bOptimize,true);
        if (rares == null) return;
        if (bStop)
        {
            return;
        }



        // 2) Calculate all paths between the rare states (if numRares > 1)
        //    If there is no path between any two rare states, abort search.
        RegExp.ResultWay intPaths[][][] = null;
        if (rares.numRares > 1)
        {
            //System.err.println("TODO: include aborting; search for "exists" only.");
            
            intPaths = new RegExp.ResultWay[rares.numRares-1][][];
            for (int i = 0; i < rares.numRares-1; i++)
            {
                // ... (exists only)
                intPaths[i] = re.SearchNtoM(rares.endNodes[i].length,rares.endNodes[i],
                        rares.state[i].item,true,rares.startNodes[i+1].length,rares.startNodes[i+1],rares.state[i+1].item);

                if (bStop)
                {
                    return;
                }
                
                // Check if a path was found (quit search otherwise)
                if (!Analysis.AnyPathFound(intPaths[i]))
                {
                    bStop = true;
                    rejectRare++;
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
            return;
        }
        
        // Check for negative results
        if ((numStartNodes == 0) || (numEndNodes == 0)) 
        {
            rejectRare++;
            bStop = true;
            return;
        }



        // 4a. Calculate all shortest paths from first rare nodes backwards
        //     (If the first rare state is not a start state)
        
        // Initialize Multi-threaded implementation
        RareStartEndSearch.g = g;
        RareStartEndSearch.re = re;
        RareStartEndSearch.regNo = regNo;
        boolean startPaths[] = null;
        if (!rares.state[0].item.bBackEnds)
        {
            RareStartEndSearch.startNodes = startNodes;
            RareStartEndSearch.startState = rares.state[0].item;
            RareStartEndSearch.bForward = false;
            RareStartEndSearch.result = new boolean[numStartNodes];
            
            int nThreads = Math.min(numRareThreads,numStartNodes);
            RareStartEndSearch []thread = new RareStartEndSearch[nThreads];
            for (int i = 0; i < nThreads; i++)
                thread[i] = new RareStartEndSearch(i,i*numStartNodes/nThreads,(i<nThreads-1?(i+1)*numStartNodes/nThreads:numStartNodes));
            for (int i = 0; i < nThreads; i++)
                thread[i].start();
            
            try {            
                for (int i = 0; i < nThreads; i++)
                    thread[i].join();
            } catch (Exception e)
            {
                System.err.println("Join error: "+e);
            }
            startPaths = RareStartEndSearch.result;
        }
        if (bStop)
        {
            return;
        }
        
        // 4b. Calculate all paths from last rare nodes to the end
        //     (If the last rare state is not a finish state)
        boolean endPaths[] = null;
        if (!rares.state[rares.numRares-1].item.bEndsExpression)
        {
            RareStartEndSearch.startNodes = endNodes;
            RareStartEndSearch.startState = rares.state[rares.numRares-1].item;
            RareStartEndSearch.bForward = true;
            RareStartEndSearch.result = new boolean[numEndNodes];
            
            int nThreads = Math.min(numRareThreads,numEndNodes);
            RareStartEndSearch []thread = new RareStartEndSearch[nThreads];
            for (int i = 0; i < nThreads; i++)
                thread[i] = new RareStartEndSearch(i,i*numEndNodes/nThreads,(i<nThreads-1?(i+1)*numEndNodes/nThreads:numEndNodes));
            for (int i = 0; i < nThreads; i++)
                thread[i].start();
            
            try {            
                for (int i = 0; i < nThreads; i++)
                    thread[i].join();
            } catch (Exception e)
            {
                System.err.println("Join error: "+e);
            }
            endPaths = RareStartEndSearch.result;
        }
        if (bStop)
        {
            return;
        }

        
        // Intercept length-one regular expressions
        if ((startPaths == null) && (endPaths == null))
        {
            bStop = true;
            synchronized(this)
            {
                if (!bFound)
                {
                    bFound = true;
                    numRare++;
                }
            }
            return;
        }
        
        
        // 5. search a path through the results
        //    There is no need to really search, we just need three fitting true entires
        boolean bF = false;
        for (int i = 0; i < numStartNodes; i++)
        {
            if ((startPaths != null) && (!startPaths[i])) continue;
            if (endPaths == null)
            {
                // All end nodes finish, and every start node is connected to at least one end node
                bF = true;
                break;
            }
            
            // Check all corresponding end nodes
            for (int j = 0; j < numEndNodes; j++)
            {
                if (wayBetween[i][j])
                {
                    if (endPaths[j])
                    {
                        bF = true;
                        break;
                    }
                }
            }
            if (bF) break;
        }
        bStop = true;

        
        if (bF)
        {
            synchronized(this)
            {
                if (!bFound)
                {
                    bFound = true;
                    numRare++;
                }
            }
        } else {
            rejectRare++;
        }
    }
    
    
}



class RareStartEndSearch extends Thread
{
    public static SearchExistsThread.NodeState [][]nodeState;
 
    public static int []startNodes;
    public static boolean bForward;
    public static Graph g;
    public static RegExp re;
    public static int regNo;
    public static Item startState;
    
    public static boolean []result;
    
    private int startNum;
    private int endNum;
    private int threadNo;
    
    public RareStartEndSearch(int thread,int s,int e)
    {
        threadNo = thread;
        startNum = s;
        endNum = e;
    }
    
    public void run()
    {
        for (int i = startNum; i < endNum; i++)
        {
            result[i] = SearchOneWay_Exists(startNodes[i]);
            if (SearchExistsThread.bStop) return;
        }
    }
    
    // nacheinander suchen - gleichzeitig bringt keinen Vorteil 
    private boolean SearchOneWay_Exists(int startNode)
    {
        // Assertions: startState != root/eof, numStartNodes > 0
        
        ArrayList<RegExp.State> fifo = new ArrayList<RegExp.State>(64);
        int ffPos = 0;
        int numTransitions = 0;
        RegExp.State curState;
        int unique =  2*regNo*g.numNodes + (bForward?g.numNodes:0) + startNode;
        
        // Initialize start position
        curState = new RegExp.State(startNode,startState,null,0);
        fifo.add(curState);
        nodeState[threadNo][startNode].AddVisited(unique,curState);
        ffPos++;

        // one-way search
        while (true)
        {
            numTransitions++;

            if (SearchExistsThread.bStop) return false;

            // Check for completed path
            if (curState.pos.EndsExpression(bForward))
            {
                return true;
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
                        if (nodeState[threadNo][neuNode].CycleCheck(unique,next[j],curState.pathNum))
                        {
                            RegExp.State st = new RegExp.State(neuNode,next[j],curState,curState.pathNum);
                            fifo.add(st);
                            nodeState[threadNo][neuNode].AddVisited(unique,st);
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
                return false;
            }
        }
    }

}
