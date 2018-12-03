package sg.edu.ntu.jopinions.control.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.Attribute;
import org.jgrapht.io.CSVExporter;
import org.jgrapht.io.CSVFormat;
import org.jgrapht.io.CSVImporter;
import org.jgrapht.io.EdgeProvider;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.ImportException;
import org.jgrapht.io.IntegerComponentNameProvider;
import org.jgrapht.io.VertexProvider;

import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.models.Utils;

public class GraphsIO {
	static final IntegerComponentNameProvider<PointND> vertexIDProvider = new IntegerComponentNameProvider<PointND>();
	static final CSVExporter<PointND, DefaultEdge> exporter = new CSVExporter<>(vertexIDProvider, CSVFormat.ADJACENCY_LIST, ',');
//	exporter.setParameter(CSVFormat.Parameter.EDGE_WEIGHTS, true);
	
	static final PointNDVertixProvider vertexProvider = new PointNDVertixProvider(3, null);
	static final EdgeProvider<PointND, DefaultEdge> edgeProvider = (from, to, label, attributes) -> new DefaultEdge();
	static final CSVImporter<PointND, DefaultEdge> importer = new CSVImporter<PointND, DefaultEdge>(vertexProvider, edgeProvider, CSVFormat.ADJACENCY_LIST, ',');

	public static void export(Graph<PointND, DefaultEdge> graph, OutputStreamWriter writer) throws ExportException{
		vertexIDProvider.clear();
		exporter.exportGraph(graph, writer);
	}
	
	public static void export(Graph<PointND, DefaultEdge> graph, PrintStream stream) {
		try {
			export(graph, new OutputStreamWriter(stream, StandardCharsets.UTF_8));
		} catch (ExportException e1) {
			e1.printStackTrace();
		}
	}

	public static void export(Graph<PointND, DefaultEdge> graph, File file) {
		try {
			export(graph, new FileWriter(file));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ExportException e) {
			e.printStackTrace();
		}
	}

	/**This function <b>MUST</b> be called before reading a graph
	 * @param d number of dimensions
	 * @param name point name (Castor or Pollux)
	 */
	private static void prepareToImport(int d, String name) {
		vertexProvider.setD(d);
		vertexProvider.setName(name);
	}
	
	
	public static void importGraph(String name, int d, Graph<PointND, DefaultEdge> g, File file) throws ImportException{
		prepareToImport(d, name);
		importer.importGraph(g, file);
		Utils.cacheVerticesDegrees(g);
	}
	
	static class PointNDVertixProvider implements VertexProvider<PointND>{
		private int d;
		private String name;
		
		public PointNDVertixProvider(int d, String name) {
			this.d = d;
			PointND.setNumDimensions(d);
			this.name = name;
		}
		public void setD(int d) {
			this.d = d;
		}
		public void setName(String name) {
			this.name = name;
		}
		@Override
		public PointND buildVertex(String id, Map<String, Attribute> attributes) {
			return new PointND(name, new float[d], Integer.valueOf(id));
		}
	}
	
}
