/****************************************************/
/* Graph - loading and storing a graph in memory 	*/
/*													*/
/* Diplomarbeit - Andr� Koschmieder, 2009			*/
/****************************************************/


import java.util.*; // Hashtable
import java.io.*; // File


public class Graph implements Comparator<int[]>
{
	// Properties of the current Graph
	int numNodes;
	int numEdges;
	Node []node;

	private boolean bUseNodeLabels;
	private Hashtable<String,Integer> nodeNames;
	private Hashtable<String,Integer> nodeLabels;
	private Hashtable<String,Integer> edgeLabels;

	// Status variables
	private int [][]edgeLabelDistribution;
	private int lastSingleEdgeLabel;
	private int [][]nodeLabelDistribution;
	private int lastSingleNodeLabel;

	// Index by edge labels
	private ArrayList<ArrayList<Integer>> edgeStartNode;
	private ArrayList<ArrayList<Integer>> edgeEndNode;


	private Graph(int nNodes,int nEdges,ArrayList<int[]> orgEdges,Hashtable<String,Integer> edgeHash,Hashtable<String,Integer> nodeHash,boolean bLabels,Hashtable<String,Integer> nodeLabelHash,ArrayList<Integer> labelList)
	{
		numNodes = nNodes;
		numEdges = nEdges;
		node = new Node[numNodes];

		bUseNodeLabels = bLabels;
		edgeLabels = edgeHash;
		nodeNames = nodeHash;
		nodeLabels = nodeLabelHash;

		int s = edgeLabels.size();
		edgeLabelDistribution = new int[s][];
		edgeStartNode = new ArrayList<ArrayList<Integer>>(s);
		edgeEndNode = new ArrayList<ArrayList<Integer>>(s);
		for (int i = 0; i < s; i++)
		{
			edgeLabelDistribution[i] = new int[2];
			edgeLabelDistribution[i][0] = 0;
			edgeLabelDistribution[i][1] = i;
			edgeStartNode.add(new ArrayList<Integer>(4));
			edgeEndNode.add(new ArrayList<Integer>(4));
		}
		if (bUseNodeLabels)
		{
			s = nodeLabels.size();
			nodeLabelDistribution = new int[s][2];
			for (int i = 0; i < s; i++)
			{
				nodeLabelDistribution[i] = new int[2];
				nodeLabelDistribution[i][0] = 0;
				nodeLabelDistribution[i][1] = i;
			}
		}
		for (int i = 0; i < numNodes; i++)
		{
			if (bUseNodeLabels)
			{
				int j = labelList.get(i);
				node[i] = new Node(j);
				nodeLabelDistribution[j][0]++;
			}
			else node[i] = new Node(i);
		}
		for (int i = 0; i < numEdges; i++)
		{
			//if ((i+1)%1000 == 0)
            //{
            //    if ((i+1)%100000 == 0) System.out.println(".");
            //    else if ((i+1)%10000 == 0) System.out.print(". ");
            //    else System.out.print(".");
            //}
			int []data = orgEdges.get(i);
			node[data[0]].AddOut(data[1],data[2]);
			node[data[1]].AddIn(data[0],data[2]);

			edgeLabelDistribution[data[2]][0]++;
			edgeStartNode.get(data[2]).add(data[0]);
			edgeEndNode.get(data[2]).add(data[1]);
		}
		//System.out.println("!");
		for (int i = 0; i < numNodes; i++)
		{
			node[i].FinalizeDataStructure();
		}

		Arrays.sort(edgeLabelDistribution,this);
		for (lastSingleEdgeLabel = 0; lastSingleEdgeLabel < edgeLabelDistribution.length && edgeLabelDistribution[lastSingleEdgeLabel][0] <= 1; lastSingleEdgeLabel++) ;
		if (bUseNodeLabels)
		{
			Arrays.sort(nodeLabelDistribution,this);
			for (lastSingleNodeLabel = 0; lastSingleNodeLabel < nodeLabelDistribution.length && nodeLabelDistribution[lastSingleNodeLabel][0] <= 1; lastSingleNodeLabel++) ;
		}
	}

	public int compare(int []e1,int []e2)
	{
		if (e1[0] < e2[0]) return -1;
		if (e1[0] > e2[0]) return 1;
		return 0;
	}

