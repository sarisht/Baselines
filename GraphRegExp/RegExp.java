/****************************************************/
/* RegExp - store regular expression 				*/
/*			includes all search algorithms			*/
/*													*/
/* Diplomarbeit - Andr� Koschmieder, 2009			*/
/****************************************************/



import java.util.*; // ArrayList
import java.io.*; // File etc.


public class RegExp
{
	// Regular Expression properties
	public String expString;
	public Item root=null;
	public Item eof=null;

	private Unique []mandatoryUnique=null;

	public static int numLauf=0;
	public static int globalTransitions;
	public static int globalFifo;
	public static int numRealSearch;
	public static int numTmp;

	// Temporary variables for parsing
	private int pos, numStates=1;
	private boolean bCurOr=false;

	// Status class
	public static class State
	{
		public int node;
		public Item pos;
		public State prev;
		public int pathNum;
        public int pathLength;

		public State(int n,Item p,State previous,int pNum)
		{
			node = n;
			pos = p;
			prev = previous;
			pathNum = pNum;
		}
	}

	public static class ResultWay
	{
		boolean bFound = false;
		String string = "";
		int wayLength = 0;

		int nodesChecked = 0;
		int fifoSize = 0;
	}

	public static class Unique
	{
		public Item item;
		public int count;

		public Unique(Item i,int c)
		{
			item = i;
			count = c;
		}
	}

	public static class JoinResult
	{
        public int numResults = 0;
		public int[] destNode = null;
		public Item[][] destItem = null;
	}


	public RegExp(String exp)
	{
		expString = exp.trim();
	}

	private void CalcFirstFollow()
	{
		root.CalcFirst(1);
		eof.CalcFollow(null,false);
		root.CalcBackFollow(null,false);
	}


	public boolean Parse(boolean bShowInfos)
	{
		if (bShowInfos) Writer.WriteLine("\nParsing the regular expression...");
		numStates = 0;

		try {
			if (expString.equals("")) throw new Exception("Empty regular expression found.");
			pos = 0;
			root = new Item(Item.TYP_START,0,Item.DEST_NONE,null,null);
			ProceedParse(null);
		}
		catch (Exception e)
		{
			Writer.WriteLine("Parse Error (Col "+pos+"): "+e.toString());
      		e.printStackTrace();
			root = null;
			return false;
		}
		CalcFirstFollow();
		if (bShowInfos) Writer.WriteLine("Finished! Created "+numStates+" states.");
		if (bShowInfos && Writer.bVerbose) root.PrintExtended(0);
		return true;
	}

	private void ProceedParse(Item parent) throws Exception
	{
		while ((expString.charAt(pos) == ' ') || (expString.charAt(pos) == '\t')) pos++;
		Item cur = null;

		while ((pos < expString.length()) && (expString.charAt(pos) != ')'))
		{
			char curChar = expString.charAt(pos);

			if (curChar == '(')
			{
				cur = CreateItem(Item.TYP_BRACKET,null,Item.DEST_NODE,parent,cur);
				pos++;
				if (cur.typ == Item.TYP_OR) ProceedParse(cur.GetLastOr());
				else ProceedParse(cur);
			}
			else if (curChar == '*')
			{
				if ((cur != null) && (cur.typ == Item.TYP_OR) && (!bCurOr))
					cur.AddOrModifier(Item.MOD_STAR);
				else {
					if ((cur == null) || (cur.modifier != Item.MOD_NONE) || (bCurOr))
						throw new Exception("Invalid regular expression:\nunexpected '*' at col "+(pos+1));
					cur.modifier = Item.MOD_STAR;
				}
				pos++;
			}
			else if (curChar == '+')
			{
				if ((cur != null) && (cur.typ == Item.TYP_OR) && (!bCurOr))
					cur.AddOrModifier(Item.MOD_PLUS);
				else {
					if ((cur == null) || (cur.modifier != Item.MOD_NONE) || (bCurOr))
						throw new Exception("Invalid regular expression:\nunexpected '+' at col "+(pos+1));
					cur.modifier = Item.MOD_PLUS;
				}
				pos++;
			}
			else if (curChar == '?')
			{
				if ((cur != null) && (cur.typ == Item.TYP_OR) && (!bCurOr))
					cur.AddOrModifier(Item.MOD_QUESTION);
				else {
					if ((cur == null) || (cur.modifier != Item.MOD_NONE) || (bCurOr))
						throw new Exception("Invalid regular expression:\nunexpected '?' at col "+(pos+1));
					cur.modifier = Item.MOD_QUESTION;
				}
				pos++;
			}
			else if (curChar == '|')
			{
				if (cur == null) throw new Exception("Invalid regular expression:\nunexpected '|' at col "+(pos+1));
				bCurOr = true;
				pos++;
			}
			else if (curChar == '<')
			{
				pos++;
				if (expString.charAt(pos) != '<') throw new Exception("Invalid regular expression:\nunexpected '<' at col "+(pos+1)+"; use name notation <<edge label>> [[node label]].");
				pos++;
				int j = expString.indexOf(">>",pos);
				if (j < 0) throw new Exception("Invalid regular expression:\nfound no matching '>>' for edge name starting at col "+(pos+1));
				String name = expString.substring(pos,j);
				if (name.equals(".")) cur = CreateItem(Item.TYP_RANDOM,null,Item.DEST_EDGE,parent,cur);
				else cur = CreateItem(Item.TYP_SYMBOL,name,Item.DEST_EDGE,parent,cur);
				pos = j+2;
			}
			else if (curChar == '[')
			{
				pos++;
				if (expString.charAt(pos) != '[') throw new Exception("Invalid regular expression:\nunexpected '[' at col "+(pos+1)+"; use name notation <<edge label>> [[node label]].");
				pos++;
				int j = expString.indexOf("]]",pos);
				if (j < 0) throw new Exception("Invalid regular expression:\nfound no matching ']]' for node name starting at col "+(pos+1));
				String name = expString.substring(pos,j);
				if (name.equals(".")) cur = CreateItem(Item.TYP_RANDOM,null,Item.DEST_NODE,parent,cur);
				else cur = CreateItem(Item.TYP_SYMBOL,name,Item.DEST_NODE,parent,cur);
				pos = j+2;
			}
			else throw new Exception("Invalid regular expression:\nunexpected '"+curChar+"' at col "+(pos+1)+"; use name notation <<edge label>> [[node label]].");


			while ((pos < expString.length()) && ((expString.charAt(pos) == ' ') || (expString.charAt(pos) == '\t'))) pos++;
		}

		if (pos == expString.length())
		{
			if ((parent != null) || (cur == null) || (bCurOr))
				throw new Exception("Invalid regular expression: unexpected end of expression");
			cur = CreateItem(Item.TYP_END,null,Item.DEST_NONE,parent,cur);
		}
		else if (expString.charAt(pos) == ')')
		{
			if ((parent == null) || (cur == null) || (bCurOr)) throw new Exception("Invalid regular expression: unexpected ')' at col "+(pos+1));
			if (parent.typ != Item.TYP_BRACKET) throw new Exception("Invalid regular expression: found ')' without matching '(' at col "+(pos+1));
			if (parent.lastChild == null) parent.lastChild = cur;
			pos++;
		}
	}


