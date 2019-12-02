/****************************************************/
/* Graphregexp - main application starting point 	*/
/*													*/
/* Diplomarbeit - Andre Koschmieder, 2009			*/
/****************************************************/


public class Graphregexp
{
	public static void main(String args[])
	{
        Globals.numProcessors = Runtime.getRuntime().availableProcessors();
        
		// Command-line version
		boolean bStartGUI = true;
        boolean bRareCount = false;
		int rareCount = 0;
		boolean bNodesFile = false;
		String nodesFile = null;
		boolean bEdgesFile = false;
		String edgesFile = null;
		boolean bExpFile = false;
		String expFile = null;
		boolean bStartNode = false;
		String startNode = null;
		boolean bEndNode = false;
		String endNode = null;
        boolean bExistsOnly = false;
        boolean bMaxPath = false;
		boolean bCreateExp = false;
		String createExp = null;

		// Evaluate command line arguments
		if (args.length > 0)
		{
			for (int i = 0; i < args.length; i++)
			{
				String s = args[i];
				s = s.toLowerCase();

				if (s.equals("-nogui"))
				{
					bStartGUI = false;
				}
				else if (s.equals("-v"))
				{
					Writer.bVerbose = true;
				}
                else if (s.equals("-exists"))
                {
                    bExistsOnly = true;
                }
                else if (s.equals("-maxpath"))
                {
                    bMaxPath = true;
                }
				else if (args.length > i+1)
				{
					if (s.equals("-n"))
					{
						bNodesFile = true;
						nodesFile = args[i+1];
                        i++;
					}
					else if (s.equals("-e"))
					{
						bEdgesFile = true;
						edgesFile = args[i+1];
                        i++;
					}
					else if (s.equals("-ex"))
					{
						bExpFile = true;
						expFile = args[i+1];
                        i++;
					}
					else if (s.equals("-start"))
					{
						bStartNode = true;
						startNode = args[i+1];
                        i++;
					}
					else if (s.equals("-end"))
					{
						bEndNode = true;
						endNode = args[i+1];
                        i++;
					}
					else if (s.equals("-rare"))
					{
						try {
							rareCount = Integer.valueOf(args[i+1]);
                            bRareCount = true;
                            i++;
						}
						catch (Exception e) {}
					}
					else if (s.equals("-create"))
					{
						bCreateExp = true;
						createExp = args[i+1];
                        i++;
					}
				}
			}
		}

		// Run the program
		System.out.println("\nGraphRegExp - find regular expression paths in big graphs");
        System.out.println("Number of processors available: "+Globals.numProcessors);
		System.out.println("Author: Andre Koschmieder, HU Berlin");

		if (bStartGUI)
		{
			System.out.println("\nStarting GUI... (use -nogui for command line version)");
			Gui gui = new Gui();
			gui.startGUI();
			Gui.ExecCommandLine(gui,bNodesFile,nodesFile,bEdgesFile,edgesFile,bExpFile,expFile,bStartNode,startNode,bEndNode,endNode,bRareCount,rareCount,bExistsOnly,bMaxPath,bCreateExp,createExp);
		}
		else {
			// Check if command line arguments are ok
			boolean bCmdOk = false;
			if (bEdgesFile && (bCreateExp || bExpFile)) bCmdOk = true;

			if (!bCmdOk)
			{
				System.out.println("Command line version usage:\n");
				System.out.println("> java Graphregexp -nogui [options]");
				System.out.println("    -v                  verbose output");
				System.out.println("    -e <filename>       edges file to load");
				System.out.println("    -n <filename>       nodes file to load");
				System.out.println("    -ex <filename>      regular expression file to run");
				System.out.println("    -start <node>       start node for evaluation");
				System.out.println("    -end <node>         end node for evaluation");
				System.out.println("    -rare <count>       rare count for optimization");
                System.out.println("    -exists             only check if a path exists in the graph");
                System.out.println("    -maxPath            optimized maxPath calculation (not just rare count)");
				System.out.println("    -create <filename>  create a random regular expressions file for the graph");
				System.out.println("\nPlease specify an edges file (nodes file optional) and a regular expression file.");
                System.out.println("Selecting analysis method:\n   * Shortest path from start to end: specify start node (end node optional)");
                System.out.println("   * Exists any path in the graph: specify -exists and no start/end nodes");
                System.out.println("   * Find all paths in the graph: specify neither -exists nor start/end nodes");
				return;
			}
            
            Gui.ExecCommandLine(null,bNodesFile,nodesFile,bEdgesFile,edgesFile,bExpFile,expFile,bStartNode,startNode,bEndNode,endNode,bRareCount,rareCount,bExistsOnly,bMaxPath,bCreateExp,createExp);
		}
	}
}