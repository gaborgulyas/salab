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

import java.awt.Dimension;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.jgrapht.traverse.*;

import deanon.structures.PerturbData;
import deanon.structures.PropagationData;

import mygraph.*;

/*
* Originally published in:
*   Wei Peng, Feng Li, Xukai Zou, and Jie Wu. Seed and grow: An attack against anonymized social
*       networks. In Sensor, Mesh and Ad Hoc Communications and Networks (SECON), 2012 9th Annual
*       IEEE Communications Society Conference on, pages 587–595, 2012
*
* Implementation: Gabor Gulyas
* */

public class libSNG
{	
	// Propagate step
	public static void propagateStep(MyGraph g_src, MyGraph g_tar, PropagationData propData)
	{
		// Candidate nodes for identification in g_src
		ArrayList<Integer> C_src = new ArrayList<Integer>();
		for(Object o : g_src.vertexSet())
		{
			Integer v = (Integer) o;
			if(propData.matches.isMapped(v))
				for(Integer v2 : g_src.neighborsOf(v))
					if(!C_src.contains(v2))
						C_src.add(v2);
		}
			
		// Candidate nodes for identification in g_tar
		ArrayList<Integer> C_tar = new ArrayList<Integer>();
		for(Object o : g_tar.vertexSet())
		{
			Integer v = (Integer) o;
			if(propData.matches.isReverseMapped(v))
				for(Integer v2 : g_tar.neighborsOf(v))
					if(!C_tar.contains(v2))
						C_tar.add(v2);
		}
		
		// Check if continuing propagation is still a valid task
		updateConvergence(propData, C_src, C_tar);
		if(!propData.hasConvergence())
			return;
		
		// Calculate dissimilarity matrices
		getDissims(g_src, g_tar, propData, C_src, C_tar);
		
		// Calculating minimum values in dissim matrices for each row and col
		double min_dissim_src = 1.0;
		double[] min_dissim_src_by_row = new double[C_tar.size()];
		double[] min_dissim_tar_by_row = new double[C_tar.size()];
		for(int row = 0; row<C_tar.size(); row++)
		{
			min_dissim_src_by_row[row] = 1.0;
			min_dissim_tar_by_row[row] = 1.0;
			
			for(int col = 0; col<C_src.size(); col++)
			{
				if(min_dissim_src_by_row[row] > propData.dissims_src[row][col])
					min_dissim_src_by_row[row] = propData.dissims_src[row][col];
				if(min_dissim_tar_by_row[row] > propData.dissims_src[row][col])	// TODO: _src -> _tar
					min_dissim_tar_by_row[row] = propData.dissims_src[row][col];// TODO: _src -> _tar
			}
			if(min_dissim_src > min_dissim_src_by_row[row])
				min_dissim_src = min_dissim_src_by_row[row];
		}
		if(min_dissim_src == 1.0)
		{
			propData.updateConvergence(false);
			return;
		}
		
		double min_dissim_tar = 1.0;
		double[] min_dissim_src_by_col = new double[C_src.size()];
		double[] min_dissim_tar_by_col = new double[C_src.size()];
		for(int col = 0; col<C_src.size(); col++)
		{
			min_dissim_src_by_col[col] = 1.0;
			min_dissim_tar_by_col[col] = 1.0;
			
			for(int row = 0; row<C_tar.size(); row++)
			{
				if(min_dissim_src_by_col[col] > propData.dissims_src[row][col])
					min_dissim_src_by_col[col] = propData.dissims_src[row][col];
				if(min_dissim_tar_by_col[col] > propData.dissims_src[row][col])	// TODO: _src -> _tar
					min_dissim_tar_by_col[col] = propData.dissims_src[row][col];// TODO: _src -> _tar
			}
			if(min_dissim_tar > min_dissim_tar_by_col[col])
				min_dissim_tar = min_dissim_tar_by_col[col];
		}
		if(min_dissim_tar == 1.0)
		{
			propData.updateConvergence(false);
			return;
		}
		
		// Find minimum positions according to previous minimum values
		List<MyDimension> minpos = new ArrayList<MyDimension>();
		for(int row = 0; row<C_tar.size(); row++)
			for(int col = 0; col<C_src.size(); col++)
				if(propData.dissims_src[row][col] == min_dissim_src_by_col[col] && propData.dissims_src[row][col] == min_dissim_src_by_row[row])
					if(propData.dissims_tar[row][col] == min_dissim_tar_by_col[col] && propData.dissims_tar[row][col] == min_dissim_tar_by_row[row])
						minpos.add(new MyDimension(row, col));

		// Collect collisions
		HashMap<Integer, HashSet> collisions_by_col = new HashMap<Integer, HashSet>();
		HashMap<Integer, HashSet> collisions_by_row = new HashMap<Integer, HashSet>();
		for(int i = 0; i<minpos.size(); i++)
		{
			MyDimension p = minpos.get(i);
			
			HashSet<MyDimension> hsc;
			if(collisions_by_col.containsKey(p.col))
				hsc = collisions_by_col.get(p.col);
			else
				hsc = new HashSet<MyDimension>();
			hsc.add(p);
			collisions_by_col.put(p.col, hsc);

			HashSet<MyDimension> hsr;
			if(collisions_by_col.containsKey(p.row))	// TODO: collisions_by_col -> collisions_by_row
				hsr = collisions_by_col.get(p.row);		// TODO: collisions_by_col -> collisions_by_row 
			else
				hsr = new HashSet<MyDimension>();
			hsr.add(p);
			collisions_by_row.put(p.row, hsr);
		}
		
		// Check eccentricity
		for(int i = 0; i<minpos.size(); i++)
		{
			MyDimension p = minpos.get(i);
			
			//
			// Check eccentricity for collisions by row
			//
			HashSet<Integer> collisions_cols = new HashSet<Integer>();
			for(Object o : collisions_by_row.get(p.row))
			{
				MyDimension md = (MyDimension)o;
				collisions_cols.add(md.col);
			}
			collisions_cols.add(p.col);
			
			HashMap<Integer, Double> src_ecc = new HashMap<Integer, Double>();
			HashMap<Integer, Double> tar_ecc = new HashMap<Integer, Double>();
			
			// Explore other items at p.row
			for(Integer col : collisions_cols)
			{
				double[] items;
				// Source dissim eccentricity measurement
				if(!src_ecc.containsKey(col))
				{
					double ecc = 0.0;
					items = new double[C_tar.size()];
					for(int row = 0; row<C_tar.size(); row++)
						items[row] = propData.dissims_src[row][col];
					ecc = eccentricity(propData.dissims_src[p.row][p.col], items);
					// Error? ecc = eccentricity(propData.dissims_src[p.row][col], items);
					src_ecc.put(col, ecc);
				}
				// Target dissim eccentricity measurement
				if(!tar_ecc.containsKey(col))
				{
					double ecc = 0.0;
					items = new double[C_tar.size()];
					for(int row = 0; row<C_tar.size(); row++)
						items[row] = propData.dissims_tar[row][col];
					ecc = eccentricity(propData.dissims_tar[p.row][p.col], items);
					tar_ecc.put(col, ecc);
				}
			}
			boolean p_outstanding = true;
			for(Integer col : collisions_cols)
				if(!col.equals(p.col) && (src_ecc.get(col)>=src_ecc.get(p.col) || tar_ecc.get(col)>=tar_ecc.get(p.col)))
				{
					p_outstanding = false;
					break;
				}
			if(!p_outstanding)
				continue;
			
			//
			// Check eccentricity for collisions by col
			//
			HashSet<Integer> collisions_rows = new HashSet<Integer>();
			for(Object o : collisions_by_col.get(p.col))
			{
				MyDimension md = (MyDimension)o;
				collisions_rows.add(md.row);
			}
			collisions_rows.add(p.row);
			
			src_ecc = new HashMap<Integer, Double>();
			tar_ecc = new HashMap<Integer, Double>();
			
			// Explore other items at p.col
			for(Integer row : collisions_rows)
			{
				double[] items;
				// Source dissim eccentricity measurement
				if(!src_ecc.containsKey(row))
				{
					double ecc = 0.0;
					items = new double[C_src.size()];
					for(int col = 0; col<C_src.size(); col++)
						items[col] = propData.dissims_src[row][col];
					// row instead of p.row
					ecc = eccentricity(propData.dissims_src[p.row][p.col], items);
					src_ecc.put(row, ecc);
				}
				// Target dissim eccentricity measurement
				if(!tar_ecc.containsKey(row))
				{
					double ecc = 0.0;
					items = new double[C_src.size()];
					for(int col = 0; col<C_src.size(); col++)
						items[col] = propData.dissims_tar[row][col];
					ecc = eccentricity(propData.dissims_tar[p.row][p.col], items);
					tar_ecc.put(row, ecc);
				}
			}
			p_outstanding = true;
			for(Integer row : collisions_rows)
				if(!row.equals(p.row) && (src_ecc.get(row)>=src_ecc.get(p.row) || tar_ecc.get(row)>=tar_ecc.get(p.row)))
				{
					p_outstanding = false;
					break;
				}
			if(!p_outstanding)
				continue;
			
			propData.matches.add(C_src.get(p.col), C_tar.get(p.row));
		}
	}
			
