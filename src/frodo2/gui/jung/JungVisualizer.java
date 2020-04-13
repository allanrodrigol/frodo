/*
FRODO: a FRamework for Open/Distributed Optimization
Copyright (C) 2008-2019  Thomas Leaute, Brammert Ottens & Radoslaw Szymanek

FRODO is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

FRODO is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


How to contact the authors: 
<https://frodo-ai.tech>
 */

package frodo2.gui.jung;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import frodo2.communication.Message;
import frodo2.gui.Visualizer;
import frodo2.solutionSpaces.DCOPProblemInterface;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;

/** A Visualizer using JUNG2
 * @author Thomas Leaute
 * @see "http://jung.sourceforge.net"
 */
public class JungVisualizer extends Visualizer implements ActionListener {
	
	/** The JUNG graph */
	private Graph<String, String> graph = new SparseGraph<String, String>();
	
	/** The JUNG visualization viewer */
	private VisualizationViewer<String,String> vv;
	
	/** The problem instance to be rendered */
	private DCOPProblemInterface<?, ?> problem;
	
	/** The color assigned to each agent */
	private HashMap<String, Color> agentColors = new HashMap<String, Color> ();
	
	/** The color of each vertex */
	private LoadingCache<String, Paint> vertexColors =
			CacheBuilder.newBuilder().build(
					CacheLoader.from(Functions.<Paint>constant(Color.white)));
	
	/** If true, then the nodes are the agents */
	private boolean compiled = false;

	/** The frame */
	private JFrame frame;
	
	/** The layout */
	private AbstractLayout<String, String> layout;
	
	/** For each agent (name), its incoming message */
	private ConcurrentHashMap<String, Message> inMsgs = new ConcurrentHashMap<String, Message> ();

	/** Constructor */
	public JungVisualizer() { }
	
	/** Initializes the VisualizationViewer */
	private void init () {
		
		/// @todo Make the layout parameters configurable in the GUI
		
//		CircleLayout<String, String> layout = new CircleLayout<String, String>(graph);
//		layout.setRadius(100.0);
//		layout.initialize();

//		FRLayout<String, String> layout = new FRLayout<String, String>(graph);
//		layout.setAttractionMultiplier(1000.0);
//		layout.setRepulsionMultiplier(1000.0);
//		layout.setMaxIterations(100000);
		
//		FRLayout2<String, String> layout = new FRLayout2<String, String>(graph); /// @todo Not much different from FRLayout 
//		layout.setAttractionMultiplier(1000.0);
//		layout.setRepulsionMultiplier(1000.0);
//		layout.setMaxIterations(100000);

		layout = new ISOMLayout<String, String>(graph);

		vv = new VisualizationViewer<String,String>(layout);
		vv.setPreferredSize(new Dimension(500, 500)); //Sets the viewing area size
		
		// Adjust the graph formatting 
		vv.setBackground( Color.white );
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vv.getRenderContext().setVertexFillPaintTransformer(this.vertexColors);
        vv.getRenderContext().setEdgeShapeTransformer(EdgeShape.line(graph));
        ToolTipManager.sharedInstance().setDismissDelay(100000); // 100 s
        vv.setGraphMouse(new DefaultModalGraphMouse<Integer, Number> ());
        
        // Set the vertex size to fit the label
        this.vv.getRenderContext().setVertexShapeTransformer(new Function<String, Shape> () {
			public Shape apply(String vertex) {
				
				Font font = vv.getRenderContext().getVertexFontTransformer().apply(vertex);
				DefaultVertexLabelRenderer labelRenderer = (DefaultVertexLabelRenderer) vv.getRenderContext().getVertexLabelRenderer()
						.getVertexLabelRendererComponent(vv, vertex, font, false, vertex);
				Dimension labelDim = labelRenderer.getPreferredSize();
				
				return new Rectangle2D.Float(- (labelDim.width + 10) / 2, - (labelDim.height + 10) / 2, 
						labelDim.width + 10, labelDim.height + 10);
			}
        });
        
        // Display picked vertices differently
        this.vv.getRenderContext().setVertexFillPaintTransformer(new Function<String, Paint> () {
			public Paint apply(String vertex) {
				
				if (vv.getRenderContext().getPickedVertexState().isPicked(vertex)) 
					return Color.LIGHT_GRAY;
				else 
					return Color.WHITE;
			}
        });
       
        // Set the vertex tooltip to display the incoming message
        vv.setVertexToolTipTransformer(new com.google.common.base.Function<String, String> () {
			public String apply(String agentName) {
				Message msg = inMsgs.get(agentName);
				return msg == null ? agentName : toHTML("Processing message from inbox:\n\n" + msg.toString());
			}
        });
      
        // Create the graph frame
		frame = new JFrame("Agent graph"); /// @todo Update the title according to the display mode
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.getContentPane().add(new GraphZoomScrollPane(vv));
		frame.pack();
		
		// Create the zoom buttons
		JPanel controls = new JPanel ();
		
		CrossoverScalingControl scaler = new CrossoverScalingControl();
		
	    JButton minus = new JButton("-");
	    minus.addActionListener(e -> scaler.scale(vv, 1 / 1.1f, vv.getCenter()));
	    controls.add(minus);

	    /// @bug The Reset button should scale back to layout
	    JButton reset = new JButton("reset");
	    reset.addActionListener(
	        e -> {
	          vv.getRenderContext()
	              .getMultiLayerTransformer()
	              .getTransformer(Layer.LAYOUT)
	              .setToIdentity();
	          vv.getRenderContext()
	              .getMultiLayerTransformer()
	              .getTransformer(Layer.VIEW)
	              .setToIdentity();
	        });
	    controls.add(reset);

	    JButton plus = new JButton("+");
	    plus.addActionListener(e -> scaler.scale(vv, 1.1f, vv.getCenter()));
	    controls.add(plus);
	    
	    // Add the layout selector
	    String[] layouts = new String[] {
	    		CircleLayout.class.getSimpleName(),
//	    		DAGLayout.class.getSimpleName(),  /// @todo For directed trees pointing upward 
	    		FRLayout.class.getSimpleName(), 
	    		FRLayout2.class.getSimpleName(), 
	    		ISOMLayout.class.getSimpleName(), /// @bug Keeps jumping around
	    		KKLayout.class.getSimpleName(), /// @bug Results in unnecessary edge crossings
	    		SpringLayout.class.getSimpleName(), /// @bug Typically doesn't converge
	    		SpringLayout2.class.getSimpleName() /// @bug Apparently experimental 
	    };
	    JComboBox<String> dropdown = new JComboBox <String> (layouts);
	    controls.add(dropdown);
	    dropdown.addActionListener(this);
	    dropdown.setSelectedItem(ISOMLayout.class.getSimpleName());

		this.frame.getContentPane().add(controls, BorderLayout.SOUTH);

		// Center the frame 
		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension dim = frame.getPreferredSize();
		frame.setSize(dim);
		frame.setLocation(screenDim.width / 2 - dim.width / 2, screenDim.height / 2 - dim.height / 2);	//Center the window
		frame.setVisible(true);
	}

