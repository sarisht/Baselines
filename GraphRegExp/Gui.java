/****************************************************/
/* Gui - main window of the application 			*/
/*													*/
/* Diplomarbeit - Andr� Koschmieder, 2009			*/
/****************************************************/



import javax.swing.*;

import java.io.*; // File
import java.awt.*; // BorderLayout
import java.awt.event.*; // KeyEvent.*

public class Gui implements ActionListener, ItemListener
{
	// GUI variables
	JFrame frame;

	/*JMenuBar menuBar;
	JMenu menuFile;
	JMenuItem menuItemLoadEdges, menuItemLoadNodes, menuItemExit;*/

	JButton buttonLoadEdges, buttonLoadNodes, buttonGraph, buttonAuto;
	JButton buttonRandom, buttonCreateExpFile;
	JTextArea graphText;
	JScrollPane graphScroll;

	JLabel regLabel, infoLabel, startLabel, endLabel, rareLabel;
	JTextField regText, startNode, endNode, rareText;
	JCheckBox pathOpt, verbose;
    JRadioButton analysisStartEnd, analysisExists, analysisAll;

	JFileChooser fc;

	public void startGUI()
	{
		// Create the main window
		frame = new JFrame("GraphRegExp");
		frame.setLayout(new BorderLayout());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(300,300);

		// Create the main menu bar
		/*menuBar = new JMenuBar();
		menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		menuItemLoadEdges = new JMenuItem("Load Graph from Edges File");
		menuItemLoadEdges.addActionListener(this);
		menuFile.add(menuItemLoadEdges);
		menuItemLoadNodes = new JMenuItem("Load Graph from Nodes and Edges Files");
		menuItemLoadNodes.addActionListener(this);
		menuFile.add(menuItemLoadNodes);
		menuFile.addSeparator();
		menuItemExit = new JMenuItem("Exit");
		menuItemExit.addActionListener(this);
		menuFile.add(menuItemExit);

		menuBar.add(menuFile);*/

		// Create the main window elements
		buttonLoadEdges = new JButton("Load Graph from Edges File");
		buttonLoadEdges.addActionListener(this);
		buttonLoadNodes = new JButton("Load Graph from Nodes and Edges File");
		buttonLoadNodes.addActionListener(this);
		buttonGraph = new JButton("Graph Statistics");
		buttonGraph.addActionListener(this);
		JPanel upperButtonPanel = new JPanel();
		upperButtonPanel.add(buttonLoadEdges);
		upperButtonPanel.add(buttonLoadNodes);
		upperButtonPanel.add(buttonGraph);

		graphText = new JTextArea(" GraphRegExp",15,50);
		graphText.setEditable(false);
		graphScroll = new JScrollPane(graphText);

		// Create the lower control elements
		infoLabel = new JLabel(" Analysis methods: shortest path (start to end), any path exists in graph, find all paths in graph");
		//regLabel = new JLabel("RegExp for Path: ");
		//regText = new JTextField("(<<binding>>|<<receptor>>)+<<nociception>>(<<binding>>|<<receptor>>)+<<CCR3>>(<<binding>>|<<receptor>>)+<<interpreting>>(<<binding>>|<<receptor>>)+",45);
		startLabel = new JLabel("Start node: ");
		startNode = new JTextField("",15);
		endLabel = new JLabel(" End node: ");
		endNode = new JTextField("",15);
		buttonRandom = new JButton("Random");
		buttonRandom.addActionListener(this);
        
        analysisStartEnd = new JRadioButton("Fixed start / end node");
        analysisExists = new JRadioButton("Any path exists in the graph");
        analysisAll = new JRadioButton("Find all paths in the graph",true);
        ButtonGroup group = new ButtonGroup();
        group.add(analysisStartEnd);
        group.add(analysisExists);
        group.add(analysisAll);

		rareLabel = new JLabel("Rare count: ");
		rareText = new JTextField("100",5);
		pathOpt = new JCheckBox("maxPath optimization",false);
		//buttonQuery = new JButton("Run Query");
		//buttonQuery.addActionListener(this);
		verbose = new JCheckBox("Verbose");
		verbose.setSelected(Writer.bVerbose);
		verbose.addItemListener(this);
        buttonAuto = new JButton("Evaluate Query Set");
        buttonAuto.addActionListener(this);
		buttonCreateExpFile = new JButton("Exp File");
		buttonCreateExpFile.addActionListener(this);

		JPanel setP1 = new JPanel();
		//setP1.add(regLabel);
		//setP1.add(regText);
        setP1.add(analysisStartEnd);
        setP1.add(analysisExists);
        setP1.add(analysisAll);
		JPanel setP2 = new JPanel();
		setP2.add(startLabel);
		setP2.add(startNode);
		setP2.add(endLabel);
		setP2.add(endNode);
		setP2.add(buttonRandom);
		JPanel mitP = new JPanel(new BorderLayout());
		mitP.add(setP1,BorderLayout.NORTH);
		mitP.add(setP2,BorderLayout.CENTER);
		JPanel botP = new JPanel();
		botP.add(rareLabel);
		botP.add(rareText);
		botP.add(pathOpt);
		botP.add(verbose);
		//botP.add(buttonQuery);
		botP.add(buttonAuto);
		botP.add(buttonCreateExpFile);
		JPanel southPane = new JPanel(new BorderLayout());
		southPane.add(infoLabel,BorderLayout.NORTH);
		southPane.add(mitP,BorderLayout.CENTER);
		southPane.add(botP,BorderLayout.SOUTH);

		// Create the file chooser
		fc = new JFileChooser();


		// Pack everything together
		//frame.setJMenuBar(menuBar);
		frame.add(upperButtonPanel,BorderLayout.NORTH);
		frame.add(graphScroll,BorderLayout.CENTER);
		frame.add(southPane,BorderLayout.SOUTH);

		frame.pack();
		frame.setVisible(true);
		Writer.SetGui(this);

		Globals.DeInit(this);
	}

	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		if (source == buttonLoadEdges)
		{
			fc.setDialogTitle("Choose edge file to open...");
			int ret = fc.showOpenDialog(frame);
			if (ret == JFileChooser.APPROVE_OPTION)
			{
				File file = fc.getSelectedFile();
				Globals.DeInit(this);
				Globals.graph = Graph.LoadFromEdgeFile(file);
				if (Globals.graph != null)
				{
					Globals.bGraphLoaded = true;
					EnableButtons(true);
				}
			}
		}
		else if (source == buttonLoadNodes)
		{
			fc.setDialogTitle("Choose node file to open...");
			int ret = fc.showOpenDialog(frame);
			if (ret == JFileChooser.APPROVE_OPTION)
			{
				File nodeFile = fc.getSelectedFile();
				fc.setDialogTitle("Choose edge file to open...");
				ret = fc.showOpenDialog(frame);
				if (ret == JFileChooser.APPROVE_OPTION)
				{
					File edgeFile = fc.getSelectedFile();
					Globals.DeInit(this);
					Globals.graph = Graph.LoadFromNodeAndEdgeFile(nodeFile,edgeFile);
					if (Globals.graph != null)
					{
						Globals.bGraphLoaded = true;
						EnableButtons(true);
					}
				}
			}
		}
		else if (source == buttonGraph)
		{
			Globals.graph.PrintGraphStatistics();
		}
		else if (source == buttonRandom)
		{
			startNode.setText(Globals.graph.GetRandomNode());
			endNode.setText(Globals.graph.GetRandomNode());
		}
		/*else if (source == buttonQuery)
		{
			String regExp = regText.getText();
			String start = startNode.getText();
			String end = endNode.getText();
			String rare = rareText.getText();
			if ((regExp.equals("")) || (start.equals("")))
				JOptionPane.showMessageDialog(frame,"Please enter at least the regular expression and the start node!","GraphRegExp",JOptionPane.INFORMATION_MESSAGE);
			else {
				int st = Globals.graph.GetNodeName(start);
				int en = Globals.graph.GetNodeName(end);
				if (end.equals("")) en = -1;
				if (st < 0)
					JOptionPane.showMessageDialog(frame,"Node \""+start+"\" not found. Please enter a valid start node.","GraphRegExp",JOptionPane.INFORMATION_MESSAGE);
				else if ((!end.equals("")) && (en < 0))
					JOptionPane.showMessageDialog(frame,"Node \""+end+"\" not found. Please enter a valid destination node.","GraphRegExp",JOptionPane.INFORMATION_MESSAGE);
				else {
					int rar = 0;
					try {
						rar = Integer.valueOf(rare);
					} catch (Exception exx)
					{
						rar = 0;
					}
					RegExp reg = new RegExp(regExp);
					if (reg.Parse(true))
					{
						reg.ExecQuery(st,en,rar,pathOpt.isSelected(),true);
					}
				}
			}
		}*/
		else if (source == buttonAuto)
		{
			String rare = rareText.getText();
			int rar = 0;
			try {
				rar = Integer.valueOf(rare);
			} catch (Exception exx)
			{
				rar = 0;
			}
            String startString = startNode.getText();
            String endString = endNode.getText();
            int start = Globals.graph.GetNodeName(startString);
            int end = Globals.graph.GetNodeName(endString);
            if (analysisStartEnd.isSelected())
            {
                if (endString.isEmpty()) end = -2;
                if (start < 0)
                {
                    JOptionPane.showMessageDialog(frame,"Node \""+startString+"\" not found. Please enter a valid start node.","GraphRegExp",JOptionPane.INFORMATION_MESSAGE);
                    return;
                } else if (end == -1)
                {
                    JOptionPane.showMessageDialog(frame,"Node \""+endString+"\" not found. Please enter a valid destination node.","GraphRegExp",JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
			fc.setDialogTitle("Choose RegExp file for automated execution...");
			int ret = fc.showOpenDialog(frame);
			if (ret == JFileChooser.APPROVE_OPTION)
			{
				File exFile = fc.getSelectedFile();
                if (analysisStartEnd.isSelected()) Analysis.RunStartEnd(exFile,rar,pathOpt.isSelected(),start,end);
                else if (analysisExists.isSelected()) Analysis.RunExists(exFile,rar,pathOpt.isSelected());
                else Analysis.RunAllPaths(exFile,rar,pathOpt.isSelected());
				//RegExp.RunAutomation(nodeFile,rar,pathOpt.isSelected());
			}
		}
		else if (source == buttonCreateExpFile)
		{
			fc.setDialogTitle("Choose file to save regular expression list...");
			int ret = fc.showSaveDialog(frame);
			if (ret == JFileChooser.APPROVE_OPTION)
			{
				File nodeFile = fc.getSelectedFile();
				Globals.graph.CreateRegExpFile(nodeFile);
				//Globals.graph.ExportGraphDistribution(nodeFile);
			}
		}
	}

	public void itemStateChanged(ItemEvent e)
	{
		Object source = e.getItemSelectable();
		if (source == verbose)
		{
			Writer.bVerbose = verbose.isSelected();
		}
	}



	public void SetGraphText(String text)
	{
		graphText.setText(text);
		graphScroll.getVerticalScrollBar().setValue(0);
		graphScroll.paintImmediately(0,0,1000,1000);
		graphText.paintImmediately(0,0,1000,1000);
	}

	public void AddGraphText(String text)
	{
		graphText.append("\n"+text);
		graphScroll.getVerticalScrollBar().setValue(graphScroll.getVerticalScrollBar().getMaximum());
		graphText.paintImmediately(0,0,1000,1000);
	}

	public void EnableButtons(boolean bEnabled)
	{
		buttonGraph.setEnabled(bEnabled);
		//buttonQuery.setEnabled(bEnabled);
		buttonAuto.setEnabled(bEnabled);
		buttonRandom.setEnabled(bEnabled);
		//buttonCreateExpFile.setEnabled(bEnabled);
	}


	public static void ExecCommandLine(Gui gui, boolean bNodesFile, String nodesFile, boolean bEdgesFile,
                                String edgesFile, boolean bExpFile, String expFile, boolean bStartNode,
                                String sNode, boolean bEndNode, String eNode, boolean bRareCount, int rareCount,
                                boolean bExistsOnly, boolean bMaxPathOpt, boolean bCreateExp, String createExp)
	{
		boolean bGraphOK = false;

        if (gui != null)
        {
            if (bStartNode) gui.startNode.setText(sNode);
            if (bEndNode) gui.endNode.setText(eNode);
            if (bStartNode) gui.analysisStartEnd.setSelected(true);
            if (bMaxPathOpt) gui.pathOpt.setSelected(true);
            if (bExistsOnly) gui.analysisExists.setSelected(true);
            if (bRareCount) gui.rareText.setText(rareCount+"");
        }

		if (bNodesFile)
		{
			if (!bEdgesFile) Writer.WriteLine("Command-line error: cannot only load a Nodes file, please also specify an Edge file!");
			else {
				if (gui != null) Globals.DeInit(gui);
				Globals.graph = Graph.LoadFromNodeAndEdgeFile(new File(nodesFile),new File(edgesFile));
				if (Globals.graph != null)
				{
					Globals.bGraphLoaded = true;
					if (gui != null) gui.EnableButtons(true);
					bGraphOK = true;
				}
			}
		}
		else if (bEdgesFile)
		{
			if (gui != null) Globals.DeInit(gui);
			Globals.graph = Graph.LoadFromEdgeFile(new File(edgesFile));
			if (Globals.graph != null)
			{
				Globals.bGraphLoaded = true;
				if (gui != null) gui.EnableButtons(true);
				bGraphOK = true;
			}
		}

		if (bGraphOK && bCreateExp)
		{
			Globals.graph.CreateRegExpFile(new File(createExp));
		}

		if (bGraphOK && bExpFile)
		{
		    
            if (bExistsOnly) Analysis.RunExists(new File(expFile),rareCount,bMaxPathOpt);
            else if (!bStartNode) Analysis.RunAllPaths(new File(expFile),rareCount,bMaxPathOpt);
            else {
                int start = Globals.graph.GetNodeName(sNode);
                int end = -2;
                if (bEndNode) end = Globals.graph.GetNodeName(eNode);
                if (start < 0)
                {
                    Writer.WriteLine("Command-line error: Node \""+sNode+"\" not found. Please enter a valid start node.");
                    return;
                } else if (end == -1)
                {
                    Writer.WriteLine("Command-line error: Node \""+eNode+"\" not found. Please enter a valid destination node.");
                    return;
                }
                
                Analysis.RunStartEnd(new File(expFile),rareCount,bMaxPathOpt,start,end);
            }
		}
	}
}
