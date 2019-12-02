import java.io.FileWriter;
import java.io.IOException;

/*****************************************************************/
/* Item - one symbol/control structure of the regular expression */
/*																 */
/* Diplomarbeit - Andr� Koschmieder, 2009						 */
/*****************************************************************/





public class Item
{
	// Constants
	public static final int TYP_SYMBOL = 1;
	public static final int TYP_RANDOM = 2;
	public static final int TYP_BRACKET = 3;
	public static final int TYP_OR = 4;
	public static final int TYP_START = 5;
	public static final int TYP_END = 6;

	public static final int MOD_NONE = 0;
	public static final int MOD_STAR = 1;
	public static final int MOD_PLUS = 2;
	public static final int MOD_QUESTION = 3;

	public static final int DEST_NONE = 0;
	public static final int DEST_EDGE = 1;
	public static final int DEST_NODE = 2;


	public int typ; // TYP_*
	public int modifier; // MOD_*
	public int dest; // DEST_*
	public int symbol; // Hashtable Referenz
	public Item []or;
    public int orderNum;

	public Item next = null;
	public Item prev = null;
	public Item parent = null;
	public Item child = null;
	public Item lastChild = null;

	public boolean bAcceptEmpty = false;
	public boolean bEndsExpression = false;
	public Follow firstEdge = new Follow();
	public Follow firstNode = new Follow();
	public Follow followEdge = new Follow();
	public Follow followNode = new Follow();

	public boolean bBackEnds = false;
	public Follow backFirstEdge = new Follow();
	public Follow backFirstNode = new Follow();
	public Follow backFollowEdge = new Follow();
	public Follow backFollowNode = new Follow();



	public Item(int t,int s,int d,Item par,Item pr)
	{
		typ = t;
		modifier = MOD_NONE;
		dest = d;
		symbol = s;
		or = null;
		parent = par;
		prev = pr;
        orderNum = 0;
	}

	public void AddOr(int t,int s,int d)
	{
		if (typ != TYP_OR)
		{
			or = new Item[2];
			or[0] = new Item(typ,symbol,dest,this,null);
			or[0].modifier = modifier;
			or[0].child = child;
			or[0].lastChild = lastChild;
			child = null;
			lastChild = null;
			typ = TYP_OR;
			modifier = MOD_NONE;
			dest = DEST_NONE;
			symbol = 0;
		}
		else {
			int num = or.length;
			Item []tmp = or;
			or = new Item[num+1];
			for (int i = 0; i < num; i++)
				or[i] = tmp[i];
			tmp = null;
		}
		or[or.length-1] = new Item(t,s,d,this,null);

	}

	public void AddOrModifier(int mod) throws Exception
	{
		if (typ == TYP_OR)
		{
			if (or[or.length-1].modifier != MOD_NONE) throw new Exception("Invalid regular expression:\nfound unexpected modifier.");
			or[or.length-1].modifier = mod;
		}
	}

	public Item GetLastOr()
	{
		return or[or.length-1];
	}


	public String PrintSimple()
	{
		String s = "";
		if (typ == TYP_END) return s;
		else if (typ == TYP_SYMBOL)
		{
			if (dest == DEST_EDGE) s = "<"+symbol+">";
			else if (dest == DEST_NODE) s = "["+symbol+"]";
		}
		else if (typ == TYP_RANDOM)
		{
			if (dest == DEST_EDGE) s = "<.>";
			else if (dest == DEST_NODE) s = "[.]";
		}
		else if (typ == TYP_BRACKET)
		{
			s = "(" + child.PrintSimple() + ")";
		}
		else if (typ == TYP_OR)
		{
			for (int i = 0; i < or.length; i++)
			{
				if (i > 0) s += "|";
				s += or[i].PrintSimple();
			}
		}
		if (modifier != MOD_NONE) s += PrintModifier();

		if (next != null) s += next.PrintSimple();
		return s;
	}

