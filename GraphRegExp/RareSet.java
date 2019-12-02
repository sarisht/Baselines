import java.util.*;

public class RareSet
{
    // Properties of the set of rare edges
    public int numRares;
    public RegExp.Unique state[]; // [numRares]
    public int startNodes[][]; // [numRares]
    public int endNodes[][]; // [numRares]
    public int connected[][][]; // [numRares][numStartNodes] (index of end node)
    
    
    
    private RareSet(int num,RegExp.Unique u[],int s[][],int e[][],SortedArray con[][])
    {
        numRares = num;
        state = u;
        startNodes = s;
        endNodes = e;
        
        // Convert sorted array of end Nodes to array of indices
        connected = new int[numRares][][];
        for (int i = 0; i < numRares; i++)
        {
            connected[i] = new int[startNodes[i].length][];
            for (int j = 0; j < startNodes[i].length; j++)
            {
                connected[i][j] = new int[con[i][j].Size()];
                for (int k = 0; k < con[i][j].Size(); k++)
                {
                    connected[i][j][k] = Arrays.binarySearch(endNodes[i],con[i][j].GetKeyAt(k));
                }
            }
        }
    }
    
    
    public static RareSet CalcRares(Graph g,RegExp re,int rareCount,boolean bOpt,boolean bForceOneRare)
    {
        // 1. calculate rare states
        RegExp.Unique rares[] = re.GetRareStates(rareCount, bOpt);
        if (rares == null)
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
        if (rares == null) return null;
        int numRares = rares.length;
        if (SearchExistsThread.bStop)
        {
            return null;
        }
        
        // 2. Calculate the corresponding start and end nodes for the rare states
        int endNodes[][] = new int[numRares][];
        int startNodes[][] = new int[numRares][];
        SortedArray startConnections[][] = new SortedArray[numRares][];
        
        boolean hashEnd[] = new boolean[g.numNodes];
        SortedArray hashStart[] = new SortedArray[g.numNodes];

        for (int i = 0; i < numRares; i++)
        {
            if (rares[i].item.dest == Item.DEST_NODE)
            {
                System.err.print("ERROR:noNodes!");
            } else {
                int []sNodes = g.GetEdgeStartNodes(rares[i].item.symbol);
                int []eNodes = g.GetEdgeEndNodes(rares[i].item.symbol);
                
                // Use new sorting algorithm
                // Source: corresponding lists of sNodes and eNodes
                // Destination: Sorted unique lists startNodes and endNodes
                // and startConnections connecting a start node to all its end nodes.
                // Algorithm: Hash sort for start and end nodes O(2*n) and sorted array list for connections.
                int numDifferent = 0;
                for (int j = 0; j < eNodes.length; j++)
                {
                    if (!hashEnd[eNodes[j]])
                    {
                        hashEnd[eNodes[j]] = true;
                        numDifferent++;
                    }
                }
                endNodes[i] = new int[numDifferent];
                for (int j = 0, d = 0; d < numDifferent; j++)
                {
                    if (hashEnd[j])
                    {
                        endNodes[i][d] = j;
                        hashEnd[j] = false;
                        d++;
                    }
                }
                
                numDifferent = 0;
                for (int j = 0; j < sNodes.length; j++)
                {
                    if (hashStart[sNodes[j]] == null)
                    {
                        hashStart[sNodes[j]] = new SortedArray();
                        numDifferent++;
                    }
                    hashStart[sNodes[j]].InsertIfNotInside(eNodes[j],null);
                }
                startNodes[i] = new int[numDifferent];
                startConnections[i] = new SortedArray[numDifferent];
                for (int j = 0, d = 0; d < numDifferent; j++)
                {
                    if (hashStart[j] != null)
                    {
                        startNodes[i][d] = j;
                        startConnections[i][d] = hashStart[j];
                        hashStart[j] = null;
                        d++;
                    }
                }
            }
        }
        
        if (SearchExistsThread.bStop)
        {
            return null;
        }
        
        // 3. Convert sorted arrays to standard arrays
        return new RareSet(numRares,rares,startNodes,endNodes,startConnections);
    }
}
