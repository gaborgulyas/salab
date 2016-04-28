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

import deanon.structures.Matches;
import deanon.structures.PerturbData;
import deanon.structures.PropagationData;
import mygraph.*;

/*
* Originally published in:
*   S. Benedek, G. G. Gulyas, and S. Imre. Analysis of grasshopper, a novel social network de-anonymization
*       algorithm. Periodica Polytechnica Electrical Engineering and Computer Science, 58(4):161–173, 12 2014.
*
* Implementation: Gabor Gulyas
* */

public class libGrh
{
	public static double Theta = 1.0;
	public static int maxPropagationSteps = 40;
	public static int maxPropagationTime = 1200; // == 20 mins
	
	// Propagate step
	public static void propagateStep(MyGraph g_src, MyGraph g_tar, PropagationData propData)
	{
		//
		// Init
		//
		boolean convergence = false;

		// Debug stuff
		long start_time = System.nanoTime();
		double difference = 0;
		
		//
		// Set weights
		//
		// Init
		Map<Integer, Double> src_ws = new HashMap<Integer, Double>();
		Set<Integer> src_vs = g_src.vertexSet();
		for(Integer v : src_vs)
			src_ws.put(v, 1.0);
		Map<Integer, Double> tar_ws = new HashMap<Integer, Double>();
		Set<Integer> tar_vs = g_tar.vertexSet();
		for(Integer v : tar_vs)
			tar_ws.put(v, 1.0);

		// Weighting
		for(Integer v : src_vs)
		{
			if(propData.matches.isMapped(v))
			{
				for(Integer nbr : g_src.neighborsOf(v))
				{
					Integer tar_v = propData.matches.get(v);
					if(propData.matches.isMapped(nbr) && g_tar.neighborsOf(tar_v).contains(propData.matches.get(nbr)))
					{
						src_ws.put(v, src_ws.get(v)+1);
						tar_ws.put(tar_v, tar_ws.get(tar_v)+1);
					}
				}
			}
		}

		// Normalize
		for(Integer v : src_vs)
		{
			if(propData.matches.isMapped(v))
			{
				Integer tar_v = propData.matches.get(v);
				
				double d1 = g_src.degreeOf(v);
				double d2 = g_tar.degreeOf(tar_v);
				double w1 = src_ws.get(v) / Math.sqrt(d1 * d2);
				double w2 = tar_ws.get(tar_v) / Math.sqrt(d1 * d2);
				
				src_ws.put(v, w1);
				tar_ws.put(tar_v, w2);
			}
		}
		
		long weights_time = 0;
		if(Deanon.DEBUG)
		{
			weights_time = System.nanoTime();
			difference = (weights_time - start_time)/1e6;
			System.out.println("\t\t! Weights: "+difference+" ms");
		}
		
		//
		// Propagation
		//
		Matches matches_backup = new Matches();
		matches_backup.forward = new Hashtable<Integer, Integer>(propData.matches.forward);
		matches_backup.reverse = new Hashtable<Integer, Integer>(propData.matches.reverse);

		// Iterate source nodes
		for(Integer source_candidate : src_vs)
		{
			double max_score;
			
			// Get target candidates
			Integer target_candidate = bestMatch(g_src, g_tar, src_ws, tar_ws, source_candidate, propData, false);
			
			// Reverse target candidates
			if(target_candidate != null)
			{
				Integer reverse_candidate = bestMatch(g_src, g_tar, src_ws, tar_ws, target_candidate, propData, true);
				
				// Do we have a match?
				if(source_candidate.equals(reverse_candidate))
				{
					if(propData.matches.isMapped(source_candidate) && propData.matches.get(source_candidate).equals(target_candidate))
						continue;

					matches_backup.add(source_candidate, target_candidate);
					convergence = true;
				}
			}
		}
		
		if(Deanon.DEBUG)
		{
			System.out.println("\t\t! PropStep: "+difference+" ms");
			long propstep_time = System.nanoTime();
			difference = (propstep_time - weights_time)/1e6;
		}
		propData.matches.forward = new Hashtable<Integer, Integer>(matches_backup.forward);
		propData.matches.reverse = new Hashtable<Integer, Integer>(matches_backup.reverse);
		
		// Update convergence data
		propData.updateConvergence(convergence);
	}
	
	// Match scores
	public static Integer bestMatch(MyGraph g_src, MyGraph g_tar, Map<Integer, Double> src_ws, Map<Integer, Double> tar_ws, Integer candidate, PropagationData propData, boolean reverse)
	{
		Integer best_match = null;
		Map<Integer, Double> scores = new HashMap<Integer, Double>();

		if(!reverse)
		{
			// Scoring -> pretty much optimized compared to the code of Benedek
			for(Integer nbr : g_src.neighborsOf(candidate))
			{
				Integer v = null;
				if(propData.matches.isMapped(nbr))
				{
					v = propData.matches.get(nbr);
					for(Integer nbr2 : g_tar.neighborsOf(v))
					{
						if(!scores.containsKey(nbr2))
							scores.put(nbr2, 0.0);
						scores.put(nbr2, scores.get(nbr2) + tar_ws.get(v));
					}
				}
			}
			
			if(scores.size() == 0)
				return null;
			
			// Check eccentricity
			Double max = 0.0, max2 = 0.0;
			Integer max_match = null;
			Integer num = 0;
			Double sum = 0.0, sq_sum = 0.0, avg, std = 0.0;
			for(Integer nbr2 : scores.keySet())
			{
				Double score = scores.get(nbr2);
				if(score >= max)
				{
					max2 = max;
					max = score;
					max_match = nbr2;
				}
				num += 1;
				sum += score;
				sq_sum += score*score;
			}
			
			if(max_match != null)
			{
				avg = sum/num;
				std = Math.sqrt(sq_sum/num - (avg*avg));
				if(std > 0 && (max-max2)/std >= Theta)
					best_match = max_match;
			}
		}
		else
		{
			// Scoring
			for(Integer nbr : g_tar.neighborsOf(candidate))
			{
				if(propData.matches.isReverseMapped(nbr))
				{
					Integer v = propData.matches.getReverse(nbr);
					for(Integer nbr2 : g_src.neighborsOf(v))
					{
						if(!scores.containsKey(nbr2))
							scores.put(nbr2, 0.0);
						scores.put(nbr2, scores.get(nbr2) + src_ws.get(v));
					}
				}
			}
			
			if(scores.size() == 0)
				return null;

			// Check eccentricity
			Double max = 0.0, max2 = 0.0;
			Integer max_match = null;
			Integer num = 0;
			Double sum = 0.0, sq_sum = 0.0, avg, std = 0.0;
			for(Integer nbr2 : scores.keySet())
			{
				Double score = scores.get(nbr2);
				if(score >= max)
				{
					max2 = max;
					max = score;
					max_match = nbr2;
				}
				num += 1;
				sum += score;
				sq_sum += score*score;
			}
			
			if(max_match != null)
			{
				avg = sum/num;
				std = Math.sqrt(sq_sum/num - (avg*avg));
				if(std > 0 && (max-max2)/std >= Theta)
					best_match = max_match;
			}
		}
		
		// Return scores :-)
		return best_match;
	}


}