	public void PrintExtended(int col)
	{
		String s = "";
		for (int i = 0; i < col; i++) s += "    ";
		switch (typ)
		{
			case TYP_END:
				s += "EOF";
				break;
			case TYP_START:
				s += "START"+FirstString();
				break;
			case TYP_SYMBOL:
				if (dest == DEST_EDGE) s += "<"+symbol+">"+PrintModifier()+FirstString();
				else if (dest == DEST_NODE) s += "["+symbol+"]"+PrintModifier()+FirstString();
				break;
			case TYP_RANDOM:
				if (dest == DEST_EDGE) s += "<.>"+PrintModifier()+FirstString();
				else if (dest == DEST_NODE) s += "[.]"+PrintModifier()+FirstString();
				break;
			case TYP_BRACKET:
				s += "()"+PrintModifier()+FirstString();
				break;
			case TYP_OR:
				for (int i = 0; i < or.length-1; i++) s += "|";
				s += FirstString();
				break;
		}
		Writer.WriteLine(s);

		if (typ == TYP_BRACKET) child.PrintExtended(col+1);
		else if (typ == TYP_OR)
		{
			for (int i = 0; i < or.length; i++) or[i].PrintExtended(col+1);
		}

		if (next != null) next.PrintExtended(col);
	}

	private char PrintModifier()
	{
		if (modifier == MOD_STAR) return '*';
		else if (modifier == MOD_PLUS) return '+';
		else if (modifier == MOD_QUESTION) return '?';
		return ' ';
	}

	private String FirstString()
	{
		return " \t<"+firstEdge.FollowString()+">["+firstNode.FollowString()+"]"+(bAcceptEmpty?"?":"")
				+(bEndsExpression?"#":"")+" --> <"+followEdge.FollowString()+">["+followNode.FollowString()+"]";
	}

	public String SymbolString()
	{
		if (typ == TYP_RANDOM) return ".";
		else if (typ == TYP_SYMBOL) return symbol+"";
		else return "";
	}

	// Recursively calculate the first set of the current item
	public int CalcFirst(int curOrder)
	{
		// Get the first set of the Item properties, and determine
		// whether the item can be skipped.
		if ((modifier == MOD_STAR) || (modifier == MOD_QUESTION)) bAcceptEmpty = true;
        orderNum = curOrder;
        int tempOrder = orderNum+1;
        
		switch (typ)
		{
			case TYP_SYMBOL:
				if (dest == DEST_EDGE)
				{
					firstEdge.Add(symbol,this);
					backFirstEdge.Add(symbol,this);
				} else {
					firstNode.Add(symbol,this);
					backFirstNode.Add(symbol,this);
				}
				break;
			case TYP_RANDOM:
				if (dest == DEST_EDGE)
				{
					firstEdge.Add(-1,this);
					backFirstEdge.Add(-1,this);
				} else {
					firstNode.Add(-1,this);
					backFirstNode.Add(-1,this);
				}
				break;
			case TYP_END:
				bAcceptEmpty = true;
				bEndsExpression = true;
				break;
			case TYP_START:
				bAcceptEmpty = true;
				bBackEnds = true;
				break;
			case TYP_BRACKET:
				// First calculate the first sets of the children (items in brackets),
				// then use first child as reference but cycle through the items that
				// can be skipped (i.e. contain ? or *).
				tempOrder = child.CalcFirst(tempOrder);
				Item it = child;
				firstEdge.Add(it.firstEdge);
				firstNode.Add(it.firstNode);
				while ((it.bAcceptEmpty) && (it.next != null))
				{
					it = it.next;
					firstEdge.Add(it.firstEdge);
					firstNode.Add(it.firstNode);
				}
				if (it.bAcceptEmpty) bAcceptEmpty = true;

				// And the same for backwards direction
				it = lastChild;
				backFirstEdge.Add(it.backFirstEdge);
				backFirstNode.Add(it.backFirstNode);
				while ((it.bAcceptEmpty) && (it.prev != null))
				{
					it = it.prev;
					backFirstEdge.Add(it.backFirstEdge);
					backFirstNode.Add(it.backFirstNode);
				}
				break;
			case TYP_OR:
				// First set in OR is the conjunction of all sets.
				// If one can be skipped, all can.
				for (int i = 0; i < or.length; i++)
				{
					tempOrder = or[i].CalcFirst(tempOrder);
					firstEdge.Add(or[i].firstEdge);
					firstNode.Add(or[i].firstNode);
					backFirstEdge.Add(or[i].backFirstEdge);
					backFirstNode.Add(or[i].backFirstNode);
					if (or[i].bAcceptEmpty) bAcceptEmpty = true;
				}
				break;
		}

		// Recursively calculate the next item
		if (next != null) tempOrder = next.CalcFirst(tempOrder);
        return tempOrder;
	}


