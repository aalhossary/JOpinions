package sg.edu.ntu.jopinions.control.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.ImportException;

import sg.edu.ntu.jopinions.Constants;
import sg.edu.ntu.jopinions.control.cli.GraphsIO;
import sg.edu.ntu.jopinions.control.cli.Parser;
import sg.edu.ntu.jopinions.models.OpinionsMatrix;
import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.models.Utils;
import sg.edu.ntu.jopinions.views.GraphPanel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class SimulationFrame extends JFrame {
	private static final long serialVersionUID = 9056033034632796388L;

	private boolean verbose = false;
	private JPanel contentPane;
	private Parser parser = null;
	private OpinionsMatrix x;
	private Map<Integer, float[][]> states;
	private int maxFrame = -1;
	private final Action openAction = new OpenAction();
	private final Action action = new PlayPauseAction();
	private GraphPanel<PointND, DefaultEdge> graphPanel;
	private JSlider slider;
	private JSpinner frameSpinner;
	private JButton playPauseButton;
	private JButton stopButton;
	private JMenuItem mntmClose;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SimulationFrame frame = new SimulationFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public SimulationFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 382);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.setEnabled(false);
		mntmNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		mnFile.add(mntmNew);
		
		JSeparator separator_1 = new JSeparator();
		mnFile.add(separator_1);
		
		JMenuItem mntmOpen = new JMenuItem("Open ...");
		mntmOpen.setAction(openAction);
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		mnFile.add(mntmOpen);
		
		mntmClose = new JMenuItem("Close");
		mntmClose.setEnabled(false);
		mntmClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
		mnFile.add(mntmClose);
		
		JSeparator separator = new JSeparator();
		mnFile.add(separator);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mnFile.add(mntmExit);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JPanel buttomPanel = new JPanel();
		contentPane.add(buttomPanel, BorderLayout.SOUTH);
		buttomPanel.setLayout(new BorderLayout(0, 0));
		
		slider = new JSlider();
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (states == null) {
					return;
				}
				int newValue = ((JSlider)e.getSource()).getValue();
				float[][] state = states.get(newValue);
				if (state != null) {
					x.match(state);
					graphPanel.repaint();
				}
			}
		});
		slider.setValue(0);
		buttomPanel.add(slider, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		buttomPanel.add(panel, BorderLayout.EAST);

		playPauseButton = new JButton("> / ||");
		playPauseButton.setAction(action);
		playPauseButton.setEnabled(false);
		
		JLabel lblFrame = new JLabel("Frame");
		panel.add(lblFrame);
		
		frameSpinner = new JSpinner();
		panel.add(frameSpinner);
		
		JLabel lblDt = new JLabel("dt");
		panel.add(lblDt);
		
		JSpinner speedSpinner = new JSpinner();
		speedSpinner.setModel(new SpinnerNumberModel(0.5, 0.01, 5.0, 0.1));
		panel.add(speedSpinner);
		panel.add(playPauseButton);

		stopButton = new JButton("stop");
		stopButton.setEnabled(false);
		panel.add(stopButton);

		graphPanel = new GraphPanel<>();
		contentPane.add(graphPanel, BorderLayout.CENTER);

		JScrollPane scrollPane = new JScrollPane();
		JPanel paramsPanel = new JPanel();
		Box verticalBox = Box.createVerticalBox();
		paramsPanel.add(verticalBox);
		scrollPane.setViewportView(paramsPanel);
		contentPane.add(scrollPane, BorderLayout.EAST);

		JPanel modelsPanel = new JPanel();
		modelsPanel.setBorder(new TitledBorder(null, "Model", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		verticalBox.add(modelsPanel);
		modelsPanel.setLayout(new BoxLayout(modelsPanel, BoxLayout.Y_AXIS));

		ButtonGroup modelsButtonGroup = new ButtonGroup();
		JRadioButton rdbtnBarabasiAlbertGraph = new JRadioButton(Constants.TOPOLOGY_BARABASI_ALBERT_GRAPH);
		rdbtnBarabasiAlbertGraph.setEnabled(false);
		modelsPanel.add(rdbtnBarabasiAlbertGraph);
		modelsButtonGroup.add(rdbtnBarabasiAlbertGraph);
		JRadioButton rdbtnTopologywattsstrogatzgraph = new JRadioButton(Constants.TOPOLOGY_WATTS_STROGATZ_GRAPH);
		rdbtnTopologywattsstrogatzgraph.setEnabled(false);
		modelsPanel.add(rdbtnTopologywattsstrogatzgraph);
		modelsButtonGroup.add(rdbtnTopologywattsstrogatzgraph);
		JRadioButton rdbtnErdosRenyiGNBRandomGraph = new JRadioButton(Constants.TOPOLOGY_ERDOS_RENYI_GNP_RANDOM_GRAPH);
		rdbtnErdosRenyiGNBRandomGraph.setEnabled(false);
		modelsPanel.add(rdbtnErdosRenyiGNBRandomGraph);
		modelsButtonGroup.add(rdbtnErdosRenyiGNBRandomGraph);
		JRadioButton rdbtnKlienbergSmallWorldGraph = new JRadioButton(Constants.TOPOLOGY_KLEINBERG_SMALL_WORLD_GRAPH);
		rdbtnKlienbergSmallWorldGraph.setEnabled(false);
		modelsPanel.add(rdbtnKlienbergSmallWorldGraph);
		modelsButtonGroup.add(rdbtnKlienbergSmallWorldGraph);
		rdbtnBarabasiAlbertGraph.setSelected(true);
		
		JCheckBox chckbxInverted = new JCheckBox("inverted");
		chckbxInverted.setEnabled(false);
		verticalBox.add(chckbxInverted);
		
		JPanel manageStubbornPanel = new JPanel();
		manageStubbornPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Manage Stupporn", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		verticalBox.add(manageStubbornPanel);
		manageStubbornPanel.setLayout(new BoxLayout(manageStubbornPanel, BoxLayout.Y_AXIS));
		
		JRadioButton rdbtnNone = new JRadioButton("None");
		rdbtnNone.setEnabled(false);
		manageStubbornPanel.add(rdbtnNone);
		
		JRadioButton rdbtnPolarizeSingle = new JRadioButton("Polarize Single");
		rdbtnPolarizeSingle.setEnabled(false);
		manageStubbornPanel.add(rdbtnPolarizeSingle);
		
		JRadioButton rdbtnPolarizeCouple = new JRadioButton("Polarize Couple");
		rdbtnPolarizeCouple.setEnabled(false);
		manageStubbornPanel.add(rdbtnPolarizeCouple);
		
		




	}

	private class OpenAction extends AbstractAction {
		private static final long serialVersionUID = 2368183798042672044L;
		JFileChooser chooser = new JFileChooser();
		public OpenAction() {
			putValue(NAME, "Open...");
			putValue(SHORT_DESCRIPTION, "Open a simulation folder");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			PointND.PointNDSupplier pointNDSupplier = new PointND.PointNDSupplier(3, Constants.CASTOR);
			DefaultDirectedGraph<PointND, DefaultEdge> graphCC = new DefaultDirectedGraph<>(pointNDSupplier, null, false);
			pointNDSupplier = new PointND.PointNDSupplier(3, Constants.PULLOX);
			DefaultDirectedGraph<PointND, DefaultEdge> graphPP = new DefaultDirectedGraph<>(pointNDSupplier, null, false);
			@SuppressWarnings("unchecked")
			Graph<PointND, DefaultEdge>[] graphs = (Graph<PointND, DefaultEdge>[]) new Graph[] {graphCC, null, null, graphPP};

			FileNameExtensionFilter filter = new FileNameExtensionFilter("Log files", "log");
		    chooser.setFileFilter(filter);
//		    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		    int returnVal = chooser.showOpenDialog(SimulationFrame.this);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
//		    	System.out.println("You chose to open this file: " + chooser.getSelectedFile().getName());
		    	final File selectedFolder = chooser.getSelectedFile();
		    	String id = selectedFolder.getName();
		    	String paramsString = id.replaceFirst("^", "-").replaceAll(",", " -").replaceAll("_", " ");
		    	String[] args = paramsString.split(" ");
		    	int n = Integer.valueOf(Utils.getParameter(args, "-numCouples", "-1", "400"));
		    	File fileGG = new File(selectedFolder, String.format("GG-%s.log", id));
		    	File filePP = new File(selectedFolder, String.format("PP-%s.log", id));
		    	try {
					GraphsIO.importGraph(Constants.CASTOR, 3, graphCC, fileGG);
		    		GraphsIO.importGraph(Constants.PULLOX, 3, graphPP, filePP);
		    	} catch (ImportException e1) {
		    		e1.printStackTrace();
		    	}
		    	if (SimulationFrame.this.verbose) {
					System.out.println(graphCC);
					System.out.println(graphPP);
				}
		    	PointND[] castorPointNDs = graphCC.vertexSet().toArray(new PointND[0]);
		    	PointND[] pulloxPointNDs = graphPP.vertexSet().toArray(new PointND[0]);
		    	//check for consistency
		    	for (int i = 0; i < pulloxPointNDs.length; i++) {
		    		PointND pointC = castorPointNDs[i];
		    		PointND pointP = pulloxPointNDs[i];
		    		if (pointC.getId() != pointP.getId()) {
		    			throw new RuntimeException("points are not corresponding: "+ pointC + ", " + pointP);
		    		}
		    	}
		    	
		    	GraphPanel<PointND, DefaultEdge> graphPanel = getGraphPanel();
		    	graphPanel.setGraphs(graphs);
		    	
		    	File xFile = new File(selectedFolder, String.format("x-%s.log", id));
		    	parser = new Parser(n, 3, xFile);
		    	states = parser.parse();
				float[][] stateZero = states.get(0);
				
				PointND[] points = new PointND[stateZero.length];
				System.arraycopy(castorPointNDs, 0, points, 0, n);
				System.arraycopy(pulloxPointNDs, 0, points, n, n);
				
				x = new OpinionsMatrix(3, n, false);
		    	x.set(points);
		    	
		    	int maxKey=0;
		    	Set<Integer> keys = states.keySet();
		    	for (Iterator<Integer> iterator = keys.iterator(); iterator.hasNext();) {
					Integer key = iterator.next();
					if(key > maxKey) {
						maxKey = key;
					}
				}
		    	maxFrame = maxKey;
				slider.setValue(0);
				slider.setMinimum(0);
				slider.setMaximum(maxFrame);
		    	setdisplayAndCloseControlsEnabled(true);
		    	
		    	x.match(stateZero);
		    	graphPanel.repaint();
		    }
		}
	}
	public GraphPanel<PointND, DefaultEdge> getGraphPanel() {
		return graphPanel;
	}
	protected JSlider getSlider() {
		return slider;
	}
	protected JSpinner getFrameSpinner() {
		return frameSpinner;
	}
	protected JButton getPlayPauseButton() {
		return playPauseButton;
	}
	public JButton getStopButton() {
		return stopButton;
	}
	protected JMenuItem getMntmClose() {
		return mntmClose;
	}

	void setdisplayAndCloseControlsEnabled(boolean enabled) {
		getSlider().setEnabled(enabled);
		getFrameSpinner().setEnabled(enabled);
		getPlayPauseButton().setEnabled(enabled);
		getStopButton().setEnabled(enabled);
	}
	private class PlayPauseAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private boolean playing = false;
		int next = 0;
		public PlayPauseAction() {
			putValue(NAME, "> / ||");
			putValue(SHORT_DESCRIPTION, "Play / Pause");
		}
		public void actionPerformed(ActionEvent e) {
			playing = ! playing;
			if(playing) {
				new Thread() {
					@Override
					public void run() {
						float[][] nextState;
						while (playing) {
							//show next
							while ((nextState = states.get(next++)) == null) {
								//just advance and check
								if (next > maxFrame) {
									playing = false;
								}
							}
							
							slider.setValue(next);
							
							//sleep
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}.start();
			}
		}
	}
}