	// Update convergence
	public static void updateConvergence(PropagationData propData, ArrayList<Integer> C_src, ArrayList<Integer> C_tar)
	{
		if(C_src.size() == 0 || C_tar.size() == 0)
		{
			propData.updateConvergence(false);
			return;
		}
		
		String digests = MyMath.md5_digest(C_src.toString()) + " " + MyMath.md5_digest(C_tar.toString());
		if(propData.pastCandidates.contains(digests))
			propData.updateConvergence(false);
		else
			propData.pastCandidates.add(digests);
	}
	
	// Calculate dissims
	public static void getDissims(MyGraph g_src, MyGraph g_tar, PropagationData propData, ArrayList<Integer> C_src, ArrayList<Integer> C_tar)
	{
		// Init
		propData.dissims_src = new double[C_tar.size()][C_src.size()];
		propData.dissims_tar = new double[C_tar.size()][C_src.size()];
		
		// Calculate mapped neighbors (mapped to g_src)
		HashMap<Integer, HashSet> g_tar_mnbrs = new HashMap<Integer, HashSet>();
		for(Integer v_tar : C_tar)
			for(Integer nbr : g_tar.neighborsOf(v_tar))
				if(propData.matches.isReverseMapped(nbr))
				{
					HashSet<Integer> mnbr_set;
					if(!g_tar_mnbrs.containsKey(v_tar))
						mnbr_set = new HashSet<Integer>();
					else
						mnbr_set = g_tar_mnbrs.get(v_tar);
					mnbr_set.add(propData.matches.getReverse(nbr));
					g_tar_mnbrs.put(v_tar, mnbr_set);
				}
		HashMap<Integer, HashSet> g_src_mnbrs = new HashMap<Integer, HashSet>();
		for(Integer v_src : C_src)
			for(Integer nbr : g_src.neighborsOf(v_src))
				if(propData.matches.isMapped(nbr))
				{
					HashSet<Integer> mnbr_set;
					if(!g_src_mnbrs.containsKey(v_src))
						mnbr_set = new HashSet<Integer>();
					else
						mnbr_set = g_src_mnbrs.get(v_src);
					mnbr_set.add(nbr);
					g_src_mnbrs.put(v_src, mnbr_set);
				}
		
		// Calculate dissims
		int C_tar_ix = 0, C_src_ix = 0;
		for(Integer v_tar : C_tar)
		{
			for(Integer v_src : C_src)
			{
				HashSet<Integer> g_src_mnbrs_of, g_tar_mnbrs_of;

				// Forward similarity
				g_src_mnbrs_of = new HashSet<Integer>(g_src_mnbrs.get(v_src));
				g_src_mnbrs_of.removeAll(g_tar_mnbrs.get(v_tar));
				propData.dissims_src[C_tar_ix][C_src_ix] = ((double)g_src_mnbrs_of.size()) / g_src_mnbrs.get(v_src).size();
				
				// Reverse similarity
				g_tar_mnbrs_of = new HashSet<Integer>(g_tar_mnbrs.get(v_tar));
				g_tar_mnbrs_of.removeAll(g_src_mnbrs.get(v_src));
				propData.dissims_tar[C_tar_ix][C_src_ix] = ((double)g_tar_mnbrs_of.size()) / g_tar_mnbrs.get(v_tar).size();
				
				C_src_ix++;
			}
			C_src_ix = 0;
			C_tar_ix++;
		}
	}
	
