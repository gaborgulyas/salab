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

// Betweenneess and closeness centrality (together) 
public class libBCC
{
	public static class BCCDouble
	{
		public Double bwc, clc;
		
		public BCCDouble(Double b, Double c)
		{
			bwc = new Double(b);
			clc = new Double(c);
		}
		
	}
	
	public static class BCCSumDouble
	{
		public Double sum;
		public Integer counter;
		
		public BCCSumDouble(double s, int c)
		{
			sum = new Double(s);
			counter = new Integer(c);
		}
	}
	
	// Calculate betweenness centrality
	public static HashMap<Integer, BCCDouble> calculate(MyBaseGraph g, List<Integer> vertexSet)
	{
		HashMap<Integer, BCCDouble> dict = new HashMap<Integer, BCCDouble>();

		MyBaseGraph g2 = g.selfcopy();
		if(vertexSet != null)
		{
			List<Integer> vs2del = new ArrayList<Integer>(g2.vertexSet());
			vs2del.removeAll(vertexSet);
			g2.removeAllVertices(vs2del);
		}
		
		long begin;
		double avg_time = 0.0;
		DebugFrame f = null;
		if(Deanon.DEBUG)
		{
		    f = new DebugFrame();
		    f.setVisible(true);
		}
		
		System.out.print("\t  BCC: [");
		int progress = 0, ticks = 0;
		if(vertexSet == null)
			vertexSet = new ArrayList<Integer>(g2.vertexSet());
		int all_ticks = vertexSet.size();
		
		for(Integer v : vertexSet)
		{
			ticks++;
			begin = System.nanoTime() / 1000000;
			
			MultiValueMap paths = MyShortestPaths.getPathsFromV(g2, v);

			Set<Integer> keys = new TreeSet(paths.keySet());
			for(Integer v2 : keys)
			{
				if(v2.intValue() <= v.intValue())
					continue;
				
				HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
				
				ArrayList paths2 = new ArrayList(paths.getCollection(v2));
				
				// Closeness (for v <-> v2)
				int distance = ((ArrayList<Integer>) paths2.get(0)).size();
				if(!dict.containsKey(v))
					dict.put(v, new BCCDouble(0.0, (double)distance));
				else
				{
					BCCDouble bwd = dict.get(v);
					dict.put(v, new BCCDouble(bwd.bwc, bwd.clc + distance));
				}
				
				// Betweenness calculation
				for(int i = 0; i < paths2.size(); i++)
				{
					ArrayList<Integer> path = (ArrayList<Integer>) paths2.get(i);
					for(Integer v3 : path)
					{
						// Scoring for nodes
						if(path.indexOf(v3) > 0 && path.indexOf(v3) < path.size()-1)
						{
							if(!scores.containsKey(v3))
								scores.put(v3, 1.0);
							else
							{
								Double bws = scores.get(v3);
								scores.put(v3, new Double(bws + 1.0));
							}
						}
					}
				}
				
				for(Integer tv : scores.keySet())
				{
					Double score = scores.get(tv);
					score = score / paths2.size();
					
					if(!dict.containsKey(tv))
						dict.put(tv, new BCCDouble(score, 0.0));
					else
					{
						BCCDouble bwd = dict.get(tv);
						dict.put(tv, new BCCDouble(bwd.bwc + score, bwd.clc));
					}
				}
			}

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
				f.info[0].setText("Betweenness/Closeness");
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
		
		// Set CLC values
		for(Integer v : dict.keySet())
		{
			BCCDouble bwd = dict.get(v);
			bwd.clc = 1 / bwd.clc;
			dict.put(v, bwd);
		}

		return dict;
	}
	
	public static HashMap<Integer, BCCDouble> calculate(MyBaseGraph g)
	{
		return calculate(g, null);
	}
	
	// Check if BWC cache exists
	public static boolean betweennessCacheExists(String outpath)
	{
		File f = new File(outpath+".bwc");
		return f.exists();
	}
	
	// Check if CLC cache exists
	public static boolean closenessCacheExists(String outpath)
	{
		File f = new File(outpath+".clc");
		return f.exists();
	}
	
	// Calculate BCC values for all nodes and write it to cache
	public static void writeCaches(MyBaseGraph g, List<Integer> vertexSet, String outpath)
	{
		HashMap<Integer, BCCDouble> dict = calculate(g, vertexSet);
		
		// Cache centrality values
		try
    	{
    		FileOutputStream bwc_fos = new FileOutputStream(outpath+".bwc");
    		DataOutputStream bwc_dos = new DataOutputStream(bwc_fos);
    		FileOutputStream clc_fos = new FileOutputStream(outpath+".clc");
    		DataOutputStream clc_dos = new DataOutputStream(clc_fos);

    		for(Integer v : dict.keySet())
    		{
    			BCCDouble bcd = dict.get(v);
    			bwc_dos.writeInt(v);
    			bwc_dos.writeDouble(bcd.bwc);
    			
    			clc_dos.writeInt(v);
    			clc_dos.writeDouble(bcd.clc);
    		}

    		bwc_dos.close();
    		clc_dos.close();
        }
        catch (Exception e) { e.printStackTrace(); }
	}
	
	// Get betweenness values from cache
	public static HashMap<Integer, Double> readBetweennessCache(String outpath)
	{
		HashMap<Integer, Double> dict = new HashMap<Integer, Double>();

		try
    	{
    		FileInputStream fis = new FileInputStream(outpath+".bwc");
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
	
	// Get closeness values from cache
	public static HashMap<Integer, Double> readClosenessCache(String outpath)
	{
		HashMap<Integer, Double> dict = new HashMap<Integer, Double>();

		try
    	{
    		FileInputStream fis = new FileInputStream(outpath+".clc");
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