	public boolean equals(int []o)
	{
		return false; // not used
	}

	public void PrintEdgeDistribution()
	{
		/*if (Writer.bVerbose)
		{
			Writer.WriteLine("Edge Labels with maximum usage:");
			for (int i = edgeLabelDistribution.length-1; i >= edgeLabelDistribution.length-5  && i >= 0; i--)
			//for (int i = 0; i < 5; i++)
			{
				Writer.WriteLine((edgeLabelDistribution.length-i)+". "+edgeLabelDistribution[i][0]+" - ("+edgeLabelDistribution[i][1]+") "+GetEdgeString(edgeLabelDistribution[i][1]));
			}
			//if (edgeLabelDistribution.length > 5) Writer.WriteLine("...");
		}*/

		Writer.WriteLine("Number of edge labels: "+edgeLabelDistribution.length+"; avg Usage: "+((double)numEdges)/((double)edgeLabelDistribution.length));
		Writer.WriteLine("Maximum usage: "+edgeLabelDistribution[edgeLabelDistribution.length-1][0]+"; minimum usage: "+edgeLabelDistribution[0][0]);
		Writer.WriteLine("Number of edge labels with single usage: "+lastSingleEdgeLabel);
	}

	public long GetHashSize(Hashtable<String,Integer> hash)
	{
		if (hash == null) return 0;
		long s = 32;
		Set<Map.Entry<String,Integer>> set = edgeLabels.entrySet();
		Iterator<Map.Entry<String,Integer>> it = set.iterator();
		while (it.hasNext())
		{
			Map.Entry<String,Integer> en = it.next();
			s += 32 + en.getKey().length() + 1;
		}
		return s;
	}
    
    public int GetNumEdgeLabels()
    {
        return edgeLabels.size();
    }

	public void PrintMemoryUsage()
	{
		long memStatic = 88 + 24*edgeLabels.size();
		if (bUseNodeLabels) memStatic += 24*nodeLabels.size();
		long memLabels = GetHashSize(nodeNames) + GetHashSize(nodeLabels) + GetHashSize(edgeLabels);
		long memGraph = 0;
		for (int i = 0; i < numNodes; i++)
		{
			memGraph += node[i].GetMemoryUsage();
		}

		Writer.WriteLine("Static Graph Memory usage: "+Globals.MemText(memStatic+memLabels+memGraph));
		Writer.WriteLine("General: "+Globals.MemText(memStatic)+"; Labels: "+Globals.MemText(memLabels)+"; Graph representation: "+Globals.MemText(memGraph));
	}

	public int GetNodeName(String node)
	{
		if (nodeNames.containsKey(node)) return nodeNames.get(node);
		return -1;
	}

	public int GetNodeLabel(String nodeLabel)
	{
		if (!bUseNodeLabels) return GetNodeName(nodeLabel);
		if (nodeLabels.containsKey(nodeLabel)) return nodeLabels.get(nodeLabel);
		return -1;
	}

	public int GetEdgeLabel(String label)
	{
		if (edgeLabels.containsKey(label)) return edgeLabels.get(label);
		return -1;
	}

	public int IsRareNode(int label,int rare)
	{
		if (bUseNodeLabels)
		{
			for (int i = 0; i < nodeLabelDistribution.length && nodeLabelDistribution[i][0] <= rare; i++)
			{
				if (nodeLabelDistribution[i][1] == label) return nodeLabelDistribution[i][0];
			}
			return 0;
		}
		return 1; // without labels, every node is unique
	}

	public int IsRareEdge(int label,int rare)
	{
		if (edgeStartNode.get(label).size() <= rare) return edgeStartNode.get(label).size();
		return 0;
	}

	// Helper function
	private int[] AddIntToSet(int []intSet,int newInt)
	{
		if (intSet == null) return new int[]{newInt};
		int []neu = new int[intSet.length+1];
		for (int i = 0; i < intSet.length; i++)
		{
			// If the new item is already in the set, do not add again
			// -- removed, currently we need all items in the set
			//if (intSet[i] == newInt) return intSet;
			neu[i] = intSet[i];
		}
		neu[intSet.length] = newInt;
		return neu;
	}