	/** @see Visualizer#render(DCOPProblemInterface) */
	@Override
	public void render (DCOPProblemInterface<?, ?> problem) {
		
		this.problem = problem;
		
		if (problem.getAgent() != null) 
			this.frame.setTitle("Agent graph seen from Agent `" + problem.getAgent() + "'"); /// @todo Update the title according to the display mode
		
		this.render();

		if (this.vv == null) 
			this.init();	
	}

	/** Renders the problem instance */
	private void render() {
		
		/// @todo Assign a color to each agent 
		this.agentColors.clear();
//		Random rnd = new Random ();
		for (String agent : this.problem.getAgents()) 
//			this.agentColors.put(agent, new Color (rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()));
			this.agentColors.put(agent, Color.WHITE);
		
		this.drawVertices();
		this.drawEdges();
	}
	
	/** Draws the vertices */
	private void drawVertices() {
		
		// Clear the current vertices
		for (String v : this.graph.getVertices()) 
			this.graph.removeVertex(v);
		
		if (this.compiled) { // the vertices are the agents
			
			for (String agent : this .problem.getAgents()) {
				if (! this.graph.addVertex(agent)) 
					System.err.println("Failed to add `" + agent + "' to the graph");
				else // vertex successfully added; color it accordingly
					this.vertexColors.put(agent, this.agentColors.get(agent));
			}

		} else { // not compiled; the vertices are the variables
			
			// Add the variables as vertices
			for (String var : this.problem.getVariables()) {
				if(! this.graph.addVertex(var)) 
					System.err.println("Failed to add `" + var + "' to the graph");
				else // vertex successfully added; color it according to its owner agent
					this.vertexColors.put(var, this.agentColors.get(this.problem.getOwner(var)));
			}
		}
	}

	/** Draws the edges */
	private void drawEdges() {
		
		// Remove all existing edges
		for (String e : this.graph.getEdges()) 
			this.graph.removeEdge(e);
		
		if (this.compiled) { // draw edges between agents
			
			// Draw an edge between any pair of neighboring agents
			for (String agent1 : this.problem.getAgents()) 
				for (Collection<String> agents : this.problem.getAgentNeighborhoods(agent1).values()) 
					for (String agent2 : agents) 
						if (agent1.compareTo(agent2) < 0) // avoid duplicate edges
							this.graph.addEdge(agent1 + "-" + agent2, agent1, agent2);
			
		} else { // not compiled

			// Draw an edge between any pair of variables involved in a common constraint
			for (Map.Entry< String, ? extends Collection<String> > entry : this.problem.getNeighborhoods().entrySet()) {
				String var1 = entry.getKey();

				for (String var2 : entry.getValue()) 
					if (var1.compareTo(var2) < 0) // avoid duplicate edges
						if(! this.graph.addEdge(var1 + "-" + var2, var1, var2)) 
							System.err.println("Failed to add the edge `" + var1 + "-" + var2 + "' to the graph");
			}
		}

	}

