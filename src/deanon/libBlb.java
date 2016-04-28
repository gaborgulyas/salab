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

import deanon.structures.PerturbData;
import deanon.structures.PropagationData;
import mygraph.MyEdge;
import mygraph.MyGraph;
import mygraph.MyMath;

import java.util.*;

/*
* Published in:
*   B. Simon. Analysis and development of structural de-anonymization algorithms (in
*		Hungarian). Master’s thesis, Budapest University of Technology and Economics,
*		Budapest, Hungary, 2015.
*
* Implementation: Gabor Gulyas
* */

public class libBlb
{
	public static double Theta = 0.01;
	public static double Delta = 0.5;

	// Propagate step
	public static void propagateStep(MyGraph g_src, MyGraph g_tar, PropagationData propData)
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
			{
				continue;
			}
			
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
			{
				continue;
			}

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
		
		Double max_1 = Collections.max(my_items);
		my_items.remove(max_1);
		Double max_2 = Collections.max(my_items);
		
		return (max_1-max_2)/std;
	}
	
	// Match scores
	public static Map<Integer, Double> matchScores(MyGraph g_src, MyGraph g_tar, Integer candidate, PropagationData propData, boolean reverse)
	{
		Map<Integer, Double> scores = new HashMap<Integer, Double>();
		
		if(!reverse)
		{
			// Get scores for mapped neighbors of candidate
			for(Integer nbr : g_src.neighborsOf(candidate))
			{
				if(!propData.matches.isMapped(nbr))
					continue;
				if(!g_tar.vertexSet().contains(propData.matches.get(nbr)))
					System.out.println(propData.matches.get(nbr));
				for(Integer nbr2 : g_tar.neighborsOf(propData.matches.get(nbr)))
					if(!propData.matches.isReverseMapped(nbr2))
					{
						if(!scores.containsKey(nbr2))
							scores.put(nbr2, 0.0);
						scores.put(nbr2, scores.get(nbr2) + Math.pow(Math.min(((double)g_src.degreeOf(candidate)/(double)g_tar.degreeOf(nbr2)), ((double)g_tar.degreeOf(nbr2))/(double)g_src.degreeOf(candidate)), Delta));
					}
			}
		}
		else
		{
			// Get scores for mapped neighbors of candidate
			for(Integer nbr : g_tar.neighborsOf(candidate))
			{
				if(!propData.matches.isReverseMapped(nbr))
					continue;
				for(Integer nbr2 : g_src.neighborsOf(propData.matches.getReverse(nbr)))
					if(!propData.matches.isMapped(nbr2))
					{
						if(!scores.containsKey(nbr2))
							scores.put(nbr2, 0.0);
						scores.put(nbr2, scores.get(nbr2) + Math.pow(Math.min(((double)g_tar.degreeOf(candidate)/(double)g_src.degreeOf(nbr2)), ((double)g_src.degreeOf(nbr2))/(double)g_tar.degreeOf(candidate)), Delta));
					}
			}
		}

		// Return scores :-)
		return scores;
	}
}