	public int[] GetNodesByLabel(int nodeLabel)
	{
		if (!bUseNodeLabels) return new int[]{nodeLabel};
		int []res = null;
		for (int i = 0; i < numNodes; i++)
		{
			if (node[i].nodeLabel == nodeLabel) res = AddIntToSet(res,i);
		}
		return res;
	}

	public int[] GetEdgeStartNodes(int edgeLabel)
	{
		int s = edgeStartNode.get(edgeLabel).size();
		int []ret = new int[s];
		for (int i = 0; i < s; i++) ret[i] = edgeStartNode.get(edgeLabel).get(i);
		return ret;
	}

	public int[] GetEdgeEndNodes(int edgeLabel)
	{
		int s = edgeEndNode.get(edgeLabel).size();
		int []ret = new int[s];
		for (int i = 0; i < s; i++) ret[i] = edgeEndNode.get(edgeLabel).get(i);
		return ret;
	}

	public String GetNodeName(int node)
	{
		Set<Map.Entry<String,Integer>> set;
		set = nodeNames.entrySet();
		Iterator<Map.Entry<String,Integer>> it = set.iterator();
		while (it.hasNext())
		{
			Map.Entry<String,Integer> en = it.next();
			if (en.getValue() == node)
			{
				return en.getKey();
			}
		}
		return null;
	}

	public String GetNodeLabel(int label)
	{
		if (!bUseNodeLabels) return GetNodeName(label);
		Set<Map.Entry<String,Integer>> set;
		set = nodeLabels.entrySet();
		Iterator<Map.Entry<String,Integer>> it = set.iterator();
		while (it.hasNext())
		{
			Map.Entry<String,Integer> en = it.next();
			if (en.getValue() == label)
			{
				return en.getKey();
			}
		}
		return null;
	}

	public String GetEdgeString(int edge)
	{
		Set<Map.Entry<String,Integer>> set = edgeLabels.entrySet();
		Iterator<Map.Entry<String,Integer>> it = set.iterator();
		while (it.hasNext())
		{
			Map.Entry<String,Integer> en = it.next();
			if (en.getValue() == edge)
			{
				return en.getKey();
			}
		}
		return null;
	}

	public String GetRandomNode()
	{
		double d = Math.random() * numNodes;
		return GetNodeName((int)d);
	}

	public int GetRandomNodeNumber()
	{
		return (int)(Math.random() * (double)numNodes);
	}


	public void ExportGraphDistribution(File filename)
	{
		Writer.WriteLine("\nCalculating graph distribution data...");

		// Node degree: Put all degrees in an array and sort.
		int []nodeDegree = new int[numNodes];

		for (int i = 0; i < numNodes; i++)
		{
			nodeDegree[i] = node[i].numInEdges + node[i].numOutEdges;
		}
		Arrays.sort(nodeDegree);

		// Edge label degree: same procedure
		int s = edgeLabels.size();
		int []labelDegree = new int[s];

		for  (int i = 0; i < s; i++)
		{
			labelDegree[i] = edgeStartNode.get(i).size();
		}
		//Arrays.sort(labelDegree);

		Writer.WriteLine("Writing distribution file...");

		try {
			FileWriter file = new FileWriter(filename);

			file.write("% Node degree distribution\r\n");
			for (int i = 0; i < numNodes; i++)
			{
				file.write(nodeDegree[numNodes-i-1]+"\r\n");
			}

			file.write("\r\n% Edge label distribution: label: occurrence\r\n");
			for (int i = 0; i < s; i++)
			{
				//file.write(labelDegree[s-i-1]+"\r\n");
			    file.write(i+": "+labelDegree[i]+"\r\n");
			}

			file.close();
		}
		catch (Exception e)
    	{
      		Writer.WriteLine("Error: "+e.toString());
      		e.printStackTrace();
      		return;
    	}
    	Writer.WriteLine("Finished!");
	}