	private Item CreateItem(int typ,String s,int dest,Item parent,Item cur) throws Exception
	{
		// Match name with hashtable
		int name = 0;
		if (s != null)
		{
			if (dest == Item.DEST_EDGE) name = Globals.graph.GetEdgeLabel(s);
			else if (dest == Item.DEST_NODE) name = Globals.graph.GetNodeLabel(s);
			if (name < 0) throw new Exception("Unknown label found: "+s);
		}

		// Create new item
		Item tmp;
		if (bCurOr)
		{
			if (cur.typ != Item.TYP_OR) numStates++;
			cur.AddOr(typ,name,dest);
			numStates++;
			bCurOr = false;
			tmp = cur;
		}
		else {
			tmp = new Item(typ,name,dest,parent,cur);
			numStates++;
			if (cur != null) cur.next = tmp;
			else if (parent != null)
			{
				if (parent.child == null) parent.child = tmp;
			}
			else
			{
				root.next = tmp;
				tmp.prev = root;
			}
			if (typ == Item.TYP_END) eof = tmp;
		}
		return tmp;
	}
	
	public class StatAggregation
	{
	    public int numNodes;
	    public int numEdges;
	    public ArrayList<Integer> labels = new ArrayList<Integer>();
	    public ArrayList<Integer> endNodes = new ArrayList<Integer>();
	}
	
	
	/*public void ExportRegExp(FileWriter file) throws IOException
	{
	    // 1. Gather summary information
	    StatAggregation stats = root.ExportCycleRecursive(false,null);
	    file.write(numStates+" nodes "+stats.numNodes+"\n");
	    file.write(stats.numEdges+" edges\n");
	    file.write(stats.labels.size()+" labels\n");
	}
	
	public static void ExportRegExpToLiske(RegExp []reg)
	{
	    try {
	        FileWriter file = new FileWriter("regExpList.txt");
	        
	        for (int i = 0; i < reg.length; i++)
	        {
	            String fName = "regExp/reg"+i+".graph";
	            file.write(fName);
	            FileWriter regFile = new FileWriter(fName);
	            
	            reg[i].ExportRegExp(regFile);
	            
	            regFile.close();
	        }
	        
	        file.close();
	    }
	    catch (Exception e)
	    {
	        Writer.WriteLine("Error: "+e.toString());
	        e.printStackTrace();
        }
	}*/


