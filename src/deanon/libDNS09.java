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
* Originally published in:
*   Arvind Narayanan and Vitaly Shmatikov. De-anonymizing social networks. In Security and
*       Privacy, 2009 30th IEEE Symposium on, pages 173–187, 2009.
*
* Implementation: Gabor Gulyas
* */

public class libDNS09
{
	public static double Theta = 1.0;
	
	public static PerturbData perturbate(MyDirectedGraph g, float alpha_v, float alpha_e)
	{
		PerturbData rv = new PerturbData();
		rv.g_src = new MyDirectedGraph();
		rv.g_tar = new MyDirectedGraph();
		
		// Vertex sets
		List<Integer> vertices = new ArrayList(g.vertexSet());
		Collections.shuffle(vertices);
		int common_size = Math.round(vertices.size()*alpha_v);
		int disjoint_size = (vertices.size()-common_size)/2;
		List<Integer> vs2 = vertices.subList(0, common_size);
		List<Integer> vs1 = vertices.subList(common_size+1, common_size+1+disjoint_size);
		List<Integer> vs3 = vertices.subList(common_size+1+disjoint_size, Math.min(vertices.size(), common_size+1+2*disjoint_size));
		
		// Init graphs with vertexes
		for(Integer v : vs2)
		{
			rv.g_src.addVertex(v);
			rv.g_tar.addVertex(v);
		}
		for(Integer v : vs1)
			rv.g_src.addVertex(v);
		for(Integer v : vs3)
			rv.g_tar.addVertex(v);

		// Select and project edges
		List<MyEdge> edges = new ArrayList(g.edgeSet());
		float beta = (1-alpha_e) / (1+alpha_e);
		int rem_edges = Math.round((1-beta) * edges.size());

		Collections.shuffle(edges);
		List<MyEdge> es_src = edges.subList(0, rem_edges);
		for(MyEdge e : es_src)
			if(rv.g_src.containsVertex(e.source()) && rv.g_src.containsVertex(e.target()))
				rv.g_src.addEdge(e.source(), e.target());
		
		Collections.shuffle(edges);
		List<MyEdge> es_tar = edges.subList(0, rem_edges);
		for(MyEdge e : es_tar)
			if(rv.g_tar.containsVertex(e.source()) && rv.g_tar.containsVertex(e.target()))
				rv.g_tar.addEdge(e.source(), e.target());
		
		// Selecting largest connected component and leaving that only
		rv.g_src.retainLargestConnectedComponent();
		rv.g_tar.retainLargestConnectedComponent();

		return rv;
	}
	
	// Propagate step
	public static void propagateStep(MyDirectedGraph g_src, MyDirectedGraph g_tar, PropagationData propData)
	{
		// Init
		boolean convergence = false;
		
		// Iterate source nodes
		Set<Integer> src_vs = g_src.vertexSet();
		for(Integer source_candidate : src_vs)
		{
			double max_score;
			
			// Get target candidates
			Map<Integer, Double> target_candidates = matchScores(g_src, g_tar, source_candidate, propData, false);
			
			// Forward eccentricity check & target selection
			Integer target_candidate = null;
			if(target_candidates.size() > 0 && (target_candidates.size() == 1 || eccentricity(target_candidates.values()) >= Theta))
			{
				max_score = Collections.max(target_candidates.values());
				List<Integer> top_target_candidates = new ArrayList();
				for(Integer tc : target_candidates.keySet())
					if(target_candidates.get(tc).equals(max_score))
						top_target_candidates.add(tc);
				if(top_target_candidates.size() > 0)
					target_candidate = top_target_candidates.get(new Random().nextInt(top_target_candidates.size()));
			}
			
			// If we have no target candidates, carry on
			if(target_candidate == null)
				continue;
			
			// Reverse target candidates
			Map<Integer, Double> reverse_target_candidates = matchScores(g_src, g_tar, target_candidate, propData, true);

			// Reverse eccentricity check & target selection
			Integer reverse_target_candidate = null;
			if(reverse_target_candidates.size() > 0 && (reverse_target_candidates.size() == 1 || eccentricity(reverse_target_candidates.values()) >= Theta))
			{
				max_score = Collections.max(reverse_target_candidates.values());
				List<Integer> reverse_top_candidates = new ArrayList();
				for(Integer rtc : reverse_target_candidates.keySet())
					if(reverse_target_candidates.get(rtc).equals(max_score))
						reverse_top_candidates.add(rtc);
				if(reverse_top_candidates.size() > 0)
					reverse_target_candidate = reverse_top_candidates.get(new Random().nextInt(reverse_top_candidates.size()));
			}

			// If we have no reverse target candidates, carry on
			if(reverse_target_candidate == null)
				continue;

			// Do we have a match?
			if(source_candidate.equals(reverse_target_candidate))
			{
				propData.matches.add(source_candidate, target_candidate);
				convergence = true;
			}
		}
		
		// Update convergence data
		propData.updateConvergence(convergence);
	}
	