	public void PrintGraphStatistics()
	{
		Writer.WriteLine("\nGraph Statistics\n=================");
		Writer.WriteLine("Nodes: "+numNodes+"; Edges: "+numEdges);
		if (bUseNodeLabels)
		{
			Writer.WriteLine("Number of node labels: "+nodeLabelDistribution.length+"; avg Usage: "+((double)numNodes)/((double)nodeLabelDistribution.length));
			Writer.WriteLine("Maximum usage: "+nodeLabelDistribution[nodeLabelDistribution.length-1][0]+"; minimum usage: "+nodeLabelDistribution[0][0]);
			Writer.WriteLine("Number of node labels with single usage: "+lastSingleNodeLabel);
			Writer.WriteLine("Nodes distribution:");
		}
		else Writer.WriteLine("Nodes distribution: (no node labels loaded)");

		int maxOut = 0, maxIn = 0;
		for (int i = 0; i < numNodes; i++)
		{
			if (node[i].numOutEdges > maxOut) maxOut = node[i].numOutEdges;
			if (node[i].numInEdges > maxIn) maxIn = node[i].numInEdges;
		}
		double avgOut = ((double)numEdges)/((double)numNodes);
		Writer.WriteLine("Average node degree: "+avgOut+"; max out: "+maxOut+"; max in: "+maxIn);

		PrintEdgeDistribution();
		PrintMemoryUsage();
	}


	private void WriteBrackets(FileWriter file) throws Exception
	{
		int lastLabel = -1, l=-2;
		int curNode, curEdge;
		int len = (int)(Math.random() * 10 + 1);
		if (len == 1) len = 2;
		if (len > 5) len = (len/2);

		file.write("(");
		for (int k = 0; k < len; k++)
		{
			do {
				do {
					curNode = (int)(Math.random() * numNodes);
				} while ((node[curNode].numOutEdges <= 0));
				curEdge = (int)(Math.random() * node[curNode].numOutEdges);
				l = node[curNode].outLabel[curEdge];
			} while ((l == lastLabel) || (GetEdgeString(l).equals(">")));

			int typ = (int)(Math.random() * 20);
			if (typ == 0)
			{
				WriteBrackets(file);
				typ = (int)(Math.random() * 15 +5);
				lastLabel = -1;
			}
			else {
				file.write("<<"+GetEdgeString(l)+">>");
				if (typ > 10) lastLabel = l;
			}
			if (typ > 10)
			{
				if (typ < 14) file.write("+");
				else if (typ < 17) file.write("*");
				else file.write("?");
			} else lastLabel = -1;
		}
		file.write(")");
	}

	public boolean CreateRegExpFile(File filename)
	{
		int lastLabel = -1;
		int l=-2;

		/*Writer.WriteLine("Creating RegExp file...");
		boolean bRet = CreateRareQueryFile(filename);
		Writer.WriteLine("Finished!");
		return bRet;*/
		
		try {
			FileWriter file = new FileWriter(filename);
			int curNode, curEdge, len, typ;

			for (int lauf = 0; lauf < 10; lauf++)
			{
				// 0. completely random
				// 1.-10. at least one rare-1 to rare-10
				for (int i = 0; i < 1000; i++)
				{
					len = (int)(Math.random() * 20 + 1);
					if (len == 1) len = 2;
					if (len >= 10) len = (len/2-3);

					for (int k = 0; k < len; k++)
					{
						do {
							curNode = (int)(Math.random() * numNodes);
						} while (node[curNode].numOutEdges <= 0);
						if (k == 0) file.write("[["+GetNodeName(curNode)+"]]");

						if ((lauf > 0) && (k > 0) && (k == len/2))
						{
							// Rare edge
							boolean bFound = false;
							do {
								do {
									curNode = (int)(Math.random() * numNodes);
								} while (node[curNode].numOutEdges <= 0);
								for (int p = 0; p < node[curNode].numOutEdges; p++)
								{
									l = node[curNode].outLabel[p];
									if ((IsRareEdge(l,lauf) > 0) && (!GetEdgeString(l).equals(">")))
									{
										file.write("<<"+GetEdgeString(l)+">>");
										bFound = true;
										break;
									}
								}
							} while (!bFound);
							lastLabel = -1;
						}
						else {
							typ = (int)(Math.random() * 20);
							if (typ == 0)
							{
								WriteBrackets(file);
								typ = (int)(Math.random() * 15 +5);
								lastLabel = -1;
							}
							else {
								do {
									curEdge = (int)(Math.random() * node[curNode].numOutEdges);
									l = node[curNode].outLabel[curEdge];
									if (l == lastLabel)
									{
										do {
											curNode = (int)(Math.random() * numNodes);
										} while (node[curNode].numOutEdges <= 0);
									}
								} while ((l == lastLabel) || (GetEdgeString(l).equals(">")));
								file.write("<<"+GetEdgeString(l)+">>");
								lastLabel = l;
							}
							if (typ > 10)
							{
								if (typ < 14) file.write("+");
								else if (typ < 17) file.write("*");
								else file.write("?");

							}
						}
					}
					file.write("\r\n");
				}
			}

			file.close();
		}
		catch (Exception e)
    	{
      		Writer.WriteLine("Error: "+e.toString());
      		e.printStackTrace();
      		return false;
    	}
    	Writer.WriteLine("Finished!");
    	return true;
	}
	