	/*public static void RunAutomation(File regFile,int rareCount,boolean bOptimize)
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
      		return;
    	}

		if (numReg == 0)
		{
			Writer.WriteLine("\nAutomated Execution: Invalid regular expression file found!");
			return;
		}

		Writer.WriteLine("\nAutomated Execution: Parsing "+numReg+" regular expressions...");

		exp = new RegExp[numReg];
		for (int i = 0; i < numReg; i++)
		{
			exp[i] = new RegExp(inputReg.get(i));
			if (!exp[i].Parse(false)) return;
		}

		Writer.Write("Parsing complete. Executing tree searches ");
		if (bOptimize) Writer.WriteLine("(optimized rare calculation)...");
		else if (rareCount > 0) Writer.WriteLine("(rare count: "+rareCount+")... ");
		else Writer.WriteLine("(without optimization)...");

		int numFound=0, numTransitions=0, numFifo=0;
		int succTrans=0, succFifo=0;
		long startTime = System.currentTimeMillis();
		numRealSearch = 0;
		int largestFifo = 0;

		/*Graph g = Globals.graph;
		int csFound, csSucc, csNon;
		long csTime=startTime, csLastTime;*/

		//for (int i = 0; i < numReg; i++)
		//{
			/*
			** implementation for testing purposes:
			** search every path from every node in the graph
			** to simulate Liske's behavior

			csFound = csSucc = csNon = 0;
			csLastTime = csTime;
			for (int n = 0; n < g.numNodes; n++)
			{
				csFound++;
				if (exp[i].ExecQuery(n,-1,rareCount,bOptimize,false))
				{
					numFound++;
					succTrans += globalTransitions;
					succFifo += globalFifo;
					csSucc += globalTransitions;
					break;
				} else csNon += globalTransitions;

				numTransitions += globalTransitions;
				numFifo += globalFifo;
				if (globalFifo > largestFifo) largestFifo = globalFifo;

				if (n%10000 == 0) System.out.print(".");
			}

			csTime = System.currentTimeMillis();
			long time = csTime-csLastTime;
			String dauer = (time/1000)+"."+((time%1000)/100)+""+((time%100)/10)+""+(time%10)+"s";
			System.out.print(String.format("\nQuery "+(i+1)+": "+csFound+"; SUCC=%.2f; NON=%.2f; time="+dauer,((double)csSucc),((double)csNon/(double)(csFound-1))));
			*/
            
            //boolean bRes = AllPaths.ExecQuery(Globals.graph, exp[i], rareCount, bOptimize, false, i);
            //if (bRes) numFound++;

			/*int startNode = Globals.graph.GetRandomNodeNumber();
			String e = inputReg.get(i);
			if ((e.charAt(0) == '[') && (e.charAt(1) == '['))
			{
				int j = e.indexOf("]]",1);
				int k = Globals.graph.GetNodeName(e.substring(2,j));
				if (k >= 0) startNode = k;
			}

			if (Writer.bVerbose) Writer.Write((i+1)+": ");
			if (exp[i].ExecQuery(startNode,-1,rareCount,bOptimize,false))
			{
				numFound++;
				succTrans += globalTransitions;
				succFifo += globalFifo;
			}
			numTransitions += globalTransitions;
			numFifo += globalFifo;
			if (globalFifo > largestFifo) largestFifo = globalFifo;*/
		/*}

		long time = System.currentTimeMillis()-startTime;
		String dauer = (time/1000)+"."+((time%1000)/100)+""+((time%100)/10)+""+(time%10)+"s";

		Writer.WriteLine("\nFinished! Time: "+dauer+" Found: "+numFound+"/"+numReg+" ("+(numFound*100/numReg)+"%)");
		Writer.WriteLine("Search calls: "+numRealSearch+"; Transitions: "+globalTransitions+"; Fifo: "+globalFifo+"; max: "+largestFifo);
		Writer.WriteLine("Success: "+(numFound*100/numReg)+"%: Transitions: "+succTrans+"; Fifo: "+succFifo);
		//double dSuccTr = (double)succTrans / (double)numFound;
		//double dSuccFi = (double)succFifo / (double)numFound;
		//double dUnsuTr = (double)(numTransitions-succTrans) / (double)(numReg-numFound);
		//double dUnsuFi = (double)(numFifo-succFifo) / (double)(numReg-numFound);
		//Writer.WriteLine(String.format("Success: Trans/call: %.2f; Fifo/call: %.2f; NOT Success: Trans/call: %.2f; Fifo/call: %.2f.",dSuccTr,dSuccFi,dUnsuTr,dUnsuFi));
        Writer.WriteLine("More than 1 rare: "+AllPaths.numMoreThanOne+", BruteForce: "+AllPaths.numBruteForce);//+", NumMergeJoinCalls: "+Follow.numMergeJoins+", number of result labels: "+Follow.numMergeResults+", avg labels per join: "+(double)Follow.numMergedLabels/(double)Follow.numMergeJoins);
	}*/



