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

package frodo2.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTree;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import frodo2.communication.MessageType;
import frodo2.gui.Visualizer.MsgVisualization;

/** A GUI to control how long messages are displayed
 * @author Thomas Leaute
 */
public class VisualizerControls extends JFrame implements ActionListener, TreeExpansionListener {
	
	/** A message type and its slider */
	public class MsgTypeSetting extends DefaultMutableTreeNode implements ChangeListener {
		
		/** Serialization ID */
		private static final long serialVersionUID = 5602894741439533664L;

		/** The message type */
		private MessageType type;
		
		/** The slider to set how/whether to display the message type */
		private JSlider slider;

		/** Constructor 
		 * @param type 		the message type
		 */
		public MsgTypeSetting(MessageType type) {
			super();
			
			this.type = type;
			
			slider = new JSlider (0, 2);
			slider.setMajorTickSpacing(1);
			slider.setSnapToTicks(true);
			slider.setPaintTicks(true);
			slider.setName(type.toString());

			// Only display labels for the root message type slider to save space
			if (MessageType.ROOT.equals(type)) {
				Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel> ();
				labelTable.put(0, new JLabel("Hide"));
				labelTable.put(1, new JLabel(Visualizer.getDisplayTime() + " ms"));
				labelTable.put(2, new JLabel("Step"));			
				slider.setLabelTable(labelTable);
				slider.setPaintLabels(true);
				slider.setValue(2);
				
			} else { // not the root message type

				// Retrieve the visualization strategy for this message type and set the slider accordingly
				switch (Visualizer.getMsgViz(type)) {
				case NONE:
					slider.setValue(0);
					break;
				case TIMED:
					slider.setValue(1);
					break;
				default:
					slider.setValue(2);
				}
			}
			
			// Listen to the slider
			slider.addChangeListener(this);
		}

		/** @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent) */
		@Override
		public void stateChanged(ChangeEvent e) {
			
			assert this.slider == e.getSource(); // each MsgTypeSetting listens to its own slider
			
			// Don't do anything while the user hasn't released the slider
			if (slider.getValueIsAdjusting()) 
				return;
			
			MsgVisualization viz;
			final int sliderVal = slider.getValue();
			switch (sliderVal) {
			case 0:
				viz = MsgVisualization.NONE;
				break;
			case 1:
				viz = MsgVisualization.TIMED;
				break;
			default:
				viz = MsgVisualization.STEPPED;
			}
			
			Visualizer.setMsgViz(this.type, viz);
			
			// If this message type is collapsed, apply the setting to all its children
			if (! tree.isExpanded(new TreePath (this.getPath()))) {
				for (Enumeration<TreeNode> enumeration = this.depthFirstEnumeration(); enumeration.hasMoreElements(); ) {
					MsgTypeSetting node = (MsgTypeSetting) enumeration.nextElement();
					Visualizer.setMsgViz(node.type, viz);
					node.slider.setValue(sliderVal);
				}
			}
		}
		
		/** @see javax.swing.tree.DefaultMutableTreeNode#toString() */
		@Override
		public String toString () {
			return "Display setting for `" + this.type + "': " + this.slider.getValue();
		}
	}

	/** Renders each node in the tree as a message type name and a slider */
	public static class MsgTypeRenderer implements TreeCellRenderer, TreeCellEditor {

		/** Empty constructor */
		public MsgTypeRenderer() {}

		/** @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean) */
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			
			MsgTypeSetting setting = (MsgTypeSetting) value;

			JPanel panel = new JPanel ();
			FlowLayout layout = new FlowLayout ();
			panel.setLayout(layout);
			panel.setBackground(Color.WHITE);

			panel.add(new JLabel (setting.type.getLeafType()));
			panel.add(setting.slider);
			
			Dimension dim = panel.getPreferredSize();
			dim.height = MessageType.ROOT.equals(setting.type) ? 55 : 35;
			panel.setPreferredSize(dim);
			
			return panel;
		}

		/** @see javax.swing.CellEditor#getCellEditorValue() */
		@Override
		public Object getCellEditorValue() {
			return null;
		}

		/** @see javax.swing.CellEditor#isCellEditable(java.util.EventObject) */
		@Override
		public boolean isCellEditable(EventObject anEvent) {
			return true;
		}

		/** @see javax.swing.CellEditor#shouldSelectCell(java.util.EventObject) */
		@Override
		public boolean shouldSelectCell(EventObject anEvent) {
			return false;
		}

		/** @see javax.swing.CellEditor#stopCellEditing() */
		@Override
		public boolean stopCellEditing() {
			return true;
		}

		/** @see javax.swing.CellEditor#cancelCellEditing() */
		@Override
		public void cancelCellEditing() { }