	private int GetUniqueRandom(int min,int max,int []reserved)
	{
	    int res = 0, count = 0;
	    boolean bOK;
	    
	    do {
	        res = (int)(Math.random() * (max-min+1)) + min;
	        bOK = true;
	        if (reserved != null)
	        {
	            for (int j = 0; j < reserved.length; j++)
	            {
	                if (res == reserved[j])
	                {
	                    bOK = false;
	                    count++;
	                    break;
	                }
	            }
	            if (count > reserved.length*10) return -1;
	        }
	    } while (!bOK);
	    return res;
	}
	
	
	private boolean CreateRareQueryFile(File filename)
	{
        try {
            FileWriter file = new FileWriter(filename);
            
            // Set parameters
            int numRare = 2;
            int rareCount = 2;
            int qLengthMin = 3;
            int qLengthMax = 8;
            int probPlus = 300; // 30%
            int probStar = 500;
            int probQuest = 100;
            
            // Find maximum rare label number
            int maxRareNum = 0;
            while (edgeLabelDistribution[maxRareNum][0] <= rareCount) maxRareNum++;
            
            for (int i = 0; i < 1000; i++)
            {
                // Create random query according to fixed parameters
                
                // 1. Query Length
                int curLength = GetUniqueRandom(qLengthMin,qLengthMax,null);
                int []label = new int[curLength];
                for (int j = 0; j < curLength; j++) label[j] = -1;
                
                // 2. Position of rare labels
                int []rarePos = new int[numRare];
                for (int j = 0; j < numRare; j++) rarePos[j] = -1;
                for (int j = 0; j < numRare; j++) rarePos[j] = GetUniqueRandom(0,curLength-1,rarePos);
                Arrays.sort(rarePos);
                
                // 3. All labels from start to end
                int rPos = 0;
                for (int n = 0; n < curLength; n++)
                {
                    if ((rPos < numRare) && (n == rarePos[rPos]))
                    {
                        // 3a. Create rare label
                        boolean bOK;
                        do {
                            label[n] = edgeLabelDistribution[(int)(Math.random()*maxRareNum)][1];
                            bOK = true;
                            if ((n > 0) && (label[n] == label[n-1])) bOK = false;
                            /*for (int j = 0; j < n; j++)
                                if (label[n] == label[j])
                                    bOK = false;*/
                        } while (!bOK);
                        file.write("<<"+GetEdgeString(label[n])+">>");
                        // Add modifier
                        int mod = (int)(Math.random() * 1000);
                        if (mod < probPlus) file.write("+");
                        rPos++;
                    } else {
                        // 3b. Create standard label
                        int curNode;
                        do {
                            curNode = (int)(Math.random() * numNodes);
                        } while (node[curNode].numOutEdges <= 0);
                        boolean bOK;
                        do {
                            //label[n] = node[curNode].outLabel[(int)(Math.random()*node[curNode].numOutEdges)];
                            label[n] = edgeLabelDistribution[(int)(Math.random()*4)+edgeLabelDistribution.length-4][1];
                            bOK = true;
                            if ((n > 0) && (label[n] == label[n-1])) bOK = false;
                            /*for (int j = 0; j < n; j++)
                                if (label[n] == label[j])
                                    bOK = false;*/
                        } while (!bOK);
                        file.write("<<"+GetEdgeString(label[n])+">>");
                        int mod = (int)(Math.random() * 1000);
                        if (mod < probPlus) file.write("+");
                        else if (mod < probPlus+probStar) file.write("*");
                        else if (mod < probPlus+probStar+probQuest) file.write("?");
                    }
                }
                file.write("\r\n");
            }
            file.close();
        }
        catch (Exception e)
        {
            Writer.WriteLine("Error: "+e.toString());
            e.printStackTrace();
            return false;
        }
	    return true;
	}