	// Score eccentricity
	public static double eccentricity(Collection<Double> collection)
	{
		if(collection.size() < 2)
			return 0.0;

		List<Double> my_items = new ArrayList(collection);
		
		double std = MyMath.standardDeviation(my_items);
		if(std == 0.0)
			return 0.0;
		
		double max_1 = Collections.max(my_items);
		my_items.remove(max_1);
		double max_2 = Collections.max(my_items);
		
		return (max_1-max_2)/std;
	}
	
	// Match scores
	public static Map<Integer, Double> matchScores(MyDirectedGraph g_src, MyDirectedGraph g_tar, Integer candidate, PropagationData propData, boolean reverse)
	{
		Map<Integer, Double> scores = new HashMap<Integer, Double>();
		
		if(!reverse)
		{
			// Get scores of mapped in-neighbors of candidate
			for(Integer nbr : g_src.inNeighborsOf(candidate))
			{
				if(!propData.matches.isMapped(nbr))
					continue;
				for(Integer nbr2 : g_tar.outNeighborsOf(propData.matches.get(nbr)))
					if(!propData.matches.isReverseMapped(nbr2))
					{
						if(!scores.containsKey(nbr2))
							scores.put(nbr2, 0.0);
						scores.put(nbr2, scores.get(nbr2) + (1.0/Math.sqrt(g_tar.inDegreeOf(nbr2))));
					}
			}
			
			// Get scores of mapped out-neighbors of candidate
			for(Integer nbr : g_src.outNeighborsOf(candidate))
			{
				if(!propData.matches.isMapped(nbr))
					continue;
				for(Integer nbr2 : g_tar.inNeighborsOf(propData.matches.get(nbr)))
					if(!propData.matches.isReverseMapped(nbr2))
					{
						if(!scores.containsKey(nbr2))
							scores.put(nbr2, 0.0);
						scores.put(nbr2, scores.get(nbr2) + (1.0/Math.sqrt(g_tar.outDegreeOf(nbr2))));
					}
			}
		}
		else
		{
			// Get scores of mapped in-neighbors of candidate
			for(Integer nbr : g_tar.inNeighborsOf(candidate))
			{
				if(!propData.matches.isReverseMapped(nbr))
					continue;
				for(Integer nbr2 : g_src.outNeighborsOf(propData.matches.getReverse(nbr)))
					if(!propData.matches.isMapped(nbr2))
					{
						if(!scores.containsKey(nbr2))
							scores.put(nbr2, 0.0);
						scores.put(nbr2, scores.get(nbr2) + (1.0/Math.sqrt(g_src.inDegreeOf(nbr2))));
					}
			}
			
			// Get scores of mapped out-neighbors of candidate
			for(Integer nbr : g_tar.outNeighborsOf(candidate))
			{
				if(!propData.matches.isReverseMapped(nbr))
					continue;
				for(Integer nbr2 : g_src.inNeighborsOf(propData.matches.getReverse(nbr)))
					if(!propData.matches.isMapped(nbr2))
					{
						if(!scores.containsKey(nbr2))
							scores.put(nbr2, 0.0);
						scores.put(nbr2, scores.get(nbr2) + (1.0/Math.sqrt(g_src.outDegreeOf(nbr2))));
					}
			}
		}
		
		// Return scores :-)
		return scores;
	}
}
