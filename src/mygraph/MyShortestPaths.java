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
package mygraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.map.MultiValueMap;

public class MyShortestPaths
{
	public static MultiValueMap getPathsFromV(MyBaseGraph g, Integer v)
	{
		Set<Integer> visitedBefore = new TreeSet<Integer>(), visitedNow = new TreeSet<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>(), toBeQueued = new LinkedList<Integer>();
		MultiValueMap paths = new MultiValueMap();
		HashMap<Integer, Integer> min_lengths = new HashMap<Integer, Integer>();
		
		queue.add(v);
		ArrayList<Integer> firstPath = new ArrayList<Integer>();
		firstPath.add(v);
		paths.put(v, firstPath);
		min_lengths.put(v, firstPath.size());
		while(queue.size() > 0 || toBeQueued.size() > 0)
		{
			Integer node = null;
			// Init a new round if queue is empty
			// (length +1 for all new paths)
			if(queue.size() == 0)
			{
				visitedBefore.addAll(visitedNow);
				visitedNow.clear();
				queue.addAll(toBeQueued);
				toBeQueued.clear();
				continue;
			}

			// Get a node from the Queue
			node = queue.poll();
			
			// Crawl node neighborhood
			if(!visitedBefore.contains(node) && !visitedNow.contains(node))
				visitedNow.add(node);
			List<Integer> nbrs = new ArrayList<Integer>(g.neighborsOf(node));
			nbrs.removeAll(visitedBefore);
			for(Integer nbr : nbrs)
			{
				if(!visitedNow.contains(nbr))
				{
					if(!queue.contains(nbr) && !toBeQueued.contains(nbr))
						toBeQueued.add(nbr);
				}

				// Register short paths to nbr (vita all short paths to node)
				ArrayList paths_to_node = new ArrayList(paths.getCollection(node));
				int min_length = Integer.MAX_VALUE;
				if(min_lengths.containsKey(nbr))
					min_length = min_lengths.get(nbr);
				for(int i = 0; i < paths_to_node.size(); i++)
				{
					ArrayList<Integer> path = (ArrayList<Integer>) paths_to_node.get(i);

					if(path.size() + 1 > min_length)
						continue;
					
					ArrayList<Integer> new_path = new ArrayList<Integer>();
					new_path.addAll(path);
					new_path.add(nbr);
					paths.put(nbr, new_path);
					if(!min_lengths.containsKey(nbr))
						min_lengths.put(nbr, new_path.size());
				}

			}
		}
		
		paths.remove(v);
		
		return paths;
	}
}