	/////////////////////////////////
	// Static Graph-Loading Methods
	/////////////////////////////////

	public static Graph LoadFromEdgeFile(File srcFile)
	{
		Writer.Clear();
		Writer.WriteLine("Loading Graph into memory... please wait! (free: "+Globals.MemText(Runtime.getRuntime().freeMemory())+", max: "+Globals.MemText(Runtime.getRuntime().maxMemory())+", total: "+Globals.MemText(Runtime.getRuntime().totalMemory())+")");

		ArrayList<int[]> orgEdges = new ArrayList<int[]>(1024);
		Hashtable<String,Integer> edgeHash = new Hashtable<String,Integer>(1024);
		Hashtable<String,Integer> nodeHash = new Hashtable<String,Integer>(1024);
		String line;
		String []s = new String[4];
		int []n = new int[3];
		int numNodes = 0, numEdges = 0, numLabels = 0;
		int i;
		long startTime = System.currentTimeMillis();

		try {
			BufferedReader file = new BufferedReader(new FileReader(srcFile));

			do {
				line = file.readLine();
				if (line != null)
				{
					line = line.trim();
					if ((line.equals("")) || (line.charAt(0) == ';')) continue;

					s = SplitLine(line);

					// Add node names and edge label to hashtables
					for (i = 0; i < 2; i++)
					{
						if (nodeHash.containsKey(s[i])) n[i] = nodeHash.get(s[i]);
						else {
							n[i] = numNodes++;
							nodeHash.put(s[i],n[i]);
						}
					}
					if (s[2] == null) s[2] = "";
					if (edgeHash.containsKey(s[2])) n[2] = edgeHash.get(s[2]);
					else {
						n[2] = numLabels++;
						edgeHash.put(s[2],n[2]);
					}
					orgEdges.add(new int[]{n[0],n[1],n[2]});
					numEdges++;
                    
                    /*if (numEdges%1000 == 0)
                    {
                        if (numEdges%100000 == 0) System.out.println(".");
                        else if (numEdges%10000 == 0) System.out.print(". ");
                        else System.out.print(".");
                    }*/
				}
			} while (line != null);
			file.close();
		}
		catch (Exception e)
    	{
      		Writer.WriteLine("Error (Edge "+(numEdges+1)+"): "+e.toString());
      		e.printStackTrace();
      		return null;
    	}

    	Writer.WriteLine("\nCreating memory structure for "+(numNodes)+" nodes and "+numEdges+" edges...");
		long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		Graph g = new Graph(numNodes,numEdges,orgEdges,edgeHash,nodeHash,false,null,null);

		long endTime = System.currentTimeMillis();
		long mem2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		Writer.WriteLine("Successfully loaded the graph into memory: ~"+Globals.MemText(mem2-mem1)+" ("+Globals.TimeText(endTime-startTime)+")");

		return g;
	}




