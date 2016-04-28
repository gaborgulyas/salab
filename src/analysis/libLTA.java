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
package analysis;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ui.DebugFrame;
import deanon.Deanon;
import deanon.libNS09;
import mygraph.MyBaseGraph;
import mygraph.MyGraph;
import mygraph.MyMath;

public class libLTA
{
	public static final int LTA_A = 0;
	public static final int LTA_B = 1;
	public static final int LTA_C = 2;
	public static final int LTA_D = 3;
	public static final int NUM_LTA_VARS = 4;
	public static final char[] LTA_VAR_ID = {'a', 'b', 'c', 'd'};
	public static final char[] LTA_VAR_ID_UC = {'A', 'B', 'C', 'D'};

	// Cosine similarity between two nodes
	public static double cosSim(List<Integer> nbrs1, List<Integer> nbrs2)
	{
		double cossim = 0.0;
		
		List<Integer> common_nbrs = new ArrayList(nbrs1);
		common_nbrs.retainAll(nbrs2);
		
		cossim = common_nbrs.size() / Math.sqrt(nbrs1.size() * nbrs2.size());
		
		return cossim;
	}
	
	// Calculate different LTA values
	public static double[] calculateLTAOf(MyBaseGraph g, Integer v)
	{
		double[] lta = new double[NUM_LTA_VARS];
		
		// Get all neighbors of neighbors
		HashSet<Integer> nbrs_of_nbrs = new HashSet<Integer>();
		List<Integer> nbrs = new ArrayList(g.neighborsOf(v));
		for(Integer v1 : nbrs)
		{
			List<Integer> nbrs2 = new ArrayList(g.neighborsOf(v1));
			nbrs_of_nbrs.addAll(nbrs2);
		}
		nbrs_of_nbrs.remove(v);
		
		// Calculate sum of similarity
		double sum_cossim = 0.0;
		List<Double> deg_diffs = new ArrayList<Double>();
		int nbrs_size = nbrs.size();
		for(Integer v1 : nbrs_of_nbrs)
		{
			List<Integer> nbrs1 = new ArrayList(g.neighborsOf(v1));
			
			sum_cossim += cosSim(nbrs, nbrs1);
			deg_diffs.add((double) Math.abs(nbrs_size-nbrs1.size()));
		}
		
		// Calculate variance for LTA_c
		double deg_sd = 1.0;
		if(deg_diffs.size() > 1)
			deg_sd = MyMath.standardDeviation(deg_diffs);
		if(deg_sd < 1.0)
			deg_sd = 1.0;
		
		// Finally, calculate LTAs
		lta[LTA_A] = sum_cossim / nbrs_of_nbrs.size();
		lta[LTA_B] = sum_cossim / Math.max(nbrs_size, 2);
		lta[LTA_C] = lta[LTA_A] / deg_sd;
		
		return lta;
	}
	
	// Calculate LTA for the whole network
	public static HashMap<Integer, Double> calculate(MyBaseGraph g, int variant)
	{
		HashMap<Integer, Double> ltadict = new HashMap<Integer, Double>();
		
		long begin;
		double avg_time = 0.0;
		DebugFrame f = null;
		if(Deanon.DEBUG)
		{
		    f = new DebugFrame();
		    f.setVisible(true);
		}
		
		// Calculate LTAs
		System.out.print("\t  LTA: [");
		int progress = 0;
		long node_count = 0;
		int num_vs = g.vertexSet().size();
		List<Integer> vs = new ArrayList<Integer>(g.vertexSet());
		Collections.sort(vs);
		
		for(Integer v : vs)
		{
			node_count++;
			begin = System.nanoTime() / 1000000;
			
			double[] ltas = calculateLTAOf(g, v);
			
			ltadict.put(v, ltas[variant]);
			
			int new_progress = Math.round((node_count/(float)num_vs)*100);
			if(new_progress-progress > 0)
			{
				for(int i = 0; i < new_progress-progress; i++)
					System.out.print(".");
				progress = new_progress;
			}
			
			if(Deanon.DEBUG)
			{
				avg_time += System.nanoTime() / 1000000 - begin;
				DecimalFormat df = new DecimalFormat("0.00");
				f.info[0].setText("Calculating LTA (Local Topological Anonymity)");
				f.info[1].setText("Round:   "+df.format((avg_time/node_count))+" ms");
				f.info[2].setText("Elapsed: "+df.format(avg_time/1000)+" s");
				f.info[3].setText("Left:    "+df.format(((avg_time/node_count)*(g.vertexSet().size()-node_count))/1000)+" s");
				f.info[4].setText("Overall: "+df.format((avg_time/node_count)*(g.vertexSet().size())/1000)+" s ("+df.format((avg_time/node_count)*(g.vertexSet().size())/60000)+" mins)");
				f.info[5].setText("Progress:");
				f.progressBar.setValue(progress);
			}
		}
		
		System.out.print("]\n");
		
		if(Deanon.DEBUG)
		{
			f.setVisible(false);
			f.dispose();
		}
		
		return ltadict;
	}
	