	// Eccentricity calculation
	public static double eccentricity(double item, double[] items)
	{
		double ecc = 0.0;
		
		if(items.length < 2)
			return ecc;
		Double[] doubleArray = ArrayUtils.toObject(items);
		List<Double> itemlist = Arrays.asList(doubleArray);
		double std = MyMath.standardDeviation(itemlist);
		if(std == 0.0)
			return ecc;
		
		double abs_diff = 1.0;
		int multitude = 0;
		for(int i = 0; i<items.length; i++)
		{
			if(items[i] != item)
			{
				double diff = Math.abs(items[i] - item);
				if(diff < abs_diff)
					abs_diff = diff;
			}
			else
				multitude++;
		}
		
		ecc = abs_diff / (multitude*std);
		
		return ecc;
	}
	
	public static PerturbData perturbate(MyGraph g, int common_size, int additional_size, double perturbation_rate)
	{
		PerturbData rv = new PerturbData();
		rv.g_src = new MyGraph();
		rv.g_tar = new MyGraph();

		// Init graphs with vertexes
		List<Integer> vs2 = new ArrayList<Integer>();
		List<Integer> vs1 = new ArrayList<Integer>();
		List<Integer> vs3 = new ArrayList<Integer>();
		int tries = 5;
		MyGraph cg = new MyGraph();
		while(cg.vertexSet().size() < common_size && tries > 0)
		{
			cg = g.export(common_size);
			tries--;
		}
		vs2 = new ArrayList<Integer>(cg.vertexSet());

		Queue<Integer> queue = new LinkedList(vs2);
		MyGraph sg = new MyGraph();
		queue.addAll(vs2);

		// Extract additional nodes
		while(queue.size() > 0 && (vs1.size()<additional_size || vs3.size()<additional_size))
		{
			Integer v1 = queue.poll();
			Set<Integer> nbrs = g.neighborsOf(v1);
			for(Integer v2 : nbrs)
			{
				if(vs1.contains(v2) || vs2.contains(v2) || vs3.contains(v2))
					continue;
				queue.offer(v2);

				int choose = ((int)Math.random()*1000) % 2;
				if(choose == 0 && vs1.size()<additional_size)
					vs1.add(v2);
				else if(vs3.size()<additional_size)
					vs3.add(v2);
			}
		}
		
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

		// Project edges
		for(Object o1 : g.vertexSet())
			for(Object o2 : g.vertexSet())
			{
				Integer v1 = (Integer)o1;
				Integer v2 = (Integer)o2;
				if(v1.equals(v2))
					continue;
				
				if(g.neighborsOf(v1).contains(v2))
				{
					if(rv.g_src.containsVertex(v1) && rv.g_src.containsVertex(v2) && !rv.g_src.neighborsOf(v1).contains(v2))
						rv.g_src.addEdge(v1, v2);
					if(rv.g_tar.containsVertex(v1) && rv.g_tar.containsVertex(v2) && !rv.g_tar.neighborsOf(v1).contains(v2))
						rv.g_tar.addEdge(v1, v2);
				}
			}
		
		// Create new edges
		if(perturbation_rate > 0.0)
		{
			double new_edges = (rv.g_tar.edgeSet().size()*perturbation_rate);
			double empty_spaces = (((rv.g_tar.vertexSet().size()-1)*rv.g_tar.vertexSet().size())-rv.g_tar.edgeSet().size());
			double p_new_edge = new_edges / empty_spaces;
			int ctr = 0;
			for(Object o1 : g.vertexSet())
				for(Object o2 : g.vertexSet())
				{
					Integer v1 = (Integer)o1;
					Integer v2 = (Integer)o2;
					if(v1.equals(v2))
						continue;
	
					if(rv.g_tar.containsVertex(v1) && rv.g_tar.containsVertex(v2) && !rv.g_tar.neighborsOf(v1).contains(v2))
					{
						double r = Math.random();
						if(r < p_new_edge)
						{
							rv.g_tar.addEdge(v1, v2);
							ctr++;
						}
					}
				}
		}
		
		// Selecting largest connected component and leaving that only
		rv.g_src.retainLargestConnectedComponent();
		rv.g_tar.retainLargestConnectedComponent();
		
		return rv;
	}
}