	// Recursively calculate the follow set of the current item.
	// The recursion works backwards, starting at the EOF item.
	public void CalcFollow(Item reference,boolean bLastChild)
	{
		if (typ != TYP_END)
		{
			// Special rules apply if we are at the end of brackets (parent item is the bracket item)
			if (bLastChild)
			{
				// Always add the bracket's follow set
				followEdge.Add(parent.followEdge);
				followNode.Add(parent.followNode);

				// If the bracket can be repeated, also add its first set
				if ((parent.modifier == MOD_PLUS) || (parent.modifier == MOD_STAR))
				{
					followEdge.Add(parent.firstEdge);
					followNode.Add(parent.firstNode);
				}

				// If the bracket ends the expression, so does this last item in the bracket
				if (parent.bEndsExpression) bEndsExpression = true;
			}
			else // Not the last item in a bracket
			{
				// Add the first set of the following item to the follow set
				followEdge.Add(reference.firstEdge);
				followNode.Add(reference.firstNode);

				// If the following item can be skipped, also add its follow set
				if (reference.bAcceptEmpty)
				{
					followEdge.Add(reference.followEdge);
					followNode.Add(reference.followNode);

					// If the following item can be skipped and ends the expression, this item does too.
					if (reference.bEndsExpression) bEndsExpression = true;
				}
			}
		}

		if (typ == TYP_BRACKET)
		{
			// For brackets, recursively calculate the follow set backwards
			lastChild.CalcFollow(this,true);
		}
		else if (typ == TYP_OR)
		{
			// Also calculate the follow set of the OR parts
			for (int i = 0; i < or.length; i++)
			{
				or[i].CalcFollow(this,true);
			}
		}
		else if ((modifier == MOD_PLUS) || (modifier == MOD_STAR))
		{
			// If the current item is repeatable, add its first set to the follow set
			followEdge.Add(firstEdge);
			followNode.Add(firstNode);
		}

		// Continue calculating recursively (backwards)
		if (prev != null) prev.CalcFollow(this,false);
	}


	// Calculate the follow set for backwards direction.
	// Recursively works forward, starting at the start item.
	public void CalcBackFollow(Item reference,boolean bFirstChild)
	{
		if (typ != TYP_START)
		{
			// Special rules apply if we are at the beginning of brackets (parent item is the bracket item)
			if (bFirstChild)
			{
				// Always add the bracket's follow set
				backFollowEdge.Add(parent.backFollowEdge);
				backFollowNode.Add(parent.backFollowNode);

				// If the bracket can be repeated, also add its first set
				if ((parent.modifier == MOD_PLUS) || (parent.modifier == MOD_STAR))
				{
					backFollowEdge.Add(parent.backFirstEdge);
					backFollowNode.Add(parent.backFirstNode);
				}

				// If the bracket ends the expression, so does this last item in the bracket
				if (parent.bBackEnds) bBackEnds = true;
			}
			else // Not the first item in a bracket
			{
				// Add the first set of the following item to the follow set
				backFollowEdge.Add(reference.backFirstEdge);
				backFollowNode.Add(reference.backFirstNode);

				// If the following item can be skipped, also add its follow set
				if (reference.bAcceptEmpty)
				{
					backFollowEdge.Add(reference.backFollowEdge);
					backFollowNode.Add(reference.backFollowNode);

					// If the following item can be skipped and ends the expression, this item does too.
					if (reference.bBackEnds) bBackEnds = true;
				}
			}
		}

		if (typ == TYP_BRACKET)
		{
			// For brackets, recursively calculate the follow set
			child.CalcBackFollow(this,true);
		}
		else if (typ == TYP_OR)
		{
			// Also calculate the follow set of the OR parts
			for (int i = 0; i < or.length; i++)
			{
				or[i].CalcBackFollow(this,true);
			}
		}
		else if ((modifier == MOD_PLUS) || (modifier == MOD_STAR))
		{
			// If the current item is repeatable, add its first set to the follow set
			backFollowEdge.Add(backFirstEdge);
			backFollowNode.Add(backFirstNode);
		}

		// Continue calculating recursively (forward)
		if (next != null) next.CalcBackFollow(this,false);
	}

