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

import analysis.libLTA;

import deanon.structures.GroundTruth;
import deanon.structures.tuples.DegIx;
import deanon.structures.tuples.DictTuple;

import mygraph.MyBaseGraph;
import mygraph.MyGraph;

public class Seeding
{
	public static long seedTimeout = 120*1000; // Seeding timeout in ms (2mins / Long.MAX_VALUE)
	
	// Seeding cliques of a given size from the top nodes
	public static List<Object> getCliquesFromTop(MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt, int seed_count, int clique_size, double top_percent)
	{
		// Init
		List<Object> cliques = new ArrayList<Object>();
		List<Integer> common_vs = new ArrayList(gt.common_vertices);
		long start_time = System.nanoTime() / 1000000;

		// Filter nodes having a low degree value
		if(top_percent < 1.0)
		{
			List<DegIx> degixs = new ArrayList<DegIx>();
			for(Integer v : common_vs)
				degixs.add(new DegIx(v, g_src.degreeOf(v)));
			Comparator<DegIx> comparator = new Comparator<DegIx>()
		    {
		        public int compare(DegIx tupleA, DegIx tupleB)
		        {
		            return tupleA.degree.compareTo(tupleB.degree);
		        }
		    };
			Collections.sort(degixs, comparator);
			ArrayList<Integer> vs = new ArrayList<Integer>();
			for(int i = degixs.size()-1; i>=0; i--)
			{
				DegIx dit = degixs.get(i);
				vs.add(dit.index);
				if(vs.size() >= (int)(common_vs.size()*top_percent))
					break;
			}
			common_vs.clear();
			common_vs.addAll(vs);
		}
		
		int progress = 0;
		System.out.print("\t    Cliques: [");
		
		// Iterating for cliques
		while(cliques.size() < seed_count && (System.nanoTime() / 1000000 - start_time < Seeding.seedTimeout))
		{
			// Get a random node first
			Integer v = common_vs.get(new Random().nextInt(common_vs.size()));
			List<Integer> clique = new ArrayList();
			clique.add(v);
			// Then its neighbors that are also in the common set
			List<Integer> cnbrs = new ArrayList();
			for(Integer vt : g_src.neighborsOf(v))
				if(common_vs.contains(vt))
					cnbrs.add(vt);
			// Now try to get a clique of the given size around it by adding nodes
			while(clique.size() < clique_size && cnbrs.size() > 0)
			{
				Integer vt = cnbrs.get(new Random().nextInt(cnbrs.size()));
				List<Integer> vt_nbrs = new ArrayList();
				for(Integer vt2 : g_src.neighborsOf(vt))
					if(common_vs.contains(vt2))
						vt_nbrs.add(vt2);
				clique.add(vt);
				cnbrs.retainAll(vt_nbrs);
			}
			if(clique.size() < clique_size)
				continue;
			cliques.add(clique);
			common_vs.removeAll(clique);
			
			int new_progress = Math.round((cliques.size()/(float)seed_count)*100);
			if(new_progress-progress > 0)
			{
				for(int i = 0; i < new_progress-progress; i++)
					System.out.print(".");
				progress = new_progress;
			}
		}
		
		System.out.print("]\n");
		
		return cliques;
	}
	
	// Seeding cliques of a given size
	public static List<Object> getCliques(MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt, int clique_size, int seed_count)
	{
		return getCliquesFromTop(g_src, g_tar, gt, clique_size, seed_count, 1.0);
	}
	
	// Seeding random nodes (of the highest node degrees)
	public static List<Integer> getNodes(MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt, int seed_count, double top_percent)
	{
		List<Integer> common_vs = new ArrayList();

		if(top_percent < 1.0)
		{
			List<DegIx> degixs = new ArrayList<DegIx>();
			for(Integer v : gt.common_vertices)
				degixs.add(new DegIx(v, g_src.degreeOf(v)));
			Comparator<DegIx> comparator = new Comparator<DegIx>()
		    {
		        public int compare(DegIx tupleA, DegIx tupleB)
		        {
		            return tupleA.degree.compareTo(tupleB.degree);
		        }
		    };
			Collections.sort(degixs, comparator);
			ArrayList<Integer> vs = new ArrayList<Integer>();
			for(int i = degixs.size()-1; i>=0; i--)
			{
				DegIx dit = degixs.get(i);
				vs.add(dit.index);
				if(vs.size() >= (int)(gt.common_vertices.size()*top_percent))
					break;
			}
			common_vs.addAll(vs);
		}
		else
			common_vs.addAll(gt.common_vertices);
		
		Collections.shuffle(common_vs);
		List<Integer> seeds = null;
		if(common_vs.size() < seed_count)
			seeds = new ArrayList<Integer>(common_vs);
		else
			seeds = common_vs.subList(0, seed_count);

		return seeds;
	}
	
