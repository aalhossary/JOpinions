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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.generate.GraphGenerator;


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
    /**probability that the new edge is from an existing vertex v to a new vertex w, 
     * where v is chosen according to d_out + delta_out.*/
    private final float gamma;
    final float alphaPlusBeta;
    
    /**Bias used for Alpha and Beta*/
    private final float dIn;
    /**Bias used for Beta and Gamma*/
    private final float dOut;
    /**Target total number of nodes*/
    private final int n;

    public BollobasGraphGenerator(float alpha, float gamma, float dIn, float dOut, int n){
    	this(alpha, gamma, dIn, dOut, n, new Random());
    }
    
    public BollobasGraphGenerator(float alpha, float gamma, float dIn, float dOut, int n, long seed){
    	this(alpha, gamma, dIn, dOut, n, new Random(seed));
    }
    
    public BollobasGraphGenerator(float alpha, float gamma, float dIn, float dOut, int n, Random rng){
    	this.alpha = alpha;
    	this.gamma = gamma;
    	this.alphaPlusBeta = 1.0f - gamma;
    	this.dIn = dIn;
    	this.dOut = dOut;
        this.n = n;
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
    public void generateGraph(Graph<V, E> target, Map<String, V> resultMap)
    {
        List<V> nodesInDegrees  = new ArrayList<>();
        List<V> nodesOutDegrees = new ArrayList<>();
        
        Set<V> oldNodes = new HashSet<>(target.vertexSet());
        Set<V> newNodes = new HashSet<>();
        List<V> nodes = new ArrayList<>();

//        /*
//         * Create complete graph with m0 nodes
//         */
//        Set<V> oldNodes = new HashSet<>(target.vertexSet());
//        Set<V> newNodes = new HashSet<>();
//        new CompleteGraphGenerator<V, E>(m0).generateGraph(target, resultMap);
//        target.vertexSet().stream().filter(v -> !oldNodes.contains(v)).forEach(newNodes::add);
//
//        List<V> nodes = new ArrayList<>(n * m);

        //let's assume we initially have two nodes with one edge
        //TODO discuss that with ZR and change it later
        V newV1 = target.addVertex();
        V newV2 = target.addVertex();
        target.addEdge(newV1, newV2);
        newNodes.add(newV1);
        newNodes.add(newV2);
        nodes.addAll(newNodes);
        
        //now add them proportionately
        for (V node : newNodes) {
        	//TODO Do we assume that all initial nodes are connected? Change this if we don't. 
        	for (int i = 0; i < target.inDegreeOf(node); i++) {
				nodesInDegrees.add(node);
			}
        	for (int i = 0; i < target.outDegreeOf(node); i++) {
				nodesOutDegrees.add(node);
			}
		}
        
        //grow network now
        while(newNodes.size() < n) {
        	V v, w; E e;
        	float direction = rng.nextFloat();
        	if(direction <= alpha) {
        		v = target.addVertex();
        		w = picAVertix(dIn, nodesInDegrees);
        	} else if (direction <= alphaPlusBeta) {
        		v = picAVertix(dOut, nodesOutDegrees);
        		w = picAVertix(dIn, nodesInDegrees);
        	}else {//gamma
        		v = picAVertix(dOut, nodesOutDegrees);
        		w = target.addVertex();
        	}
			
        	if(v == null || w == null) {
        		continue;
        		//TODO add counter, warning, and break
        	}
        	final GraphType graphType = target.getType();
        	//check for self loops
        	if( ! graphType.isAllowingSelfLoops() && v == w) {
        		continue;
        	}
        	//check for multiple parallel edges
			if ( ! graphType.isAllowingMultipleEdges() && target.containsEdge(v, w)) {
            	continue;
            }
        	
        	e = target.addEdge(v, w);
        	if(e == null) {
        		//some other unknown internal error
        		throw new RuntimeException("unexpected failure;");
        		//TODO ?????
        	}
        	nodesOutDegrees.add(v);
        	nodesInDegrees.add(w);
        	newNodes.add(v);
        	newNodes.add(w);
        }
    }
    
    /**
     * @param bias deltaIn or deltaOut according to the call
     * @param allNodes bag containing all vertices, where each node exists a 
     * number of times proportional to their inDegree / outDegree. It represents [ t + delta_out n(t) ] in the paper.
     * @return the selected node.
     */
    public V picAVertix(float bias, List<V> allNodes){
    	final int size = allNodes.size();
    	if(size == 0) {
    		return null;
    	}
    	//index of a node to pickup
		float r = rng.nextFloat() * size - bias;
		//add a random offset to allow all nodes (not only first ones) to be picked up
    	final int randomOffset = rng.nextInt(size);
		r = (r + randomOffset) % size;
		return allNodes.get((int) r);
    }

}
