package sg.edu.ntu.jopinions.views;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import sg.edu.ntu.jopinions.Defaults;
import sg.edu.ntu.jopinions.models.PointND;

public class GraphPanel<V, E> extends JPanel implements ComponentListener, MouseInputListener, KeyListener{
	
	private static final long serialVersionUID = 4891309553805247705L;

	private Graph<PointND, DefaultEdge>[] graphs;

	private float xRatio;
	private float yRatio;

	private PointND[] castorPointNDs;
	private PointND[] pulloxPointNDs;

	private PointND[] castorSourcesNoLoop;
	private PointND[] castorTargetsNoLoop;
	private PointND[] pulloxSourcesNoLoop;
	private PointND[] pulloxTargetsNoLoop;

	private Point pressPoint;
//	private Point releasePoint;
	private Point currentPoint;

	private int xTranslation = 0;
	private int yTranslation = 0;

	private boolean zooming = false;
	private boolean button3Down = false;

	private boolean showNodeDetails = false;
	
	public GraphPanel() {
		this.addComponentListener(this);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.setFocusable(true);
		this.addKeyListener(this);
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
			g.drawLine((int)(xRatio * castorX[0]) + xTranslation, (int)(yRatio * castorX[1]) + yTranslation,
					   (int)(xRatio * pulloxX[0]) + xTranslation, (int)(yRatio * pulloxX[1]) + yTranslation);
		}
		
		//draw Pullox edges
		g.setColor(Defaults.COLOR_PULLOX);
		for (int i = 0; i < pulloxSourcesNoLoop.length; i++) {
			float[] sourceCoord = pulloxSourcesNoLoop[i].getX_i();
			float[] targetCoord = pulloxTargetsNoLoop[i].getX_i();
			g.drawLine((int)(xRatio * sourceCoord[0]) + xTranslation, (int)(yRatio * sourceCoord[1]) + yTranslation, 
					   (int)(xRatio * targetCoord[0]) + xTranslation, (int)(yRatio * targetCoord[1]) + yTranslation);
		}
		//draw Castor edges
		g.setColor(Defaults.COLOR_CASTOR);
		for (int i = 0; i < castorSourcesNoLoop.length; i++) {
			float[] sourceCoord = castorSourcesNoLoop[i].getX_i();
			float[] targetCoord = castorTargetsNoLoop[i].getX_i();
			g.drawLine((int)(xRatio * sourceCoord[0]) + xTranslation, (int)(yRatio * sourceCoord[1]) + yTranslation, 
					   (int)(xRatio * targetCoord[0]) + xTranslation, (int)(yRatio * targetCoord[1]) + yTranslation);
		}
		
		//draw Castor Vertices
		g.setColor(Defaults.COLOR_CASTOR);
		for (int i = 0; i < castorPointNDs.length; i++) {
			float[] opinion = castorPointNDs[i].getX_i();
			int pointX = (int) (xRatio * opinion[0]);
			int pointY = (int) (yRatio * opinion[1]);
			g.fillOval((pointX - 3) + xTranslation, (pointY - 3) + yTranslation, 6, 6);
		}
		//draw Pullox Vertices
		g.setColor(Defaults.COLOR_PULLOX);
		for (int i = 0; i < pulloxPointNDs.length; i++) {
			float[] opinion = pulloxPointNDs[i].getX_i();
			int pointX = (int) (xRatio * opinion[0]);
			int pointY = (int) (yRatio * opinion[1]);
			g.fillOval((pointX - 3) + xTranslation, (pointY - 3) + yTranslation, 6, 6);
		}
		