	// Seeding with BFS (of the highest node degrees)
	public static List<Integer> getBFSNodes(MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt, int seed_count, int bfs_size, double top_percent)
	{
		// Init
		List<Integer> seeds = new ArrayList();
		List<Integer> common_vs = new ArrayList();
		long start_time = System.nanoTime() / 1000000;

		// Filter low degree nodes 
		if(top_percent < 1.0)
		{
			List<DegIx> degixs = new ArrayList<DegIx>();
			for(Integer v : gt.common_vertices)
				degixs.add(new DegIx(v, g_src.degreeOf(v)));
			Comparator<DegIx> comparator = new Comparator<DegIx>()
		    {
		        public int compare(DegIx tupleA, DegIx tupleB)
		        {
		            return tupleA.degree.compareTo(tupleB.degree);
		        }
		    };
			Collections.sort(degixs, comparator);
			ArrayList<Integer> vs = new ArrayList<Integer>();
			for(int i = degixs.size()-1; i>=0; i--)
			{
				DegIx dit = degixs.get(i);
				vs.add(dit.index);
				if(vs.size() >= (int)(gt.common_vertices.size()*top_percent))
					break;
			}
			common_vs.addAll(vs);
		}
		else
			common_vs.addAll(gt.common_vertices);
		
		// Do the BFS
		int progress = 0;
		System.out.print("\t    BFS-patterns: [");
		
		// Iterating for patterns
		while(seeds.size() < seed_count &&  (System.nanoTime() / 1000000 - start_time < Seeding.seedTimeout))
		{
			Queue<Integer> queue = new LinkedList();
			Integer v = common_vs.get(new Random().nextInt(common_vs.size()));
			seeds.add(v);
			common_vs.remove(v);
			queue.offer(v);
			
			// Extract nodes with BFS
			int bfs_size_tmp = bfs_size;
			while(queue.size() > 0 && bfs_size_tmp > 0 && seeds.size() < seed_count)
			{
				Integer v1 = queue.poll();
				List<Integer> nbrs = new ArrayList(g_src.neighborsOf(v1));
				Collections.shuffle(nbrs);
				for(Integer v2 : nbrs)
				{
					if(common_vs.contains(v2) && !seeds.contains(v2))
					{
						seeds.add(v2);
						common_vs.remove(v2);
						queue.offer(v2);
						bfs_size_tmp--;
					}
					
					if(bfs_size_tmp < 0 || seeds.size() >= seed_count)
					{
						queue.clear();
						break;
					}
				}
			}
			
			int new_progress = Math.round((seeds.size()/(float)seed_count)*100);
			if(new_progress-progress > 0)
			{
				for(int i = 0; i < new_progress-progress; i++)
					System.out.print(".");
				progress = new_progress;
			}
		}
		
		System.out.print("]\n");

		return seeds;
	}
	
	// Seeding top nodes
	public static List<Integer> getTopNodes(MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt, int seed_count)
	{
		List<DegIx> degixs = new ArrayList<DegIx>();
		for(Integer v : gt.common_vertices)
			degixs.add(new DegIx(v, g_src.degreeOf(v)));
		
		Comparator<DegIx> comparator = new Comparator<DegIx>()
	    {
	        public int compare(DegIx tupleA, DegIx tupleB)
	        {
	            return tupleA.degree.compareTo(tupleB.degree);
	        }

	    };
		
		Collections.sort(degixs, comparator);
		
		List<Integer> seeds = new ArrayList();
		for(int i = degixs.size()-1; i>=0; i--)
		{
			DegIx dit = degixs.get(i);
			seeds.add(dit.index);
			if(seeds.size() >= seed_count)
				break;
		}

		return seeds;
	}
	
	// Seeding nodes selected by dictionary
	public static List<Integer> getDictNodes(MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt, int seed_count, HashMap<Integer, Double> dict, boolean from_top, double skip)
	{
		List<DictTuple> tuples = new ArrayList<DictTuple>();
		for(Integer v : dict.keySet())
		{
			if(!gt.common_vertices.contains(v))
				continue;
			Double val = dict.get(v);
			tuples.add(new DictTuple(v, val));
		}
		
		Comparator<DictTuple> comparator = new Comparator<DictTuple>()
	    {
	        public int compare(DictTuple tupleA, DictTuple tupleB)
	        {
	            return tupleA.value.compareTo(tupleB.value);
	        }
	    };
		
		Collections.sort(tuples, comparator);
		
		if(skip > 0.0)
		{
			int skip_n = (int)((double)tuples.size() * skip);
			if(from_top)
				tuples = tuples.subList(0, tuples.size()-skip_n+1);
			else
				tuples = tuples.subList(skip_n, tuples.size());
		}
		
		List<Integer> seeds = new ArrayList();
		if(from_top)
		{
			for(int i = tuples.size()-1; i >= 0; i--)
			{
				DictTuple tuple = tuples.get(i);
				seeds.add(tuple.index);
				if(seeds.size() >= seed_count)
					break;
			}
		}
		else
		{
			for(int i = 0; i < tuples.size(); i++)
			{
				DictTuple tuple = tuples.get(i);
				seeds.add(tuple.index);
				if(seeds.size() >= seed_count)
					break;
			}
		}

		return seeds;
	}
	
	public static List<Integer> getDictNodes(MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt, int seed_count, HashMap<Integer, Double> dict, boolean from_top)
	{
		return getDictNodes(g_src, g_tar, gt, seed_count, dict, from_top, 0.0);
	}
	
	public static List<Integer> getDictNodes(MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt, int seed_count, HashMap<Integer, Double> dict)
	{
		return getDictNodes(g_src, g_tar, gt, seed_count, dict, false, 0.0);
	}
}