	public ResultWay CreateWay(State state,boolean bForward,boolean bTwoWay,State dest,int numTransitions,int ffSize)
	{
		ResultWay res = new ResultWay();

		// Compute statistics for the path
		if (bTwoWay) res.nodesChecked = 2*numTransitions - (bForward?1:0);
		else res.nodesChecked = numTransitions;
		res.fifoSize = ffSize;
		if (state.pos == null) return res;

		// Create the path string
		for (State t = state; t != null; t = t.prev)
		{
			if (t.pos.dest == Item.DEST_NODE) continue;
			String w = "";
			if (t.prev != null) w = "<"+t.pos.SymbolString()+">";
			if (bForward) res.string = w + " ["+t.node+"] " + res.string;
			else res.string += " ["+t.node+"] " + w ;
			res.wayLength++;
		}
		if (bTwoWay)
		{
			for (State t = dest; t != null; t = t.prev)
			{
				if (t.pos.dest == Item.DEST_NODE) continue;
				String w = "";
				if (t.prev != null) w = "<"+t.pos.SymbolString()+">";
				if (bForward)
				{
					if ((t == dest) || ((t == dest.prev) && (dest.node == t.node)))
						res.string += w;
					else res.string += " ["+t.node+"] " + w ;
				}
				else {
					if ((t == dest) || ((t == dest.prev) && (dest.node == t.node)))
						res.string = w + res.string;
					else res.string = w + " ["+t.node+"] " + res.string;
				}
				res.wayLength++;
			}
		}
		return res;
	}

    
    public Unique[] GetRareStates(int rareCount,boolean bOptimize)
    {
        mandatoryUnique = null;
        if (bOptimize)
        {
            // Optimization of maximum path length.
            // First, get all possible rare labels (up to a predefined maximum).
            // Then construct the rare list by adding one rare item at a time.
            mandatoryUnique = root.GetMandatoryUniques(100000);
            if (mandatoryUnique != null)
            {
                int numInitialUniques = mandatoryUnique.length;
                SortedArray uniques = new SortedArray();
                for (int i = 0; i < numInitialUniques; i++)
                {
                    uniques.Insert(mandatoryUnique[i].count, i);
                }
                SortedArray used = new SortedArray();
                
                int curNum = 0; // current position in uniques array
                int curLength = 0; // current path length
                while ((curNum < numInitialUniques) && (curLength < rareCount)) 
                {
                    // Add the next rare item to the list
                    used.Insert((Integer)uniques.GetAt(curNum), uniques.GetKeyAt(curNum));
                    
                    // Calculate the number of paths with current rare list
                    curLength = (Integer)used.GetAt(0); // start item
                    for (int i = 0; i < used.Size()-1; i++)
                        curLength += (Integer)used.GetAt(i) * (Integer)used.GetAt(i+1);
                    curLength += (Integer)used.GetAt(used.Size()-1); // end item
                    
                    curNum++;
                }
                if (curLength > rareCount)
                {
                    curNum--;
                    used.Remove((Integer)(uniques.GetAt(curNum)));
                }
                if (used.Size() < 1) mandatoryUnique = null;
                else {
                    Unique nUnique[] = new Unique[curNum];
                    for (int i = 0; i < curNum; i++)
                    {
                        nUnique[i] = new Unique(mandatoryUnique[used.GetKeyAt(i)].item,mandatoryUnique[(Integer)used.GetKeyAt(i)].count);
                    }
                    mandatoryUnique = nUnique;
                }
            }
            
        } else {
            if (rareCount > 0) mandatoryUnique = root.GetMandatoryUniques(rareCount);
        }
        return mandatoryUnique;
    }


