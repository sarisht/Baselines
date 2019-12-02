/****************************************************/
/* Follow - representation of a state's LFS			*/
/*													*/
/* Diplomarbeit - Andr� Koschmieder, 2009			*/
/****************************************************/


import java.util.*;


public class Follow
{
	// Properties of a follow set
	int numLabels = 0;
	Item []wcDest = null; // wildcard destination, if applicable
    
    SortedArray labels = new SortedArray();
    SortedArray sortedStates = new SortedArray(); // Additional sorted list of all states
    
    public static long numMergeJoins = 0;
    public static long numMergeResults = 0;
    public static long numMergedLabels = 0;
    

    // Helper function
    private void AddItemToSorted(Item state)
    {
        if (!sortedStates.Contains(state.orderNum))
            sortedStates.Insert(state.orderNum, state);
    }

	// Helper function
	private Item []AddItemToSet(Item []itemSet,Item newItem)
	{
		if (itemSet == null) return new Item[]{newItem};
		Item []neu = new Item[itemSet.length+1];
		for (int i = 0; i < itemSet.length; i++) neu[i] = itemSet[i];
		neu[itemSet.length] = newItem;
		return neu;
	}

	// Helper function
	private boolean ItemSetContains(Item []itemSet,Item testItem)
	{
		if (itemSet == null) return false;
		for (int i = 0; i < itemSet.length; i++)
		{
			if (itemSet[i] == testItem) return true;
		}
		return false;
	}



	// Add a single symbol to the follow set. If a wildcard is added,
	// all labels have to add that destination.
	public void Add(int symbol,Item d)
	{
        AddItemToSorted(d);
        
		if (symbol == -1)
		{
			// Add wildcard to the set
			if (!ItemSetContains(wcDest,d)) wcDest = AddItemToSet(wcDest,d);

			// Update all symbols to also include the added wildcard
			for (int i = 0; i < numLabels; i++)
			{
                Item []dest = (Item[])labels.GetAt(i);
				if (!ItemSetContains(dest,d)) labels.SetDataAt(i, AddItemToSet(dest,d));
			}
			return;
		}

		if (numLabels == 0)
		{
			// Empty set encountered, create a fresh one
			numLabels = 1;
            labels.Insert(symbol, new Item[]{d});
			return;
		}

		// Add symbol to the set. If the symbol is already in the set, only update the reference.
        int pos = labels.GetPos(symbol);
        if (pos >= 0)
        {
            Item []dest = (Item[])labels.GetAt(pos);
            if (!ItemSetContains(dest,d)) labels.SetDataAt(pos, AddItemToSet(dest,d));
            return;
        }

		// Add a new symbol to the set
		numLabels++;
        labels.Insert(symbol, new Item[]{d});
	}


	// Add a follow set to the set. Uses the symbols and references in the given set.
	public void Add(Follow f)
	{
		// Add all symbols to the set
		for (int i = 0; i < f.numLabels; i++)
		{
            Item []items = (Item[])f.labels.GetAt(i);
			for (int j = 0; j < items.length; j++)
			{
				Add(f.labels.GetKeyAt(i),items[j]);
			}
		}

		// Add wildcard references to the set
		if (f.wcDest != null)
		{
			for (int i = 0; i < f.wcDest.length; i++)
			{
				Add(-1,f.wcDest[i]);
			}
		}
	}


	public String FollowString()
	{
		String s = "";
		if (wcDest != null) s = ".";
		for (int i = 0; i < numLabels; i++)
		{
			s += labels.GetKeyAt(i);
			if (i < numLabels-1) s += ",";
		}
		return s;
	}



	public Item[] GetFollows(int symbol)
	{
		// Return the set of the given symbol
        Item[] res = (Item[])labels.Get(symbol);
        if (res != null) return res;

		// Symbol not found, look for wildcard
		if (wcDest != null) return wcDest;
		return null;
	}

	public boolean ContainsDest(Item destItem)
	{
        return sortedStates.Contains(destItem.orderNum);
	}

	public RegExp.JoinResult Join(int []joinLabels,int []joinDest)
	{
        //numMergeJoins++;
        //numMergedLabels += numLabels;
		// Execute sort-merge join between edge labels and accepted symbols
		RegExp.JoinResult res = new RegExp.JoinResult();
        int len = joinLabels.length;
		int i = 0, j = 0;
        
		while ((i < len) && (j < numLabels))
		{
			if (joinLabels[i] == labels.GetKeyAt(j)) // Found
			{
                //numMergeResults++;
				if (res.numResults == 0)
				{
                    res.numResults = 1;
					res.destNode = new int[len];
                    res.destNode[0] = joinDest[i];
					res.destItem = new Item[len][];
                    res.destItem[0] = (Item[])labels.GetAt(j);
				} else {
                    res.destNode[res.numResults] = joinDest[i];
                    res.destItem[res.numResults] = (Item[])labels.GetAt(j);
                    res.numResults++;
                }
				i++;
			}
			else if (joinLabels[i] < labels.GetKeyAt(j))
			{
				i++;
			}
			else j++;
		}

		if (res.destNode != null) return res;
		if (wcDest != null)
		{
            //numMergeResults++;
            res.numResults = len;
			res.destNode = new int[len];
			res.destItem = new Item[len][];
			for (i = 0; i < len; i++)
			{
				res.destNode[i] = joinDest[i];
				res.destItem[i] = wcDest;
			}
			return res;
		}
		return null;
	}
}