	/** @see Visualizer#setCompiled(boolean) */
	@Override
	public boolean setCompiled(boolean compiled) {
		boolean old = this.compiled;
		this.compiled = compiled;
		return old;
	}

	/** @see Visualizer#showOutgoingAgentMessage(java.lang.Object, Message, java.lang.Object, MsgVisualization) */
	@Override
	protected void showOutgoingAgentMessage(Object fromAgent, Message msg, Object toAgent, MsgVisualization viz) {
		
		String from = fromAgent.toString();
		String to = toAgent.toString();
		
		// Skip if either agent is not currently displayed
		if (! this.graph.containsVertex(from) || ! this.graph.containsVertex(to)) 
			return;
		
		// Highlight the sending agent 
		this.vv.getPickedVertexState().pick(from, true);
		
		// Replace the undirected edge with a directed edge
		String htmlEdge = toHTML("Sending message:\n\n" + msg.toString());
		this.graph.addEdge(htmlEdge, from, to, EdgeType.DIRECTED);
		if (from.compareTo(to) < 0) 
			this.graph.removeEdge(from + "-" + to);
		else if (from.compareTo(to) > 0) 
			this.graph.removeEdge(to + "-" + from);
		
		/// @todo Set the edge label to a message image 
		
		// Refresh
		this.vv.repaint(10); /// @bug Should be called from the event dispatching thread to avoid NullPointerExceptions 

		// Wait while the message is displayed
		this.wait(viz);
		
		// Undo
		if (from.compareTo(to) < 0) 
			this.graph.addEdge(from + "-" + to, from, to);
		else if (from.compareTo(to) > 0) 
			this.graph.addEdge(to + "-" + from, to, from);
		this.graph.removeEdge(htmlEdge);
		this.vv.getPickedVertexState().pick(from, false);

		// Refresh
		this.vv.repaint(10); /// @bug Should be called from the event dispatching thread to avoid NullPointerExceptions 
	}
	
	/** Converts an input string to HTML
	 * @param str 	the input string
	 * @return an HTML conversion of the input string
	 */
	static public String toHTML (String str) {
		
		str = str.replaceAll("\n", "<br>").replaceAll("\t", "&nbsp;&nbsp;");
		return "<html>" + str + "</html>";
	}

	/** @see Visualizer#showIncomingAgentMessage(Message, java.lang.Object, MsgVisualization) */
	@Override
	protected void showIncomingAgentMessage(Message msg, Object toAgent, MsgVisualization viz) {

		String to = toAgent.toString();

		// Highlight the agent 
		this.vv.getPickedVertexState().pick(to, true);
		this.inMsgs.put(to, msg); // looked up by the vertex tooltip function

		// Refresh
		this.vv.repaint(10); /// @bug Should be called from the event dispatching thread to avoid NullPointerExceptions 

		// Wait while the message is displayed
		this.wait(viz);
		
		// Undo
		/// @todo Only undo when the agent is finished processing the message
		this.vv.getPickedVertexState().pick(to, false);
		this.inMsgs.remove(to);

		// Refresh
		this.vv.repaint(10); /// @bug Should be called from the event dispatching thread to avoid NullPointerExceptions 
	}

	/** @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent) */
	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent e) {
		
		// Get the layout class name
		String layoutShortName = (String) ((JComboBox<String>) e.getSource()).getSelectedItem();
		String layoutClassName = ISOMLayout.class.getPackageName() + "." + layoutShortName;
		
		// Get the layout class
		Class<AbstractLayout<String, String>> layoutClass = null;
		try {
			layoutClass = (Class<AbstractLayout<String, String>>) Class.forName(layoutClassName);
		} catch (ClassNotFoundException e1) {
			System.err.println("JUNG layout class not found: " + layoutClassName);
		}
		
		// Instantiate and set the layout  
		try {
			this.layout = layoutClass.getConstructor(Graph.class).newInstance(this.graph);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e1) {
			System.err.println("Could not instantiate JUNG layout: " + layoutClass);
			e1.printStackTrace();
		}
		layout.setSize(new Dimension(300,300)); // sets the initial size of the space
		this.vv.setGraphLayout(layout);
        vv.setEdgeToolTipTransformer(new ToStringLabeller());
		
		// Auto-scale
		this.vv.scaleToLayout(new CrossoverScalingControl()); /// @bug Not working 
	}

}
