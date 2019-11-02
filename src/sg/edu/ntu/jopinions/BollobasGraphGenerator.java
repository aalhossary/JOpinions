/*
 * (C) Copyright 2017-2018, by Dimitrios Michail and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package sg.edu.ntu.jopinions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.util.SupplierUtil;

import sg.edu.ntu.jopinions.control.cli.GraphsIO;
import sg.edu.ntu.jopinions.models.PointND;
import sg.edu.ntu.jopinions.models.PointND.PointNDSupplier;
import sg.edu.ntu.jopinions.models.Utils;


public class BollobasGraphGenerator<V, E>
    implements
    GraphGenerator<V, E, V>
{
    private final Random rng;
    
//    /**initial graph*/
//    private Graph<V, E> g0 = null;
//    /**number of vertices in initial graph*/
//    private int v0 = -1;
//    /**number of edges in initial graph*/
//    private int t0 = -1;
    /**probability that the new edge is from a new vertex v to an 
     * existing vertex w, where w is chosen according to d_in + delta_in*/
    private final float alpha;
//    /**probability that the new edge is from an existing vertex v to an
//     * existing vertex w, where v and w are chosen independently, v 
//     * according to d_out + delta_out, and w according to d_in + delta_in.*/
//    private final float beta;
//    /**probability that the new edge is from an existing vertex v to a new vertex w, 
//     * where v is chosen according to d_out + delta_out.*/
//    private final float gamma;
    /**probability that the new edge is (from a new vertex v to an existing vertex w)
     * plus the probability that the new edge is (from an existing vertex v to an
    * existing vertex w), where v and w are chosen independently, v 
    * according to d_out + delta_out, and w according to d_in + delta_in.<br>
    * This equals 1 - gamma. Gamma refers to the probability that the new edge is 
    * from an existing vertex v to a new vertex w. */
    final float alphaPlusBeta;
    
    /**In-degree bias used for Alpha and Beta*/
    private final float deltaIn;
    /**Out-degree bias used for Beta and Gamma*/
    private final float deltaOut;
    /**Target total number of edges to reach. This is more accurate and has a higher priority than {@link #targetNodes}.<br>
     * It must be provided. If negative number, the user does not care about the total number of edges are 
     * and is interested only in the number of nodes, therefore, {@link #targetNodes} will be considered instead.
     * Otherwise, {@link #targetEdges} will be considered and {@link #targetEdges} will be neglected.*/
    private final int targetEdges;
    /**Target total number of targetNodes to reach.<br>
     * This has lower priority than {@link #targetEdges}. It will not be used unless {@link #targetEdges} given is a <i>negative</i> number.*/
    private final int targetNodes;

    public BollobasGraphGenerator(float alpha, float gamma, float dIn, float dOut, int targetEdges, int targetNodes){
    	this(alpha, gamma, dIn, dOut, targetEdges, targetNodes, new Random());
    }
    
    public BollobasGraphGenerator(float alpha, float gamma, float dIn, float dOut, int targetEdges, int targetNodes, long seed){
    	this(alpha, gamma, dIn, dOut, targetEdges, targetNodes, new Random(seed));
    }
    
    public BollobasGraphGenerator(float alpha, float gamma, float dIn, float dOut, int targetEdges, int targetNodes, Random rng){
    	this.alpha = alpha;
//    	this.gamma = gamma;
    	this.alphaPlusBeta = 1.0f - gamma;
    	this.deltaIn = dIn;
    	this.deltaOut = dOut;
    	this.targetEdges = targetEdges;
        this.targetNodes = targetNodes;
        this.rng = Objects.requireNonNull(rng, "Random number generator cannot be null");
    	
    	//TODO Do several checks on the parameters
    	
    }

    /**
     * Generates an instance.
     * 
     * @param target the target graph
     * @param resultMap not used by this generator, can be null
     */
    @Override
    public void generateGraph(Graph<V, E> target, Map<String, V> resultMap){
        Set<V> newNodesSet = new HashSet<>();
        Set<E> newEdgesSet = new HashSet<>();
        final int MAX_VERTEX_FAILURES = 1000;

        V initV = target.addVertex();
        newNodesSet.add(initV);
        
        int failuresCounter = 0;
        //grow network now
        while(targetEdges >= 0 ? targetEdges > newEdgesSet.size() : newNodesSet.size() <= targetNodes) {
			
        	if(failuresCounter >= MAX_VERTEX_FAILURES) {
				final String errMsg = failuresCounter +" successive failures more than maximum allowed number.";
				System.err.println(errMsg);
				throw new RuntimeException(errMsg);
			}

			V v, w, newV = null, newW = null; E e;
        	float tributaries = rng.nextFloat();
        	if(tributaries <= alpha) {
        		//stop adding nodes if you will exceed the target
        		if(targetEdges < 0 && newNodesSet.size() == targetNodes)
        			break;
        		v = newV = target.addVertex();
        		w = picAVertix(target, newNodesSet, newEdgesSet, true, deltaIn);
        	} else if (tributaries <= alphaPlusBeta) {
        		v = picAVertix(target, newNodesSet, newEdgesSet, false, deltaOut);
        		w = picAVertix(target, newNodesSet, newEdgesSet, true, deltaIn);
        	}else {//gamma
        		//stop adding nodes if you will exceed the target
        		if(targetEdges < 0 && newNodesSet.size() == targetNodes)
        			break;
        		v = picAVertix(target, newNodesSet, newEdgesSet, false, deltaOut);
        		w = newW = target.addVertex();
        	}
			
        	if(v== null || w == null) {
        		if(v == null) {
        			target.removeVertex(newW);
        		}
        		if(w == null) {
        			target.removeVertex(newV);
        		}
    			failuresCounter++;
    			System.err.println("WARNING: failed to pick a suitable node: Trial # "+failuresCounter);
    			continue;
        	}else {
        		failuresCounter = 0;
        	}

        	final GraphType graphType = target.getType();
        	//check for self loops
        	if( ! graphType.isAllowingSelfLoops() && v == w) {
        		failuresCounter++;
        		continue;
        	}
        	//check for multiple parallel targetEdges
			if ( ! graphType.isAllowingMultipleEdges() && target.containsEdge(v, w)) {
        		failuresCounter++;
        		continue;
            }
			
        	e = target.addEdge(v, w);
        	if(e == null) {
        		//some other unknown internal error
        		throw new RuntimeException("unexpected failure");
        	}

        	newNodesSet.add(v);
        	newNodesSet.add(w);
        	newEdgesSet.add(e);
        }
    }
    
    /**
     * @param target TODO
     * @param allNewNodes TODO
     * @param allNewEdgesSet TODO
     * @param directionIn in or out
     * @param bias deltaIn or deltaOut according to the call
     * @return the selected node.
     */
    public V picAVertix(Graph<V, E> target, Set<V> allNewNodes, Set<E> allNewEdgesSet, boolean directionIn, float bias){
    	final int allNewNodesSize = allNewNodes.size();
    	if(allNewNodesSize == 0) {
    		return null;
    	} else if (allNewNodesSize == 1) {
			return allNewNodes.iterator().next();
		}

    	float indicatorAccumulator = 0;
    	V ret;
		float denominator = allNewEdgesSet.size() + allNewNodesSize * bias;
    	
    	float r = rng.nextFloat();
    	//multiply r by denominator instead of dividing all individual values by it.
    	r *= denominator;
    	Iterator<V> verticesIterator = allNewNodes.iterator();
    	do {
    		ret = verticesIterator.next();
    		if (directionIn) {
				indicatorAccumulator += (target.inDegreeOf(ret) + bias);
			}else {
				indicatorAccumulator += (target.outDegreeOf(ret) + bias);
			}
		} while (verticesIterator.hasNext() && indicatorAccumulator < r);

		return ret;
    }
    
    public static void main(String[] args) {
    	Graph<PointND, DefaultEdge> testGraph = new DefaultDirectedGraph<PointND, DefaultEdge>(new PointNDSupplier(3, Constants.CASTOR),SupplierUtil.createDefaultEdgeSupplier(), false);
		BollobasGraphGenerator<PointND, DefaultEdge> testGraphGenerator = new BollobasGraphGenerator<PointND, DefaultEdge>(0.111f, 0.111f, 0, 1, 100, 0, 0);
		testGraphGenerator.generateGraph(testGraph);
		System.out.println(testGraph);
		GraphsIO.export(testGraph, System.out);
		System.out.println("==============");
		
		Utils.cacheVerticesDegrees(testGraph);
		Set<PointND> vertexSet = testGraph.vertexSet();
		vertexSet.stream().sorted((v1, v2) -> v1.getInDegree() - v2.getInDegree()).forEachOrdered(v -> System.out.println("" + v.getId()+"\t"+v.getInDegree()));
		System.out.println("==============");
		vertexSet.stream().sorted((v1, v2) -> v1.getOutDegree() - v2.getOutDegree()).forEachOrdered(v -> System.out.println("" + v.getId()+"\t"+v.getOutDegree()));
		System.out.println("==============");
		//distribution of outdegree where indegree == 0
		vertexSet.stream().filter(v -> v.getInDegree()==0).sorted((v1, v2) -> v1.getOutDegree() - v2.getOutDegree()).forEachOrdered(v -> System.out.println("" + v.getId()+"\t"+v.getOutDegree()));
		System.out.println("==============");
		
	}

}