		if (showNodeDetails) {
			float minX= (0 - xTranslation)/xRatio;
			float minY= (0 - yTranslation)/yRatio;
			float maxX= (getWidth() - xTranslation)/xRatio;
			float maxY= (getHeight()- yTranslation)/yRatio;
			showNodeDetails(minX, minY, maxX, maxY, castorPointNDs, g);
			showNodeDetails(minX, minY, maxX, maxY, pulloxPointNDs, g);
		}
		
		
		Point pressPoint = this.pressPoint;
		Point currentPoint = this.currentPoint;
		if(pressPoint != null && currentPoint != null) {
			int x = Math.min(pressPoint.x, currentPoint.x);
			int y = Math.min(pressPoint.y, currentPoint.y);
			int w = Math.abs(pressPoint.x - currentPoint.x);
			int h = Math.abs(pressPoint.y - currentPoint.y);
			g.setColor(Color.BLACK);
			g.drawRect(x, y, w, h);
		}
	}

	private void showNodeDetails(float minX, float minY, float maxX, float maxY, PointND[] points, Graphics g) {
		Color color;
		int shift;
		if (points[0].toString().startsWith("C")) {
			color = Defaults.COLOR_CASTOR;
			shift = 0;
		} else {
			color = Defaults.COLOR_PULLOX;
			shift = g.getFontMetrics().getHeight();
		}
		for (int i = 0; i < points.length; i++) {
			PointND point = points[i];
			float[] coords = point.getX_i();
			final float x = coords[0];
			final float y = coords[1];
			if (x < minX || x > maxX || y < minY || y > maxY)
				continue;
			g.setColor(color);
			g.drawString(String.format("%s%d(%d)", point.getName(), point.getId(),point.getInDegree()),
					(int)(x*xRatio + xTranslation), (int)(y*yRatio + yTranslation + shift));
		}
	}




	@Override
	public void componentResized(ComponentEvent e) {
		//0 and 1.0 are reserved frame and maxRange
		this.xRatio = (getWidth() - 0) / 1.0f;
		this.yRatio = (getHeight()- 0) / 1.0f;
	}
	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentShown(ComponentEvent e) {}
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
		Set<DefaultEdge> pulloxEdgeSet = graphPP.edgeSet();
		
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


	@Override
	public void mouseMoved(MouseEvent e) {}
	@Override
	public void mouseClicked(MouseEvent e) {
		if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0) {
			this.xRatio /= 2.0;
			this.yRatio /= 2.0;
			repaint();
		}
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		this.zooming = true;
		this.currentPoint = e.getPoint();
		repaint();
	}
	@Override
	public void mousePressed(MouseEvent e) {
		pressPoint = e.getPoint();
		if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK)  == InputEvent.BUTTON3_DOWN_MASK) {
			this.button3Down = true;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (this.button3Down && (e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != InputEvent.BUTTON3_DOWN_MASK ) {
			float viewportZeroOriginalX = (0 - xTranslation) / xRatio;
			float viewportZeroOriginalY = (0 - yTranslation) / yRatio;
			this.xRatio /= 2.0;
			this.yRatio /= 2.0;
			this.xTranslation = -((int)(viewportZeroOriginalX*xRatio) - getWidth()/4);
			this.yTranslation = -((int)(viewportZeroOriginalY*yRatio) - getHeight()/4);
			this.button3Down = false;
		}
		if (zooming) {
			zooming = false;
			Point releasePoint = 
					/*this.releasePoint=*/ 
					e.getPoint();
			Point pressPoint = this.pressPoint;
			this.pressPoint = this.currentPoint = null;
			int x = Math.min(pressPoint.x, releasePoint.x);
			int y = Math.min(pressPoint.y, releasePoint.y);
			int w = Math.abs(pressPoint.x - releasePoint.x);
			int h = Math.abs(pressPoint.y - releasePoint.y);
			float originalX = (x - xTranslation) / xRatio;
			float originalY = (y - yTranslation) / yRatio;
			float originalW = w / xRatio;
			float originalH = h / yRatio; //if we want to include every pixel in the zoomed-in box, it may need to be a little more complicated
			//now take the action
			this.xRatio = (getWidth() / originalW);
			this.yRatio = (getHeight()/ originalH);
			this.xTranslation = - ((int) (originalX*xRatio));
			this.yTranslation = - ((int) (originalY*yRatio));
		}
		repaint();
	}

	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}

	public void finishedSimulation() {
		setBackground(Color.WHITE);
		repaint();
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			showNodeDetails = true;
			repaint();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			showNodeDetails = false;
			repaint();
		}
	}
	
}