	public boolean ExecQuery(int start,int end,int rareCount,boolean bOptimize,boolean bWriteInfos)
	{
		if (bWriteInfos)
		{
			Writer.Write("\nExecuting tree search algorithm ");
			if (bOptimize) Writer.WriteLine("with optimized rare labels...");
			else if (rareCount <= 0) Writer.WriteLine(" without optimization...");
			else Writer.WriteLine(" with fixed rare labels ("+rareCount+")...");
		}

		boolean bFixedEnd = (end>=0);

		mandatoryUnique = GetRareStates(rareCount, bOptimize);
		if (mandatoryUnique == null) rareCount = 0;
		/*if (rareCount > 0) //System.out.print(mandatoryUnique.length+" ");
        {
		    System.out.print("(");
            for (int i = 0; i < mandatoryUnique.length; i++)
            {
                if (i > 0) System.out.print(",");
                System.out.print(mandatoryUnique[i].count);
            }
            System.out.print(") ");
        }*/

		int numSearches = 1;
		if (rareCount > 0) numSearches += mandatoryUnique.length;
		int [][][]wayNode = new int[numSearches][2][];
		Item [][]wayState = new Item[numSearches][2];

		// Gather start and end nodes for all search cycles
		for (int i = 0; i < numSearches; i++)
		{
			// set start nodes
			if (i == 0)
			{
				wayNode[0][0] = new int[]{start};
				wayState[0][0] = root;
			}
			else {
				if (mandatoryUnique[i-1].item.dest == Item.DEST_NODE)
					wayNode[i][0] = Globals.graph.GetNodesByLabel(mandatoryUnique[i-1].item.symbol);
				else wayNode[i][0] = Globals.graph.GetEdgeEndNodes(mandatoryUnique[i-1].item.symbol);
				wayState[i][0] = mandatoryUnique[i-1].item;
			}

			// set destination nodes
			if (i == numSearches-1)
			{
				wayNode[i][1] = new int[]{end};
				wayState[i][1] = eof;
			}
			else {
				if (mandatoryUnique[i].item.dest == Item.DEST_NODE)
					wayNode[i][1] = Globals.graph.GetNodesByLabel(mandatoryUnique[i].item.symbol);
				else wayNode[i][1] = Globals.graph.GetEdgeStartNodes(mandatoryUnique[i].item.symbol);
				wayState[i][1] = mandatoryUnique[i].item;
			}
		}

		if (bWriteInfos && (numSearches > 1)) Writer.WriteLine("Splitting search in "+numSearches+" parts.");

		ResultWay [][][]resultWay = new ResultWay[numSearches][][];
		boolean bFound = true;
		globalTransitions = 0;
		globalFifo = 0;
		long startTime = System.currentTimeMillis();

		// Sort the search
		int [][]sorted = new int[numSearches][2];
		for (int i = 0; i < numSearches; i++)
		{
			sorted[i][0] = wayNode[i][0].length * (bFixedEnd||(i<numSearches-1)?wayNode[i][1].length:2);
			sorted[i][1] = i;
		}
		if (numSearches > 2)
		{
			Arrays.sort(sorted,Globals.graph);
		}

		// Run all search parts consecutively
		for (int i = 0; i < numSearches; i++)
		{
			int curI = sorted[i][1]; // use sorted order

			// Verbose output
			if (bWriteInfos && (numSearches > 1))
			{
				Writer.Write("Part "+(curI+1)+": [");
				for (int j = 0; j < wayNode[curI][0].length; j++)
				{
					if (j > 0) Writer.Write(",");
					Writer.Write(wayNode[curI][0][j]+"");
				}
				Writer.Write("] -> "+((bFixedEnd||(curI<numSearches-1))?"[":"(open end)"));
				if (bFixedEnd || (curI < numSearches-1))
				{
					for (int j = 0; j < wayNode[curI][1].length; j++)
					{
						if (j > 0) Writer.Write(",");
						Writer.Write(wayNode[curI][1][j]+"");
					}
					Writer.Write("]");
				}
				Writer.WriteLine("");
			}

			// Run search
			resultWay[curI] = SearchNtoM(wayNode[curI][0].length,wayNode[curI][0],wayState[curI][0],
							bFixedEnd||(curI<numSearches-1),wayNode[curI][1].length,wayNode[curI][1],wayState[curI][1]);

			// Debug output
			if (Writer.bVerbose && bWriteInfos)
			{
				for (int ss = 0; ss < wayNode[curI][0].length; ss++)
				{
					for (int ee = 0; ee < wayNode[curI][1].length; ee++)
					{
						if (resultWay[curI][ss][ee] == null) Writer.Write("n ");
						else Writer.Write("j ");
					}
					Writer.WriteLine("");
				}
			}

			// Check if we can stop the search at this point
			if (numSearches > 1)
			{
				boolean bStop = true;
				int numEnd = (bFixedEnd||(curI<numSearches-1))?wayNode[curI][1].length:1;
				for (int j = 0; (j < wayNode[curI][0].length && bStop); j++)
				{
					for (int k = 0; k < numEnd; k++)
					{
						if (resultWay[curI][j][k] != null)
						{
							bStop = false;
							break;
						}
					}
				}
				if (bStop)
				{
					bFound = false;
					break;
				}
			}
		}

		// Search shortest path within the results
		ResultWay []destWay=null;
		if (numSearches > 1)
		{
			if (bFound)
			{
				destWay = SearchResultPath(resultWay,wayNode);
				if (destWay == null) bFound = false;
			}
		}
		else {
			if (resultWay[0][0][0] == null) bFound = false;
			else destWay = new ResultWay[]{resultWay[0][0][0]};
		}

		long time = System.currentTimeMillis()-startTime;
		String dauer = (time/1000)+"."+((time%1000)/100)+""+((time%100)/10)+""+(time%10)+"s";

		// Print results
		if (bFound)
		{
			if (bWriteInfos||Writer.bVerbose) Writer.WriteLine("Found! Time: "+dauer+"; Transitions: "+globalTransitions+"; Fifo: "+globalFifo);
			if (bWriteInfos)
			{
				for (int i = 0; i < numSearches; i++)
				{
					if (i > 0) Writer.WriteLine(" -->");
					if ((destWay[i].string.length() < 2) || (destWay[i].string.charAt(1) != '['))
						Writer.Write("["+mandatoryUnique[i-1].item.symbol+"] ");
					Writer.Write(destWay[i].string);
					if ((destWay[i].string.length() > 2) && (destWay[i].string.charAt(destWay[i].string.length()-2) != ']'))
						Writer.Write(" ["+mandatoryUnique[i].item.symbol+"]");
				}
				Writer.WriteLine("");
			}

		}
		else {
			if (bWriteInfos||Writer.bVerbose) Writer.WriteLine("Not found! Time: "+dauer+"; Transitions: "+globalTransitions+"; Fifo: "+globalFifo);
		}
		return bFound;
	}