	// Calculate LTA for the whole network
	public static HashMap<Integer, Double> calculateLTA_D(MyBaseGraph g)
	{
		HashMap<Integer, Double> ltadict = new HashMap<Integer, Double>();
		
		long begin;
		double avg_time = 0.0;
		
		// Calculate LTAs
		System.out.print("\t  LTA_D: [");
		int progress = 0;
		long node_count = 0;
		int num_vs = g.vertexSet().size();
		List<Integer> vs = new ArrayList<Integer>(g.vertexSet());
		Collections.sort(vs);
		
		for(Integer v : vs)
		{
			node_count++;
			begin = System.nanoTime() / 1000000;
			
			double lta = 0.0;
			
			// Get all neighbors of neighbors
			HashSet<Integer> nbrs_of_nbrs = new HashSet<Integer>();
			List<Integer> nbrs = new ArrayList(g.neighborsOf(v));
			for(Integer v1 : nbrs)
			{
				List<Integer> nbrs2 = new ArrayList(g.neighborsOf(v1));
				nbrs_of_nbrs.addAll(nbrs2);
			}
			nbrs_of_nbrs.remove(v);
			for(int nbr : nbrs)
				if(nbrs_of_nbrs.contains(nbr))
					nbrs_of_nbrs.remove(nbr);
			
			// Calculate sum of similarity
			double sum_cossim = 0.0;
			int nbrs_size = nbrs.size();
			for(Integer v1 : nbrs_of_nbrs)
			{
				List<Integer> nbrs1 = new ArrayList(g.neighborsOf(v1));
				sum_cossim += cosSim(nbrs, nbrs1);
			}
			lta = sum_cossim / nbrs_of_nbrs.size();
			
			ltadict.put(v, lta);
			
			int new_progress = Math.round((node_count/(float)num_vs)*100);
			if(new_progress-progress > 0)
			{
				for(int i = 0; i < new_progress-progress; i++)
					System.out.print(".");
				progress = new_progress;
			}
		}
		
		System.out.print("]\n");
		
		return ltadict;
	}
	
	// Calculate LTA for the whole network
	public static HashMap<Integer, double[]> calculateAll(MyBaseGraph g)
	{
		HashMap<Integer, double[]> ltadict = new HashMap<Integer, double[]>();
		
		long begin;
		double avg_time = 0.0;
		DebugFrame f = null;
		if(Deanon.DEBUG)
		{
		    f = new DebugFrame();
		    f.setVisible(true);
		}
		
		// Calculate LTAs
		System.out.print("\t  LTA: [");
		int progress = 0;
		long node_count = 0;
		int num_vs = g.vertexSet().size();
		List<Integer> vs = new ArrayList<Integer>(g.vertexSet());
		Collections.sort(vs);
		
		for(Integer v : vs)
		{
			node_count++;
			begin = System.nanoTime() / 1000000;
			
			double[] ltas = calculateLTAOf(g, v);
			
			ltadict.put(v, ltas);
			
			int new_progress = Math.round((node_count/(float)num_vs)*100);
			if(new_progress-progress > 0)
			{
				for(int i = 0; i < new_progress-progress; i++)
					System.out.print(".");
				progress = new_progress;
			}
			
			if(Deanon.DEBUG)
			{
				avg_time += System.nanoTime() / 1000000 - begin;
				DecimalFormat df = new DecimalFormat("0.00");
				f.info[0].setText("Calculating LTA (Local Topological Anonymity)");
				f.info[1].setText("Round:   "+df.format((avg_time/node_count))+" ms");
				f.info[2].setText("Elapsed: "+df.format(avg_time/1000)+" s");
				f.info[3].setText("Left:    "+df.format(((avg_time/node_count)*(g.vertexSet().size()-node_count))/1000)+" s");
				f.info[4].setText("Overall: "+df.format((avg_time/node_count)*(g.vertexSet().size())/1000)+" s ("+df.format((avg_time/node_count)*(g.vertexSet().size())/60000)+" mins)");
				f.info[5].setText("Progress:");
				f.progressBar.setValue(progress);
			}
		}
		
		System.out.print("]\n");
		
		if(Deanon.DEBUG)
		{
			f.setVisible(false);
			f.dispose();
		}
		
		return ltadict;
	}
	