		/** @see javax.swing.CellEditor#addCellEditorListener(javax.swing.event.CellEditorListener) */
		@Override
		public void addCellEditorListener(CellEditorListener l) { }

		/** @see javax.swing.CellEditor#removeCellEditorListener(javax.swing.event.CellEditorListener) */
		@Override
		public void removeCellEditorListener(CellEditorListener l) { }

		/** @see javax.swing.tree.TreeCellEditor#getTreeCellEditorComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int) */
		@Override
		public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
				boolean leaf, int row) {
			
			MsgTypeSetting setting = (MsgTypeSetting) value;

			JPanel panel = new JPanel ();
			FlowLayout layout = new FlowLayout ();
			panel.setLayout(layout);
			panel.setBackground(Color.WHITE);

			panel.add(new JLabel (setting.type.getLeafType()));
			panel.add(setting.slider);
			
			Dimension dim = panel.getPreferredSize();
			dim.height = MessageType.ROOT.equals(setting.type) ? 55 : 35;
			panel.setPreferredSize(dim);
			
			return panel;
		}

	}
	/** Serial version UID */
	private static final long serialVersionUID = 3541530360724833030L;

	/** The name of the STEP button */
	private static final String STEP_BUTTON_NAME = "Step";

	/** The tree containing the settings */
	private JTree tree;
	
	/** The root of the message type hierarchy */
	private MsgTypeSetting root;
	
	/** The tree nodes, indexed by message type */
	private HashMap< MessageType, MsgTypeSetting> nodes = new HashMap< MessageType, MsgTypeSetting > ();

	/** The tree model used to dynamically add nodes in a thread-safe way */
	private DefaultTreeModel model;

	/** The STEP button */
	private JButton stepBtn;
	
	/** Constructor */
	public VisualizerControls() {
		super("Visualization settings per message type");
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		// Add the STEP button
		this.setLayout(new BoxLayout (this.getContentPane(), BoxLayout.Y_AXIS));
		JPanel stepPanel = new JPanel ();
		stepPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		stepBtn = new JButton (STEP_BUTTON_NAME);
		stepBtn.addActionListener(this);
		stepPanel.add(stepBtn);
		this.getContentPane().add(stepPanel);
		
		// Add the settings panel
		this.nodes.put(MessageType.ROOT, this.root = new MsgTypeSetting (MessageType.ROOT));
		this.model = new DefaultTreeModel (this.root);
		this.getContentPane().add(new JScrollPane (this.tree = new JTree (model)));
		tree.setShowsRootHandles(true);
		tree.setRowHeight(0);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		MsgTypeRenderer renderer = new MsgTypeRenderer();
		tree.setCellRenderer(renderer);
		tree.setCellEditor(renderer);
		tree.setEditable(true);
		tree.setExpandsSelectedPaths(true);
		tree.setScrollsOnExpand(true);
		tree.addTreeExpansionListener(this);
						
		this.setLocation(0, 0);	// top left
		this.setVisible(true);
	}
	
	/** Adds a new control panel for a message type
	 * @param msgType 	the message type
	 */
	void addControlPanel (MessageType msgType) {
		this.getTreeNode(msgType);
		/// @todo Auto-expand
		this.pack();
	}

	/** Retrieves the node for the input message type
	 * @param type 	the message type
	 * @return the node in the tree for this message type (newly created if not yet existent)
	 */
	private MsgTypeSetting getTreeNode(MessageType type) {
		
		MsgTypeSetting node = this.nodes.get(type);
		
		if (node == null) {
			node = new MsgTypeSetting (type);
			this.nodes.put(type, node);
			
			if (! MessageType.ROOT.equals(type)) {
				MsgTypeSetting parent = this.getTreeNode(type.getParent());
				this.model.insertNodeInto(node, parent, parent.getChildCount());
			}
		}
		
		return node;
	}

	/** @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent) */
	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized (this) {
			this.notifyAll(); // the STEP button was pressed
		}
	}

	/** @see javax.swing.event.TreeExpansionListener#treeExpanded(javax.swing.event.TreeExpansionEvent) */
	@Override
	public void treeExpanded(TreeExpansionEvent event) {
		this.pack();
	}

	/** @see javax.swing.event.TreeExpansionListener#treeCollapsed(javax.swing.event.TreeExpansionEvent) */
	@Override
	public void treeCollapsed(TreeExpansionEvent event) {
		this.pack();
	}
	
	/** Enables or disables the STEP button
	 * @param enabled 	whether the STEP button should be enabled
	 */
	public void enableStepButton (final boolean enabled) {
		this.stepBtn.setEnabled(enabled);
	}
	
}
