/*
* Structural Anonymity Lab
* ========================
*
* Copyright (c) 2016 Gabor Gulyas
* Licenced under GNU GPLv3 (see licence.txt)
*
* URL:      https://github.com/gaborgulyas/salab
*
* */
package deanon;

import java.util.*;

import org.jgrapht.traverse.*;

import deanon.structures.PerturbData;
import deanon.structures.PropagationData;

import mygraph.*;

/*
* Sample based perturbation used in multiple publications.
*
* Implementation: Gabor Gulyas
* */

public class libSPert
{
	public static PerturbData perturbate(MyGraph g, float s_v, float s_e)
	{
		PerturbData rv = new PerturbData();
		rv.g_src = new MyGraph();
		rv.g_tar = new MyGraph();
		
		// Vertex sets
		List<Integer> vertices = new ArrayList(g.vertexSet());
		int common_size = Math.round(vertices.size()*s_v);
		Collections.shuffle(vertices);
		List<Integer> vs1 = vertices.subList(0, common_size);
		for(Integer v : vs1)
			rv.g_src.addVertex(v);
		Collections.shuffle(vertices);
		List<Integer> vs2 = vertices.subList(0, common_size);
		for(Integer v : vs2)
			rv.g_tar.addVertex(v);

		// Select and project edges
		List<MyEdge> edges = new ArrayList(g.edgeSet());
		common_size = Math.round(edges.size()*s_e);
		Collections.shuffle(edges);
		List<MyEdge> es1 = edges.subList(0, common_size);
		for(MyEdge e : es1)
			if(rv.g_src.containsVertex(e.source()) && rv.g_src.containsVertex(e.target()))
				rv.g_src.addEdge(e.source(), e.target());
		Collections.shuffle(edges);
		List<MyEdge> es2 = edges.subList(0, common_size);
		for(MyEdge e : es2)
			if(rv.g_tar.containsVertex(e.source()) && rv.g_tar.containsVertex(e.target()))
				rv.g_tar.addEdge(e.source(), e.target());

		// Selecting largest connected component and leaving that only
		rv.g_src.retainLargestConnectedComponent();
		rv.g_tar.retainLargestConnectedComponent();
		
		return rv;
	}
}
