/****************************************************/
/* Writer - controls printing information (gui/cmd) */
/*													*/
/* Diplomarbeit - Andr� Koschmieder, 2009			*/
/****************************************************/




public class Writer
{
	public static boolean bVerbose = false;

	private static boolean bGui = false;
	private static Gui gui = null;

	private static String string = null;



	public static void SetGui(Gui g)
	{
		bGui = true;
		gui = g;
	}

	public static void Write(String s)
	{
		if (bGui)
		{
			if (string == null) string = s;
			else string += s;
		}
		else System.out.print(s);
	}

	public static void WriteLine(String s)
	{
		if (bGui)
		{
			if (string == null) gui.AddGraphText(s);
			else {
				gui.AddGraphText(string+s);
				string = null;
			}
		}
		else System.out.println(s);
	}

	public static void Clear()
	{
		if (bGui) gui.SetGraphText("");
		else System.out.println("\n");
	}
}