	// Search shortest paths from all start nodes to all end nodes.
	// The algorithm searches all paths at the same time, so using only
	// n+m complexity instead of n*m.
	public ResultWay[][] SearchNtoM(int numStart,int []startNode,Item startState,
									 boolean bFixedEnd,int numEnd,int []endNode,Item endState)
	{
		// Create result data structure
		if (!bFixedEnd) numEnd = 1;
		ResultWay [][]res = new ResultWay[numStart][numEnd];
		numRealSearch++;

		// Intercept 0-length paths
		if ((!bFixedEnd) && (numStart == 1) && (startState.bEndsExpression))
		{
			res[0][0] = CreateWay(new State(startNode[0],startState,null,0),true,false,null,0,0);
			return res;
		}
		if ((bFixedEnd) && (numStart == 1) && (numEnd == 1) && (startNode[0] == endNode[0]))
		{
			if (startState.Meets(endState))
			{
				res[0][0] = CreateWay(new State(startNode[0],startState,null,0),true,false,null,0,0);
				return res;
			}
		}

		// Initialize search variables
		numLauf++;
		ArrayList<State> fifo = new ArrayList<State>(1024);
		ArrayList<State> backFifo = new ArrayList<State>(1024);
		int ffPos = 0, backffPos = 0;
		int numTransitions = 0;
		Node curNode, curBackNode=null;
		Item curItem, curBackItem=null;
		State []dest;
		Item[] next, nodeNext;
		State curState = null, curBackState = null;
		boolean []bStartFinished = new boolean[numStart];
		boolean []bEndFinished = new boolean[numEnd];
		boolean bFinished;
		//JoinResult join = null;
		Graph g = Globals.graph;

		// Initialize search positions
		for (int s = 0; s < numStart; s++)
		{
			curState = new State(startNode[s],startState,null,s);
			fifo.add(curState);
			g.node[startNode[s]].AddVisited(numLauf,curState);

			// Try to match the start node with the regular expression
			nodeNext = startState.MatchNode(g.node[startNode[s]].nodeLabel);
			if (nodeNext != null)
			{
				for (int k = 0; k < nodeNext.length; k++)
				{
					State sta = new State(startNode[s],nodeNext[k],curState,s);
					fifo.add(sta);
					g.node[startNode[s]].AddVisited(numLauf,sta);
				}
		 	}
		}
		if (bFixedEnd)
		{
			for (int e = 0; e < numEnd; e++)
			{
				curBackState = new State(endNode[e],endState,null,e);
				backFifo.add(curBackState);
				g.node[endNode[e]].AddBackVisited(numLauf,curBackState);

				// Try to match the end node with the regular expression
				nodeNext = endState.MatchBackNode(g.node[endNode[e]].nodeLabel);
				if (nodeNext != null)
				{
					for (int k = 0; k < nodeNext.length; k++)
					{
						State sta = new State(endNode[e],nodeNext[k],curBackState,e);
						backFifo.add(sta);
						g.node[endNode[e]].AddBackVisited(numLauf,sta);
					}
				}
			}
		}

		// Prepare first iteration
		curState = fifo.get(ffPos);
		curNode = g.node[curState.node];
		curItem = curState.pos;
		ffPos++;
		if (bFixedEnd)
		{
			curBackState = backFifo.get(backffPos);
			curBackNode = g.node[curBackState.node];
			curBackItem = curBackState.pos;
			backffPos++;
		}

		// Run 2-way search algorithm
		while (true)
		{
			numTransitions++;

			// Part 1: forward search (1 step)
			if (!bStartFinished[curState.pathNum]) // not yet all paths found for start node
			{
				// 1.1 - Check for completed paths
				if (!bFixedEnd)
				{
					if (curItem.bEndsExpression)
					{
						res[curState.pathNum][0] = CreateWay(curState,true,bFixedEnd,null,numTransitions,0);
						bStartFinished[curState.pathNum] = true;
					}
				} else {
					dest = curNode.FinishCheck(numLauf,curItem);
					if (dest != null)
					{
						// One or more paths found
						for (int i = 0; i < dest.length; i++)
						{
							// Only use if no path for that start->dest has been found
							if (res[curState.pathNum][dest[i].pathNum] == null)
							{
								res[curState.pathNum][dest[i].pathNum] = CreateWay(curState,true,bFixedEnd,dest[i],numTransitions,0);
								if (EndFinished(res,dest[i].pathNum)) bEndFinished[dest[i].pathNum] = true;
							}
						}
						if (StartFinished(res,curState.pathNum)) bStartFinished[curState.pathNum] = true;
					}
				}
			}
			if (!bStartFinished[curState.pathNum])
			{

				// 1.2 - Make one step forward
				for (int i = 0; i < curNode.numOutEdges; i++)
				{
					// Look at all outgoing edges and match them with the regular expression
					next = curItem.MatchEdge(curNode.outLabel[i]);
					if (next != null)
					{
						for (int j = 0; j < next.length; j++)
						{
							// If there is no cycle, step to the next node
							if (g.node[curNode.outNode[i]].CycleCheck(numLauf,next[j],curState.pathNum))
							{
								State st = new State(curNode.outNode[i],next[j],curState,curState.pathNum);
								fifo.add(st);
								g.node[curNode.outNode[i]].AddVisited(numLauf,st);

								// Try to match the node with the regular expression
								nodeNext = next[j].MatchNode(g.node[curNode.outNode[i]].nodeLabel);
								if (nodeNext != null)
								{
									for (int k = 0; k < nodeNext.length; k++)
									{
										if (g.node[curNode.outNode[i]].CycleCheck(numLauf,nodeNext[k],curState.pathNum))
										{
											State sta = new State(curNode.outNode[i],nodeNext[k],st,curState.pathNum);
											fifo.add(sta);
											g.node[curNode.outNode[i]].AddVisited(numLauf,sta);
										}
									}
								}
							}
						}
					}
				}



				/*
				** alternative implementation: use merge join
				** (does not execute faster than standard version)

				// 1.2 - Make one step forward
				join = curItem.JoinEdgesForward(curNode);
				if (join != null) // At least one path was found
				{
					// Cycle through all combinations of next node / next states
					for (int i = 0; i < join.destNode.size(); i++)
					{
						int nextNode = join.destNode.get(i);
						Item []nextStates = join.destItem.get(i);
						for (int j = 0; j < nextStates.length; j++)
						{
							// If there is no cycle, step to the next node
							if (g.node[nextNode].CycleCheck(numLauf,nextStates[j],curState.pathNum))
							{
								State st = new State(nextNode,nextStates[j],curState,curState.pathNum);
								fifo.add(st);
								g.node[nextNode].AddVisited(numLauf,st);

								// Try to match the node with the regular expression
								nodeNext = nextStates[j].MatchNode(g.node[nextNode].nodeLabel);
								if (nodeNext != null)
								{
									for (int k = 0; k < nodeNext.length; k++)
									{
										if (g.node[nextNode].CycleCheck(numLauf,nodeNext[k],curState.pathNum))
										{
											State sta = new State(nextNode,nodeNext[k],st,curState.pathNum);
											fifo.add(sta);
											g.node[nextNode].AddVisited(numLauf,sta);
										}
									}
								}
							}
						}
					}
				}*/
			}


			// Part 2: backward search (1 step)
			if (bFixedEnd)
			{
				if (!bEndFinished[curBackState.pathNum]) // not yet all paths found for end node
				{
					// 2.1 - Check for completed paths
					dest = curBackNode.BackFinishCheck(numLauf,curBackItem);
					if (dest != null)
					{
						// One or more paths found
						for (int i = 0; i < dest.length; i++)
						{
							// Only use if no path for that start->dest has been found
							if (res[dest[i].pathNum][curBackState.pathNum] == null)
							{
								res[dest[i].pathNum][curBackState.pathNum] = CreateWay(curBackState,false,bFixedEnd,dest[i],numTransitions,0);
								if (StartFinished(res,dest[i].pathNum)) bStartFinished[dest[i].pathNum] = true;
							}
							if (EndFinished(res,curBackState.pathNum)) bEndFinished[curBackState.pathNum] = true;
						}
					}
				}
				if (!bEndFinished[curBackState.pathNum])
				{

					// 2.2 - Make one step backward
					for (int i = 0; i < curBackNode.numInEdges; i++)
					{
						// Look at all incoming edges and match them with the regular expression
						next = curBackItem.MatchBackEdge(curBackNode.inLabel[i]);
						if (next != null)
						{
							for (int j = 0; j < next.length; j++)
							{
								// If there is no cycle, step to the previous node
								if (g.node[curBackNode.inNode[i]].BackCycleCheck(numLauf,next[j],curBackState.pathNum))
								{
									State st = new State(curBackNode.inNode[i],next[j],curBackState,curBackState.pathNum);
									backFifo.add(st);
									g.node[curBackNode.inNode[i]].AddBackVisited(numLauf,st);

									// Try to match the node with the regular expression
									nodeNext = next[j].MatchBackNode(g.node[curBackNode.inNode[i]].nodeLabel);
									if (nodeNext != null)
									{
										for (int k = 0; k < nodeNext.length; k++)
										{
											if (g.node[curBackNode.inNode[i]].BackCycleCheck(numLauf,nodeNext[k],curBackState.pathNum))
											{
												State sta = new State(curBackNode.inNode[i],nodeNext[k],st,curBackState.pathNum);
												backFifo.add(sta);
												g.node[curBackNode.inNode[i]].AddBackVisited(numLauf,sta);
											}
										}
									}
								}
							}
						}
					}



					/*
					** alternative implementation: use merge join
					** (does not execute faster than standard version)

					// 2.2 - Make one step backward
					join = curBackItem.JoinEdgesBackward(curNode);
					if (join != null) // At least one path was found
					{
						// Cycle through all combinations of previous node / previous states
						for (int i = 0; i < join.destNode.size(); i++)
						{
							int nextNode = join.destNode.get(i);
							Item []nextStates = join.destItem.get(i);
							for (int j = 0; j < nextStates.length; j++)
							{
								// If there is no cycle, step to the previous node
								if (g.node[nextNode].BackCycleCheck(numLauf,nextStates[j],curBackState.pathNum))
								{
									State st = new State(nextNode,nextStates[j],curBackState,curBackState.pathNum);
									backFifo.add(st);
									g.node[nextNode].AddBackVisited(numLauf,st);

									// Try to match the node with the regular expression
									nodeNext = nextStates[j].MatchBackNode(g.node[nextNode].nodeLabel);
									if (nodeNext != null)
									{
										for (int k = 0; k < nodeNext.length; k++)
										{
											if (g.node[nextNode].BackCycleCheck(numLauf,nodeNext[k],curBackState.pathNum))
											{
												State sta = new State(nextNode,nodeNext[k],st,curBackState.pathNum);
												backFifo.add(sta);
												g.node[nextNode].AddBackVisited(numLauf,sta);
											}
										}
									}
								}
							}
						}
					}*/
				}
			}


			// Part 3: prepare next step and check if we are finished
			bFinished = true;
			for (int i = 0; i < numStart; i++)
			{
				if (!bStartFinished[i])
				{
					bFinished = false;
					break;
				}
			}
			if (bFinished) // all paths have been found
			{
				res[0][0].fifoSize = fifo.size()+backFifo.size();
				globalTransitions += numTransitions;
				globalFifo += fifo.size()+backFifo.size();
				return res;
			}
			if ((ffPos < fifo.size()) && ((backffPos < backFifo.size()) || (!bFixedEnd)))
			{
				// Prepare next states to continue search
				curState = fifo.get(ffPos);
				curNode = g.node[curState.node];
				curItem = curState.pos;
				ffPos++;
				if (bFixedEnd)
				{
					curBackState = backFifo.get(backffPos);
					curBackNode = g.node[curBackState.node];
					curBackItem = curBackState.pos;
					backffPos++;
				}
			}
			else {
				// Nothing more to search. This is not necessarily a negative result,
				// it only means not all n*m paths could be found.
				globalTransitions += numTransitions;
				globalFifo += fifo.size()+backFifo.size();
				return res;
			}
		}
	}


