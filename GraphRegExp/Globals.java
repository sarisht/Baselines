/****************************************************/
/* Globals - definition of global variables/methods	*/
/*													*/
/* Diplomarbeit - Andr� Koschmieder, 2009			*/
/****************************************************/



public class Globals
{
	public static boolean bGraphLoaded = false;
	public static Graph graph = null;
    public static int numProcessors = 1;



	public static void DeInit(Gui gui)
	{
		if (bGraphLoaded)
		{
			graph = null;
			bGraphLoaded = false;
		}
		Writer.Clear();
		Writer.WriteLine(" *** No Graph Loaded.");
		if (gui != null) gui.EnableButtons(false);
	}

	public static String MemText(long size)
	{
		long val = size;
		int num = 0;
		while (val >= 10000)
		{
			val = val / 1024;
			num++;
		}
		String s = val+" ";
		if (num == 0) s += "Bytes";
		else if (num == 1) s += "KB";
		else if (num == 2) s += "MB";
		else if (num == 3) s += "GB";
		else if (num == 4) s += "TB";
		else s += "Error";
		return s;
	}

	public static String TimeText(long dauer) // in ms
	{
		if (dauer >= 10000) return new String((dauer/1000)+" s");
		else return new String(dauer+" ms");
	}
    
    public static String TimeTextNano(long nanoSeconds)
    {
        long d = nanoSeconds/1000;
        if (d < 1000) return ("<1 ms");
        d /= 1000;
        if (d < 10000) return (d+" ms");
        d /= 1000;
        return d+" s";
    }
    
    public static int GetNumThreads()
    {
        int numThreads = 2;
        if (numProcessors > 1)
        {
            if (numProcessors <= 4) numThreads = numProcessors;
            else if (numProcessors < 8) numThreads = numProcessors-1;
            else numThreads = numProcessors*3 / 4;
        }
        return numThreads;
    }
}