	public RegExp.Unique[] GetMandatoryUniques(int rare)
	{
		RegExp.Unique []tmp = null;
		if (next != null) tmp = next.GetMandatoryUniques(rare);

		if ((typ == TYP_SYMBOL) && ((modifier == MOD_NONE) || (modifier == MOD_PLUS)))
		{
			int nUnique = 0;
			if (dest == DEST_EDGE) nUnique = Globals.graph.IsRareEdge(symbol,rare);
			else if (dest == DEST_NODE) nUnique = Globals.graph.IsRareNode(symbol,rare);

			if (nUnique > 0)
			{
				if (tmp == null) tmp = new RegExp.Unique[]{new RegExp.Unique(this,nUnique)};
				else {
					RegExp.Unique []old = tmp;
					tmp = new RegExp.Unique[old.length+1];
					tmp[0] = new RegExp.Unique(this,nUnique);
					for (int i = 0; i < old.length; i++) tmp[i+1] = old[i];
				}
			}
		} else if ((typ == TYP_BRACKET) && ((modifier == MOD_NONE) || (modifier == MOD_PLUS)))
		{
			RegExp.Unique []tmp2 = child.GetMandatoryUniques(rare);
			if (tmp2 != null)
			{
				if (tmp == null) tmp = tmp2;
				else {
					RegExp.Unique []old = tmp;
					tmp = new RegExp.Unique[old.length+tmp2.length];
					for (int i = 0; i < tmp2.length; i++) tmp[i] = tmp2[i];
					for (int i = 0; i < old.length; i++)  tmp[i+tmp2.length] = old[i];
				}
			}
		}
		return tmp;
	}

	public Item[] MatchEdge(int symbol)
	{
		return followEdge.GetFollows(symbol);
	}

	public Item[] MatchNode(int symbol)
	{
		return followNode.GetFollows(symbol);
	}

	public Item[] MatchBackEdge(int symbol)
	{
		return backFollowEdge.GetFollows(symbol);
	}

	public Item[] MatchBackNode(int symbol)
	{
		return backFollowNode.GetFollows(symbol);
	}

	public boolean Meets(Item backDest)
	{
		return (followEdge.ContainsDest(backDest) || followNode.ContainsDest(backDest));
	}

	public boolean BackMeets(Item forwardDest)
	{
		return (backFollowEdge.ContainsDest(forwardDest) || backFollowNode.ContainsDest(forwardDest));
	}

	public RegExp.JoinResult JoinEdges(Node node,boolean bForward)
	{
		if (bForward) return followEdge.Join(node.outLabel,node.outNode);
        else return backFollowEdge.Join(node.inLabel,node.inNode);
	}

   
    public boolean EndsExpression(boolean bForward)
    {
        if (bForward) return bEndsExpression;
        else return bBackEnds;
    }
    
    
    /*public RegExp.StatAggregation ExportCycleRecursive(boolean bExport,FileWriter file) throws IOException
    {
        return null;
    }*/

}