	// Check if cache exists
	public static boolean cacheExists(String outpath_pref, int variant)
	{
		File f = new File(outpath_pref+"_v"+LTA_VAR_ID_UC[variant]+".lta");
		return f.exists();
	}
	
	// Calculate LTA values for all nodes and write it to cache
	public static HashMap<Integer, Double> writeCache(MyBaseGraph g, String outpath_pref, int variant, List<Integer> vertexSet)
	{
		HashMap<Integer, Double> ltadict;
		if(variant == libLTA.LTA_D)
			ltadict = libLTA.calculateLTA_D(g);
		else
			ltadict = libLTA.calculate(g, variant);
		
		try
    	{
    		FileOutputStream fos = new FileOutputStream(outpath_pref+"_v"+LTA_VAR_ID_UC[variant]+".lta");
    		DataOutputStream dos = new DataOutputStream(fos);

    		if(vertexSet == null)
    			vertexSet = new ArrayList<Integer>(ltadict.keySet());
    		for(Integer v : vertexSet)
    		{
    			if(!ltadict.containsKey(v))
    				continue;
    			dos.writeInt(v);
    			dos.writeDouble(ltadict.get(v));
    		}
    		
    		dos.close();
        }
        catch (Exception e) { e.printStackTrace(); }
		
		return ltadict;
	}
	
	public static HashMap<Integer, Double> writeCache(MyBaseGraph g, String outpath_pref, int variant)
	{
		return writeCache(g, outpath_pref, variant, null);
	}
	
	// Calculate LTA values for all nodes and write it to cache
	public static HashMap<Integer, double[]> writeCaches(MyBaseGraph g, String outpath_pref)
	{
		HashMap<Integer, double[]> ltadict = libLTA.calculateAll(g);
		
		int lta_vars[] = {LTA_A, LTA_B, LTA_C};
		for(int lta_var : lta_vars){
			try
	    	{
	    		FileOutputStream fos = new FileOutputStream(outpath_pref+"_v"+LTA_VAR_ID_UC[lta_var]+".lta");
	    		DataOutputStream dos = new DataOutputStream(fos);

    			List<Integer> vertexSet = new ArrayList<Integer>(ltadict.keySet());
	    		for(Integer v : vertexSet)
	    		{
	    			if(!ltadict.containsKey(v))
	    				continue;
	    			dos.writeInt(v);
	    			dos.writeDouble(ltadict.get(v)[lta_var]);
	    		}
	    		
	    		dos.close();
	        }
	        catch (Exception e) { e.printStackTrace(); }
		}
		return ltadict;
	}
	
	// Get LTA values from cache
	public static HashMap<Integer, Double> readCache(String outpath_pref, int variant)
	{
		HashMap<Integer, Double> ltadict = new HashMap<Integer, Double>();

		try
    	{
    		FileInputStream fis = new FileInputStream(outpath_pref+"_v"+LTA_VAR_ID_UC[variant]+".lta");
    		DataInputStream dis = new DataInputStream(fis);

    		while(dis.available() != 0)
    		{
    			Integer v = new Integer(dis.readInt());
    			if(dis.available() != 0)
    			{
    				Double lta = new Double(dis.readDouble());
    				ltadict.put(v, lta);
    			}
    			
    		}
    		dis.close();
        }
        catch (Exception e) { e.printStackTrace(); }

		return ltadict;
	}
}
