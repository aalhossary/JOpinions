package sg.edu.ntu.jopinions.control.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.models.PointND;

public class GraphPanel<V, E> extends JPanel implements ComponentListener{
	
	private static final long serialVersionUID = 1L;
	
	public static final Color CASTOR_COLOR = Color.RED;
	public static final Color PULLOX_COLOR = Color.BLUE;
	
	private Graph<PointND, DefaultEdge>[] graphs;

	private float xRatio;
	private float yRatio;

	private PointND[] castorPointNDs;
	private PointND[] pulloxPointNDs;

	private PointND[] castorSourcesNoLoop;
	private PointND[] castorTargetsNoLoop;
	private PointND[] pulloxSourcesNoLoop;
	private PointND[] pulloxTargetsNoLoop;
	
	public GraphPanel() {
		this.addComponentListener(this);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		float xRatio = this.xRatio, yRatio = this.yRatio;

		//TODO draw background
		
		Graph<PointND, DefaultEdge>[] graphs = this.graphs;
		if (graphs == null) {
			return;
		}
		//TODO draw BG and axes

		//draw Castor-Pullox edges
		g.setColor(Color.BLACK);
		for (int i = 0; i < castorPointNDs.length; i++) {
			float[] castorX = castorPointNDs[i].getX_i();
			float[] pulloxX = pulloxPointNDs[i].getX_i();
			g.drawLine((int)(xRatio * castorX[0]), (int)(yRatio * castorX[1]), (int)(xRatio * pulloxX[0]), (int)(yRatio * pulloxX[1]));
		}
		
		//draw Pullox edges
		g.setColor(PULLOX_COLOR);
		for (int i = 0; i < pulloxSourcesNoLoop.length; i++) {
			float[] sourceCoord = pulloxSourcesNoLoop[i].getX_i();
			float[] targetCoord = pulloxTargetsNoLoop[i].getX_i();
			g.drawLine((int)(xRatio * sourceCoord[0]), (int)(yRatio * sourceCoord[1]), (int)(xRatio * targetCoord[0]), (int)(yRatio * targetCoord[1]));
		}
		//draw Castor edges
		g.setColor(CASTOR_COLOR);
		for (int i = 0; i < castorSourcesNoLoop.length; i++) {
			float[] sourceCoord = castorSourcesNoLoop[i].getX_i();
			float[] targetCoord = castorTargetsNoLoop[i].getX_i();
			g.drawLine((int)(xRatio * sourceCoord[0]), (int)(yRatio * sourceCoord[1]), (int)(xRatio * targetCoord[0]), (int)(yRatio * targetCoord[1]));
		}
		
		//draw Castor Vertices
		g.setColor(CASTOR_COLOR);
		for (int i = 0; i < castorPointNDs.length; i++) {
			float[] opinion = castorPointNDs[i].getX_i();
			int pointX = (int) (xRatio * opinion[0]);
			int pointY = (int) (yRatio * opinion[1]);
			g.fillOval(pointX - 2, pointY - 2, 4, 4);
		}
		//draw Pullox Vertices
		g.setColor(PULLOX_COLOR);
		for (int i = 0; i < pulloxPointNDs.length; i++) {
			float[] opinion = pulloxPointNDs[i].getX_i();
			int pointX = (int) (xRatio * opinion[0]);
			int pointY = (int) (yRatio * opinion[1]);
			g.fillOval(pointX - 2, pointY - 2, 4, 4);
		}
	}




	@Override
	public void componentResized(ComponentEvent e) {
		//0 and 1.0 are reserver frame and maxRange
		this.xRatio = (getWidth() - 0) / 1.0f;
		this.yRatio = (getHeight()- 0) / 1.0f;
	}
	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentShown(ComponentEvent e) {
		componentResized(null);
	}
	@Override
	public void componentHidden(ComponentEvent e) {}
	
	public void setGraphs(Graph<PointND, DefaultEdge>[] graphs) {
		this.graphs = graphs;
		Graph<PointND, DefaultEdge> graphCC = graphs[0];
		Graph<PointND, DefaultEdge> graphPP = graphs[3];
		if (graphCC == null || graphPP == null) {
			return;
		}
		castorPointNDs = graphCC.vertexSet().toArray(new PointND[0]);
		pulloxPointNDs = graphPP.vertexSet().toArray(new PointND[0]);
		//check for consistency
		for (int i = 0; i < pulloxPointNDs.length; i++) {
			PointND pointC = castorPointNDs[i];
			PointND pointP = pulloxPointNDs[i];
			if (pointC.getId() != pointP.getId()) {
				throw new RuntimeException("points are not corresponding: "+ pointC + ", " + pointP);
			}
		}
		ArrayList<PointND> castorSourcesNoLoop = new ArrayList<>();
		ArrayList<PointND> castorTargetsNoLoop = new ArrayList<>();
		ArrayList<PointND> pulloxSourcesNoLoop = new ArrayList<>();
		ArrayList<PointND> pulloxTargetsNoLoop = new ArrayList<>();
		Set<DefaultEdge> castorEdgeSet = graphCC.edgeSet();
		Set<DefaultEdge> pulloxEdgeSet = graphCC.edgeSet();
		
		for (Iterator<DefaultEdge> iterator = castorEdgeSet.iterator(); iterator.hasNext();) {
			DefaultEdge edge = (DefaultEdge) iterator.next();
			PointND source = graphCC.getEdgeSource(edge);
			PointND target = graphCC.getEdgeTarget(edge);
			if (source != target) {
				castorSourcesNoLoop.add(source);
				castorTargetsNoLoop.add(target);
			}
		}
		for (Iterator<DefaultEdge> iterator = pulloxEdgeSet.iterator(); iterator.hasNext();) {
			DefaultEdge edge = (DefaultEdge) iterator.next();
			PointND source = graphPP.getEdgeSource(edge);
			PointND target = graphPP.getEdgeTarget(edge);
			if (source != target) {
				pulloxSourcesNoLoop.add(source);
				pulloxTargetsNoLoop.add(target);
			}
		}
		PointND[] temp = new PointND[0];
		this.castorSourcesNoLoop = castorSourcesNoLoop.toArray(temp);
		this.castorTargetsNoLoop = castorTargetsNoLoop.toArray(temp);
		this.pulloxSourcesNoLoop = pulloxSourcesNoLoop.toArray(temp);
		this.pulloxTargetsNoLoop = pulloxTargetsNoLoop.toArray(temp);
	}
}