	// Find the shortest path through the results.
	// We search all possible paths, and then return the shortest one.
	private ResultWay[] SearchResultPath(ResultWay [][][]way,int [][][]node)
	{
		// Class representing one current search position
		class ResPoint
		{
			public ResultWay []way;
			public int pointIndex;
			public int length;

			ResPoint(ResultWay []old,ResultWay neu,int p,int l)
			{
				pointIndex = p;
				length = l;
				if (old == null) way = new ResultWay[]{neu};
				else {
					way = new ResultWay[old.length+1];
					for (int i = 0; i < old.length; i++) way[i] = old[i];
					way[old.length] = neu;
				}
			}
		}

		// Search variables
		ArrayList<ResPoint> srcFifo = null;
		ArrayList<ResPoint> dstFifo = new ArrayList<ResPoint>(16);

		// Initialize search
		for (int s = 0; s < way[0].length; s++)
		{
			for (int e = 0; e < way[0][s].length; e++)
			{
				if (way[0][s][e] != null)
					dstFifo.add(new ResPoint(null,way[0][s][e],e,way[0][s][e].wayLength));
			}
		}

		// Run search
		for (int run = 1; run < way.length; run++)
		{
			if (dstFifo.size() == 0) return null; // no path found
			srcFifo = dstFifo;
			dstFifo = new ArrayList<ResPoint>(32);

			// Continue all current paths
			for (int i = 0; i < srcFifo.size(); i++)
			{
				ResPoint p = srcFifo.get(i);

				// Get start node for next iteration
				int s = p.pointIndex;

				// Add 1 length if there ist an implicit transition over an edge.
				if (node[run-1][1][s] != node[run][0][s]) p.length += 1;

				// Add all available paths from that node
				for (int e = 0; e < way[run][s].length; e++)
				{
					if (way[run][s][e] != null)
						dstFifo.add(new ResPoint(p.way,way[run][s][e],e,p.length+way[run][s][e].wayLength));
				}
			}
		}

		if (dstFifo.size() == 0) return null; // no path found

		// Find shortest path to return
		ResPoint shortestP = dstFifo.get(0);
		int numShort = 1;
		for (int i = 1; i < dstFifo.size(); i++)
		{
			ResPoint p = dstFifo.get(i);
			if (p.length < shortestP.length)
			{
				shortestP = p;
				numShort = 1;
			}
			else if (p.length == shortestP.length) numShort++;
		}

		if (Writer.bVerbose && (numShort > 1)) Writer.WriteLine("Result paths with equal length: "+numShort);
		return shortestP.way;
	}


	private boolean StartFinished(ResultWay [][]res,int num)
	{
		for (int i = 0; i < res[num].length; i++)
		{
			if (res[num][i] == null) return false;
		}
		return true;
	}

	private boolean EndFinished(ResultWay [][]res,int num)
	{
		for (int i = 0; i < res.length; i++)
		{
			if (res[i][num] == null) return false;
		}
		return true;
	}

}
