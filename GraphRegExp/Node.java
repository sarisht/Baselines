/****************************************************/
/* Node - representation of one node in the graph 	*/
/*													*/
/* Diplomarbeit - Andr� Koschmieder, 2009			*/
/****************************************************/


public class Node
{
	// Node properties
	public int nodeLabel = 0;

	public int numOutEdges = 0;
	public int numInEdges = 0;
	public int []outNode = null;
	public int []outLabel = null;
	public int []inNode = null;
	public int []inLabel = null;

	// Attributes for faster creation
    private SortedArray t_out = new SortedArray();
    private SortedArray t_in = new SortedArray();

	// Search status flags
	public int visited = 0;
    public SortedArray visitStates = null;
	public int backVisited = 0;
    public SortedArray backVisitStates = null;
    

	public Node(int label)
	{
		nodeLabel = label;
	}

	public void AddOut(int dst,int label)
	{
		// Add outgoing edge (sorted)
		numOutEdges++;
        t_out.Insert(label, dst);
	}

	public void AddIn(int src,int label)
	{
		// Add incoming edge (sorted)
		numInEdges++;
        t_in.Insert(label, src);
	}


	public void FinalizeDataStructure()
	{
		outNode = new int[numOutEdges];
		outLabel = new int[numOutEdges];
		inNode = new int[numInEdges];
		inLabel = new int[numInEdges];

		for (int i = 0; i < numOutEdges; i++)
		{
            outNode[i] = (Integer)t_out.GetAt(i);
            outLabel[i] = t_out.GetKeyAt(i);
		}
		for (int i = 0; i < numInEdges; i++)
		{
            inNode[i] = (Integer)t_in.GetAt(i);
            inLabel[i] = t_in.GetKeyAt(i);
		}
        t_out = null;
        t_in = null;
	}

	public long GetMemoryUsage()
	{
		return 88 + 8*numOutEdges + 8*numInEdges;
	}
    
    public static int CycleHash(int statePos, int pathNum)
    {
        return pathNum + statePos*100000;
    }
    
    public void AddVisited(boolean bForward,int lauf,RegExp.State state)
    {
        if (bForward) AddVisited(lauf,state);
        else AddBackVisited(lauf, state);
    }

	public void AddVisited(int lauf,RegExp.State state)
	{
		if (visited != lauf)
		{
            visitStates = new SortedArray(CycleHash(state.pos.orderNum,state.pathNum),state);
			visited = lauf;
		}
        else visitStates.Insert(CycleHash(state.pos.orderNum,state.pathNum),state);
	}

	public void AddBackVisited(int lauf,RegExp.State state)
	{
		if (backVisited != lauf)
		{
            backVisitStates = new SortedArray(CycleHash(state.pos.orderNum,state.pathNum), state);
			backVisited = lauf;
		}
        else backVisitStates.Insert(CycleHash(state.pos.orderNum,state.pathNum), state);
	}
    
    public boolean CycleCheck(boolean bForward,int lauf,Item state,int pathNum)
    {
        if (bForward) return CycleCheck(lauf,state,pathNum);
        else return BackCycleCheck(lauf, state, pathNum);
    }
    
    public boolean CycleCheck(int lauf,Item state,int pathNum)
    {
        if (visited == lauf)
        {
            return (!visitStates.Contains(CycleHash(state.orderNum, pathNum)));
        }
        return true; // no cycle
    }
    
    public boolean BackCycleCheck(int lauf,Item state,int pathNum)
    {
        if (backVisited == lauf)
        {
            return (!backVisitStates.Contains(CycleHash(state.orderNum, pathNum)));
        }
        return true; // no cycle
    }
    
    
    public RegExp.State[] FinishCheck(boolean bForward,int lauf,Item state)
    {
        if (bForward) return FinishCheck(lauf, state);
        else return BackFinishCheck(lauf, state);
    }
    
    public RegExp.State[] FinishCheck(int lauf,Item state)
    {
        RegExp.State []res = null;

        // Has the current node already been visited by backwards search?
        if (backVisited == lauf)
        {
            // Check all visited states, if they meet the current search path
            int num = 0;
            SortedArray nextStates = state.followEdge.sortedStates;
            int numNextStates = nextStates.Size();
            int numBackStates = backVisitStates.Size();
            int curNext = 0, curBack = 0;
            
            while ((curNext < numNextStates) && (curBack < numBackStates))
            {
                Item itemNext = (Item)nextStates.GetAt(curNext);
                RegExp.State stateBack = (RegExp.State)backVisitStates.GetAt(curBack);
                if (itemNext == stateBack.pos)
                {
                    // Path found
                    if (num == 0) res = new RegExp.State[1];
                    else {
                        RegExp.State []tmp = res;
                        res = new RegExp.State[num+1];
                        for (int j = 0; j < num; j++) res[j] = tmp[j];
                    }
                    res[num] = stateBack;
                    num++;
                    curNext++;
                    curBack++;
                } 
                else if (itemNext.orderNum < stateBack.pos.orderNum) curNext++;
                else curBack++;
            }
        }
        return res;
    }

    public RegExp.State[] BackFinishCheck(int lauf,Item state)
    {
        RegExp.State []res = null;

        if (visited == lauf)
        {
            int num = 0;
            SortedArray backStates = state.backFollowEdge.sortedStates;
            int numBackStates = backStates.Size();
            int numNextStates = visitStates.Size();
            int curNext = 0, curPrev = 0;
            
            while ((curNext < numNextStates) && (curPrev < numBackStates))
            {
                Item itemPrev = (Item)backStates.GetAt(curPrev);
                RegExp.State stateForward = (RegExp.State)visitStates.GetAt(curNext);
                if (itemPrev == stateForward.pos)
                {
                    // Path found
                    if (num == 0) res = new RegExp.State[1];
                    else {
                        RegExp.State []tmp = res;
                        res = new RegExp.State[num+1];
                        for (int j = 0; j < num; j++) res[j] = tmp[j];
                    }
                    res[num] = stateForward;
                    num++;
                    curNext++;
                    curPrev++;
                } 
                else if (itemPrev.orderNum < stateForward.pos.orderNum) curPrev++;
                else curNext++;
            }
        }
        return res;
    }


	public String GetOutLabelText(int destNode)
	{
		String s = null;
		for (int i = 0; i < numOutEdges; i++)
		{
			if (outNode[i] == destNode)
			{
				if (s == null) s = ""+outLabel[i];
				else {
					s += "+";
					return s;
				}
			}
		}
		return s;
	}
}
