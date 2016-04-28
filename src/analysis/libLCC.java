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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;

import org.apache.commons.collections.map.MultiValueMap;

import ui.DebugFrame;

import deanon.Deanon;

import mygraph.MyBaseGraph;
import mygraph.MyEdge;
import mygraph.MyShortestPaths;

// Local Clustering Coefficient 
public class libLCC
{
	// Get LCC of a node
	public static Double getNodeLCC(MyBaseGraph g, Integer v)
	{
		Double lcc = 0.0;
		
		long ctr = 0, ectr = 0;
		
		List<Integer> nbrs = new ArrayList<Integer>(g.neighborsOf(v));
		for(Integer v1 : nbrs)
			for(Integer v2 : nbrs)
			{
				if(g.containsEdge(v1, v2))
					ectr++;
				ctr++;
			}
		lcc = (double)ectr / (double)ctr;
		
		return lcc;
	}
	
	// Calculate LCC for all nodes
	public static HashMap<Integer, Double> calculate(MyBaseGraph g, List<Integer> vertexSet)
	{
		HashMap<Integer, Double> dict = new HashMap<Integer, Double>();

		MyBaseGraph g2 = g.selfcopy();
		if(vertexSet != null)
		{
			List<Integer> vs2del = new ArrayList<Integer>(g2.vertexSet());
			vs2del.removeAll(vertexSet);
			g2.removeAllVertices(vs2del);
		}
		
		long start = System.nanoTime() / 1000000, begin;
		double avg_time = 0.0;
		DebugFrame f = null;
		if(Deanon.DEBUG)
		{
		    f = new DebugFrame();
		    f.setVisible(true);
		}
		
		System.out.print("\t  LCC: [");
		int progress = 0, ticks = 0;
		if(vertexSet == null)
			vertexSet = new ArrayList<Integer>(g2.vertexSet());
		int all_ticks = vertexSet.size();
		
		for(Integer v : vertexSet)
		{
			ticks++;
			begin = System.nanoTime() / 1000000;

			dict.put(v, getNodeLCC(g, v));

			int new_progress = Math.round((ticks/(float)all_ticks)*100);
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
				f.info[0].setText("LCC (Local Clustering Coefficient)");
				f.info[1].setText("Round:   "+df.format((avg_time/ticks))+" ms");
				f.info[2].setText("Elapsed: "+df.format(avg_time/1000)+" s");
				f.info[3].setText("Left:    "+df.format(((avg_time/ticks)*(vertexSet.size()-ticks))/1000)+" s");
				f.info[4].setText("Overall: "+df.format((avg_time/ticks)*(vertexSet.size())/1000)+" s ("+df.format((avg_time/ticks)*(vertexSet.size())/60000)+" mins)");
				f.info[5].setText("Progress:");
				f.progressBar.setValue(progress);
			}
		}
		System.out.println("]");
		
		if(Deanon.DEBUG)
		{
			f.setVisible(false);
			f.dispose();
		}

		return dict;
	}
	
	public static HashMap<Integer, Double> calculate(MyBaseGraph g)
	{
		return calculate(g, null);
	}
	
	// Check if LCC cache exists
	public static boolean cacheExists(String outpath)
	{
		File f = new File(outpath+".lcc");
		return f.exists();
	}
	
	// Calculate LCC values for all nodes and write it to cache
	public static void writeCache(MyBaseGraph g, List<Integer> vertexSet, String outpath)
	{
		HashMap<Integer, Double> dict = calculate(g, vertexSet);

		// Cache centrality values
		try
    	{
    		FileOutputStream lcc_fos = new FileOutputStream(outpath+".lcc");
    		DataOutputStream lcc_dos = new DataOutputStream(lcc_fos);

    		for(Integer v : dict.keySet())
    		{
    			lcc_dos.writeInt(v);
    			lcc_dos.writeDouble(dict.get(v));
    		}

    		lcc_dos.close();
        }
        catch (Exception e) { e.printStackTrace(); }
	}
	
	// Get LCC values from cache
	public static HashMap<Integer, Double> readCache(String outpath)
	{
		HashMap<Integer, Double> dict = new HashMap<Integer, Double>();

		try
    	{
    		FileInputStream fis = new FileInputStream(outpath+".lcc");
    		DataInputStream dis = new DataInputStream(fis);

    		while(dis.available() != 0)
    		{
    			Integer v = new Integer(dis.readInt());
    			if(dis.available() != 0)
    			{
    				Double val = new Double(dis.readDouble());
    				dict.put(v, val);
    			}
    			
    		}
    		dis.close();
        }
        catch (Exception e) { e.printStackTrace(); }

		return dict;
	}
}