	public static Graph LoadFromNodeAndEdgeFile(File nodeFile,File edgeFile)
	{
		Writer.Clear();
		Writer.WriteLine("Loading Graph into memory... please wait! (free: "+Globals.MemText(Runtime.getRuntime().freeMemory())+", max: "+Globals.MemText(Runtime.getRuntime().maxMemory())+", total: "+Globals.MemText(Runtime.getRuntime().totalMemory())+")");

		ArrayList<Integer> orgNodes = new ArrayList<Integer>(1024);
		ArrayList<int[]> orgEdges = new ArrayList<int[]>(1024);
		Hashtable<String,Integer> edgeHash = new Hashtable<String,Integer>(1024);
		Hashtable<String,Integer> nodeHash = new Hashtable<String,Integer>(1024);
		Hashtable<String,Integer> nodeLabels = new Hashtable<String,Integer>(1024);
		String line;
		String []s = new String[4];
		int []n = new int[3];
		int numNodes = 0, numEdges = 0, numEdgeLabels = 0, numNodeLabels = 0;
		int i;
		long startTime = System.currentTimeMillis();

		try {

			// Read node file
			BufferedReader file = new BufferedReader(new FileReader(nodeFile));

			do {
				line = file.readLine();
				if (line != null)
				{
					line = line.trim();
					if ((line.equals("")) || (line.charAt(0) == ';')) continue;

					// One node consists of node name and node label
					s = SplitLine(line);
					if (s[1] == null) throw new Exception("node label missing!");

					// Save node, name and label
					if (nodeHash.containsKey(s[0])) throw new Exception("duplicate node name found: "+s[0]);
					else {
						n[0] = numNodes++;
						nodeHash.put(s[0],n[0]);
					}
					if (nodeLabels.containsKey(s[1])) n[1] = nodeLabels.get(s[1]);
					else {
						n[1] = numNodeLabels++;
						nodeLabels.put(s[1],n[1]);
					}
					// node number is implicitly the position in the array list
					orgNodes.add(new Integer(n[1]));
				}
			} while (line != null);
			file.close();
		}
		catch (Exception e)
    	{
      		Writer.WriteLine("Error (Node "+(numNodes+1)+"): "+e.toString());
      		e.printStackTrace();
      		return null;
    	}

		try {
			// Read edge file
			BufferedReader file = new BufferedReader(new FileReader(edgeFile));

			do {
				line = file.readLine();
				if (line != null)
				{
					line = line.trim();
					if ((line.equals("")) || (line.charAt(0) == ';')) continue;

					s = SplitLine(line);

					// Check node names and add edge label to hashtables
					for (i = 0; i < 2; i++)
					{
						if (nodeHash.containsKey(s[i])) n[i] = nodeHash.get(s[i]);
						else throw new Exception("Unknown node name found: "+s[i]);
					}
					if (s[2] == null) s[2] = "";
					if (edgeHash.containsKey(s[2])) n[2] = edgeHash.get(s[2]);
					else {
						n[2] = numEdgeLabels++;
						edgeHash.put(s[2],n[2]);
					}
					orgEdges.add(new int[]{n[0],n[1],n[2]});
					numEdges++;
				}
			} while (line != null);
			file.close();
		}
		catch (Exception e)
    	{
      		Writer.WriteLine("Error (Edge "+(numEdges+1)+"): "+e.toString());
      		e.printStackTrace();
      		return null;
    	}

    	Writer.WriteLine("Creating memory structure for "+(numNodes)+" nodes and "+numEdges+" edges...");
		long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		Graph g = new Graph(numNodes,numEdges,orgEdges,edgeHash,nodeHash,true,nodeLabels,orgNodes);

		long endTime = System.currentTimeMillis();
		long mem2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		Writer.WriteLine("Successfully loaded the graph into memory: ~"+Globals.MemText(mem2-mem1)+" ("+Globals.TimeText(endTime-startTime)+")");

		return g;
	}


	// Split one line into seperate strings. Strings are seperated by widespaces
	// and can be contained in quotation marks to include widespaces.
	private static String[] SplitLine(String line) throws Exception
	{
		String []s = new String[]{null,null,null,null};
		int num = 0;
		int i = 0, j, k;
		boolean bCont;

		do {
			while ((line.charAt(i) == ' ') || (line.charAt(i) == '\t')) i++;
			s[num] = "";

			if (line.charAt(i) == '"')
			{
				// String is contained in quotation marks; "" is translated into single "
				i++;
				do
				{
					bCont = false;
					j = line.indexOf('"',i);
					if (j > i)
					{
						s[num] += line.substring(i,j);
						if ((line.length()-1 > j) && (line.charAt(j+1) == '"'))
						{
							s[num] += "\"";
							i = j+2;
							bCont = true;
						} else i = j+1;
					} else {
						i++;
						if (j < 0) throw new Exception("End of label missing (quotation mark)");
					}
				} while (bCont);
			} else {
				// Simple string, read until widespace
				j = line.indexOf(' ',i);
				k = line.indexOf('\t',i);
				if ((k >= 0) && ((k < j) || (j < 0))) j = k;
				if (j > 0)
				{
					s[num] = line.substring(i,j);
					i = j+1;
				}
				else {
					s[num] = line.substring(i);
					i = line.length();
				}
			}

			while ((i < line.length()) && ((line.charAt(i) == ' ') || (line.charAt(i) == '\t'))) i++;
			if (i < line.length()) num++;
		} while ((i < line.length()) && (num < 5));

		return s;
	}

}
