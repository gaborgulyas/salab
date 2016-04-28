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

public class libDEG
{
	// Get degrees for the whole network
	public static HashMap<Integer, Integer> calculate(MyBaseGraph g)
	{
		HashMap<Integer, Integer> dict = new HashMap<Integer, Integer>();
		
		long begin;
		double avg_time = 0.0;
		DebugFrame f = null;
		if(Deanon.DEBUG)
		{
		    f = new DebugFrame();
		    f.setVisible(true);
		}
		
		// Calculate LTAs
		System.out.print("\t  DEG: [");
		int progress = 0;
		long node_count = 0;
		int num_vs = g.vertexSet().size();
		List<Integer> vs = new ArrayList<Integer>(g.vertexSet());
		Collections.sort(vs);
		
		for(Integer v : vs)
		{
			node_count++;
			begin = System.nanoTime() / 1000000;
			
			dict.put(v, g.degreeOf(v));
			
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
				f.info[0].setText("Compiling node degree dictionary");
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
		
		return dict;
	}
	
	// Check if cache exists
	public static boolean cacheExists(String outpath_pref)
	{
		File f = new File(outpath_pref+".deg");
		return f.exists();
	}
	
	// Calculate deg values for all nodes and write it to cache
	public static HashMap<Integer, Integer> writeCache(MyBaseGraph g, String outpath_pref)
	{
		HashMap<Integer, Integer> dict = libDEG.calculate(g);
		
		try
    	{
    		FileOutputStream fos = new FileOutputStream(outpath_pref+".deg");
    		DataOutputStream dos = new DataOutputStream(fos);

    		for(Integer v : dict.keySet())
    		{
    			dos.writeInt(v);
    			dos.writeInt(dict.get(v));
    		}
    		
    		dos.close();
        }
        catch (Exception e) { e.printStackTrace(); }
		
		return dict;
	}
	
	// Get deg values from cache
	public static HashMap<Integer, Integer> readCache(String outpath_pref)
	{
		HashMap<Integer, Integer> dict = new HashMap<Integer, Integer>();

		try
    	{
    		FileInputStream fis = new FileInputStream(outpath_pref+".deg");
    		DataInputStream dis = new DataInputStream(fis);

    		while(dis.available() != 0)
    		{
    			Integer v = new Integer(dis.readInt());
    			if(dis.available() != 0)
    			{
    				Integer deg = new Integer(dis.readInt());
    				dict.put(v, deg);
    			}
    		}
    		dis.close();
        }
        catch (Exception e) { e.printStackTrace(); }

		return dict;
	}
	
	// Get deg values from cache
	public static HashMap<Integer, Double> readCacheAsDoubleDict(String outpath_pref)
	{
		HashMap<Integer, Double> dict = new HashMap<Integer, Double>();

		try
    	{
    		FileInputStream fis = new FileInputStream(outpath_pref+".deg");
    		DataInputStream dis = new DataInputStream(fis);

    		while(dis.available() != 0)
    		{
    			Integer v = new Integer(dis.readInt());
    			if(dis.available() != 0)
    			{
    				Double deg = new Double(dis.readInt());
    				dict.put(v, deg);
    			}
    		}
    		dis.close();
        }
        catch (Exception e) { e.printStackTrace(); }

		return dict;
	}
}
