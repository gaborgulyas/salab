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

import java.awt.Color;
import java.io.*;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.collections.map.MultiValueMap;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;

import deanon.structures.GroundTruth;
import deanon.structures.Matches;
import deanon.structures.PerturbData;
import deanon.structures.PropagationData;
import deanon.structures.tuples.DegIx;
import analysis.libBCC;
import analysis.libDEG;
import analysis.libLCC;
import analysis.libLTA;
import mygraph.*;

public class Deanon
{
	public static boolean DEBUG = false;
	
	public static String identifier;
	public static String outdir;
	public static String logfile = "./output/log.txt";
	
	/*
	 * Generic utilities
	 * */
	public static boolean deleteDirectory(File directory)
	{
		if(directory == null)
			return false;
		if(!directory.exists())
			return true;
		if(!directory.isDirectory())
			return false;

		String[] list = directory.list();

		if(list != null)
		{
			for(int i = 0; i < list.length; i++)
			{
				File entry = new File(directory, list[i]);
				if(entry.isDirectory())
				{
					if(!deleteDirectory(entry))
						return false;
				}
				else if(!entry.delete())
					return false;
			}
		}

		return directory.delete();
	}
	
	// Log and print information
	public static void log(String s)
	{
		System.out.println(s);
		
		try
		{
			File file = new File(logfile);
			if(!file.exists())
				file.createNewFile();
			
			FileWriter fWriter = new FileWriter(logfile, true);
			BufferedWriter writer = new BufferedWriter(fWriter);
	        writer.append(s);
	        writer.newLine();
	        writer.close();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	public static void log()
	{
		try
		{
			File file = new File(logfile);
			if(file.exists())
			{
				file.delete();
				file.createNewFile();
			}
			else
				file.createNewFile();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	
	// Log a line to a specific file
	public static void log_line(String path, String s)
	{
		try
		{
			File logfile = new File(path);
			
			if(s == null)
			{
				if(logfile.exists())
					logfile.delete();
				logfile.createNewFile();
				return;
			}
			
			FileWriter fWriter = new FileWriter(path, true);
			BufferedWriter writer = new BufferedWriter(fWriter);
	        writer.append(s);
	        writer.newLine();
	        writer.close();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	public static void log_line(String path) { log_line(path, null); }
	
	// Log statistics
	public static double[] log_stats(String path, MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt, Matches matches)
	{
		double[] accuracy = {0.0, 0.0, 0.0, 0.0};
		
		if(matches == null)
		{
			log_line(path);
			String s = "";
			List<Integer> vs = new ArrayList<Integer>();
			vs.addAll(gt.common_vertices);
			vs.addAll(gt.mappings.keySet());
			for(Integer v : vs)
			{
				s += v+";";
			}
			log_line(path, s);
		}
		else
		{
			String s = "";
			int score = 0;
			List<Integer> vs = new ArrayList<Integer>();
			vs.addAll(gt.common_vertices);
			vs.addAll(gt.mappings.keySet());

			// Mappings that should not even exist:
			List<Integer> outofgt = new ArrayList<Integer>(matches.forward.keySet());
			outofgt.removeAll(vs);

			int ctr = 0;
			for(Integer v : vs)
			{
				if(matches.isMapped(v))
					score = gt.getScore(v, matches.get(v));
				else
					score = 0;
				if(score > 0 && gt.mappings.keySet().contains(v))
					ctr++;
				s += score+";";
				if(score == 1)
					accuracy[2] += 1.0;
				else if(score == 0)
					accuracy[1] += 1.0;
				else
					accuracy[0] += 1.0;
			}
			log_line(path, s);
			
			accuracy[2] = accuracy[2] / vs.size();
			accuracy[1] = accuracy[1] / vs.size();
			accuracy[3] = (accuracy[0]+outofgt.size()) / vs.size(); // error over the ground truth
			accuracy[0] = accuracy[0] / vs.size();
		}
		
		return accuracy;
	}
	public static double[] log_stats(String path, MyBaseGraph g_src, MyBaseGraph g_tar, GroundTruth gt) { return log_stats(path, g_src, g_tar, gt, null); }
	
	// Log the matches
	public static void log_matches(String path, MyBaseGraph g_src, GroundTruth gt, Matches matches)
	{
		try
    	{
    		FileOutputStream fos = new FileOutputStream(path);
    		DataOutputStream dos = new DataOutputStream(fos);

    		// Log the ground truth matches only
    		List<Integer> vs = new ArrayList<Integer>();
			vs.addAll(gt.common_vertices);
			vs.addAll(gt.mappings.keySet());
			dos.writeInt(matches.size());
			for(Integer v : vs)
			{
				if(matches.isMapped(v))
				{
					dos.writeInt(v);
					dos.writeInt(matches.get(v));
				}
			}
    		dos.close();
    		
    		fos = new FileOutputStream(path.replace(".dat", ".edat"));
    		dos = new DataOutputStream(fos);
    		// Log extended matches
			dos.writeInt(matches.size());
			for(Integer v : matches.forward.keySet())
			{
				dos.writeInt(v);
				dos.writeInt(matches.get(v));
			}
    		dos.close();
        }
        catch (Exception e) { e.printStackTrace(); }
	}
	
	// Short name for the simulation
	public static String getIdentifier(String network, Integer size, String mark)
	{
		String network_name = new String(network);
		
		if(network_name.contains("_directed"))
			network_name = network_name.substring(0, network.length()-9);
		
		String nwshort = network_name;

		if(network.contains("_directed"))
			nwshort = nwshort + "d";
		
		if(size > 0)
		{
			Float float_size = ((float)size)/1000;
			nwshort = nwshort + "_" + float_size.toString() + "k_" + mark;
		}
		else
			nwshort = nwshort + "_" + mark;
		
		return nwshort;
	}
	
	// Count common nodes
	public static List<Integer> getVertexOverlap(MyBaseGraph g_src, MyBaseGraph g_tar)
	{
		List<Integer> vs_src = new ArrayList(g_src.vertexSet());
		List<Integer> vs_tar = new ArrayList(g_tar.vertexSet());
		
		vs_src.retainAll(vs_tar);
		
		return vs_src;
	}
	
	// Cache vertex overlap
	public static List<Integer> cacheVertexOverlap(MyBaseGraph g_src, MyBaseGraph g_tar, String outfile)
	{
		List<Integer> common_vertices = new ArrayList(g_src.vertexSet());
		List<Integer> vs_tar = new ArrayList(g_tar.vertexSet());
		common_vertices.retainAll(vs_tar);
		Collections.sort(common_vertices);
		writeIntListFile(common_vertices, outfile);
		
		return common_vertices;
	}
	
	// Timestamp of now
	public static String timestamp()
	{
		java.util.Date date = new java.util.Date();
		return new Timestamp(date.getTime()).toString();
	}
	
	// Writing List<Integer> to file
	public static void writeIntListFile(List<Integer> il, String path)
    {
    	try
    	{
    		FileOutputStream fos = new FileOutputStream(path);
    		DataOutputStream dos = new DataOutputStream(fos);

    		for(Integer i : il)
    			dos.writeInt(i.intValue());
    		dos.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    }
    
	// Reading List<Integer> from file
    public static List<Integer> readIntListFile(String path)
    {
    	List<Integer> int_list = new ArrayList<Integer>();
    	
    	try
    	{
    		FileInputStream fis = new FileInputStream(path);
    		DataInputStream dis = new DataInputStream(fis);

    		while(dis.available() != 0)
    			int_list.add(new Integer(dis.readInt()));
    		dis.close();
        }
        catch (Exception e) { e.printStackTrace(); }
    	
    	return int_list;
    }
    
    // Writing MultiValueMap to file
 	public static void writeMultiValueMapFile(MultiValueMap mapping, String path)
    {
 		try
    	{
    		FileOutputStream fos = new FileOutputStream(path);
    		DataOutputStream dos = new DataOutputStream(fos);

    		Set<Integer> keys = new TreeSet(mapping.keySet());
    		for(Integer key : keys)
    		{
    			Set<Integer> values = new TreeSet(mapping.getCollection(key));
    			dos.writeInt(key.intValue());
    			dos.writeInt(values.size());
    			for(Integer value : values)
    				dos.writeInt(value.intValue());
    		}
     		dos.close();
         }
         catch (Exception e) { e.printStackTrace(); }
     }
     
 	// Reading MultiValueMap from file
    public static MultiValueMap readMultiValueMapFile(String path)
    {
    	MultiValueMap mapping = new MultiValueMap();
     	
    	try
    	{
    		FileInputStream fis = new FileInputStream(path);
    		DataInputStream dis = new DataInputStream(fis);

     		int len, key, tmp;
     		while(dis.available() != 0)
     		{
     			key = dis.readInt();
     			if(dis.available() != 0)
     			{
     				len = dis.readInt();
	    			while(dis.available() != 0 && len > 0)
	    			{
	    				tmp = dis.readInt();
	    				mapping.put(key, tmp);
	    				len--;
	    			}
     			}
     		}
     		dis.close();
        }
        catch (Exception e) { e.printStackTrace(); }
     	
     	return mapping;
     }
    
    // Writing HashMap<Integer, Integer> to file
 	public static void writeHashMapFile(HashMap<Integer, Integer> hm, String path)
     {
 		if(hm == null || hm.size() == 0)
 			return;
 		
     	try
     	{
     		FileOutputStream fos = new FileOutputStream(path);
     		DataOutputStream dos = new DataOutputStream(fos);

     		for(Integer k : hm.keySet())
     		{
     			dos.writeInt(k.intValue());
     			dos.writeInt(hm.get(k).intValue());
     		}
     		dos.close();
         }
         catch (Exception e) { e.printStackTrace(); }
     }
	
	/*
	 * Simulation tools
	 * */
	// Export subgraph
	public static void exportSubnet(String[] args)
	{
		if(args.length == 5 && args[4].equals("directed"))
		{
			// Load source graph
			System.out.println("> Loading: "+args[1]+".tgf (as directed)");
			MyDirectedGraph g_src = new MyDirectedGraph("./data/"+args[1]+".tgf");
			System.out.println("> Exporting a subnetwork of "+args[3]+" nodes");
			MyDirectedGraph g_exp = g_src.export(Integer.parseInt(args[3]));
			System.out.println("\t"+g_exp.vertexSet().size()+" nodes, "+g_exp.edgeSet().size()+" edges");
			System.out.println("> Saving: "+args[2]+".tgf (as directed)");
			g_exp.writeToTGF("./output/"+args[2]+".tgf");
		}
		else if(args.length == 4 || (args.length == 5 && Integer.parseInt(args[4]) > 0))
			{
				int counter = 1;
				if(args.length == 5 && Integer.parseInt(args[4]) > 0)
					counter = Integer.parseInt(args[4]);
			
				// Load source graph
				System.out.println("> Loading: "+args[1]+".tgf");
				MyGraph g_src = new MyGraph("./data/"+args[1]+".tgf");
				System.out.println("\t"+g_src.vertexSet().size()+" nodes, "+g_src.edgeSet().size()+" edges");
				while(counter > 0)
				{
					System.out.println("> Exporting a subnetwork of "+args[3]+" nodes [v"+counter+"]");
					MyGraph g_exp = g_src.export(Integer.parseInt(args[3]));
					System.out.println("\t"+g_exp.vertexSet().size()+" nodes, "+g_exp.edgeSet().size()+" edges");
					System.out.println("> Saving: "+args[2]+".tgf");
					g_exp.writeToTGF("./output/"+args[2]+"_"+counter+".tgf");
					counter--;
				}
			}
	}
	
	// Summarize basic statistics of a network
	public static void summarizeNetwork(String[] args)
	{
		outdir = "./output/";

		String path = "./data/"+args[1]+".tgf";
		String outpath_prefix = "./output/"+args[1];
		if(args[1].indexOf('/') > -1)
		{
			path = args[1];
			outpath_prefix = "./output/"+ (args[1].substring(args[1].lastIndexOf("/") + 1)).replaceAll("\\.tgf", "");
		}
		
		logfile = outpath_prefix+".log";
		System.out.println(logfile);
		
		// Start
		log();
		log("@ "+timestamp());
		log("Summarizing: "+args[1]);
		MyGraph g = new MyGraph(path);
		
		// Basic info
		log("\t |V| = "+g.vertexSet().size()+", |E| = "+g.edgeSet().size());
		
		// Degree distribution
		Map<Integer, Integer> freqs = new HashMap<Integer, Integer>();
		List<Integer> vs = new ArrayList<Integer>(g.vertexSet());
		for(Integer v : vs)
		{
			Integer degv = g.degreeOf(v);
			if(!freqs.containsKey(degv))
				freqs.put(degv, 1);
			else
			{
				Integer val = freqs.get(degv) + 1;
				freqs.put(degv, val);
			}
		}
		
		// Basic stats
		log("\t |deg(v)=1| = "+freqs.get(1)+" ("+(new DecimalFormat("#.##").format(((float)freqs.get(1)/g.vertexSet().size())*100))+"%)");
		log("\t |deg(v)=2| = "+freqs.get(2)+" ("+(new DecimalFormat("#.##").format(((float)freqs.get(2)/g.vertexSet().size())*100))+"%)");
		log("\t |deg(v)=3| = "+freqs.get(3)+" ("+(new DecimalFormat("#.##").format(((float)freqs.get(3)/g.vertexSet().size())*100))+"%)");
		log("\t |deg(v) in {1, 2, 3}| = "+(freqs.get(1)+freqs.get(2)+freqs.get(3))+" ("+(new DecimalFormat("#.##").format(((float)(freqs.get(1)+freqs.get(2)+freqs.get(3))/g.vertexSet().size())*100))+"%)");
		
		// Get it on a histogram
		XYSeries lta_series = new XYSeries("Degree distribution");
		for(Integer deg : freqs.keySet())
			lta_series.add(deg, freqs.get(deg));
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(lta_series);

		JFreeChart chart = ChartFactory.createScatterPlot("Degree distribution", "Degrees", "Frequency", dataset, PlotOrientation.VERTICAL, true, true, false);

		File plotfile = new File(outpath_prefix+"_dd.png");
		XYPlot plot = (XYPlot)chart.getPlot();
		plot.setBackgroundPaint(Color.WHITE); 
		try
		{
			ChartUtilities.saveChartAsPNG(plotfile, chart, 1000, 1000);
		} catch (IOException e) { e.printStackTrace(); }
		
		FileWriter f0;
		try {
			f0 = new FileWriter(outpath_prefix+"_dd.csv");
			Iterator it = freqs.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        f0.write(pairs.getKey() +";"+ pairs.getValue() + "\n");
		        it.remove();
		    }
		    f0.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Create test data
	public static void createTestData(String[] args)
	{
		// Init
		identifier = getIdentifier(args[1], Integer.parseInt(args[2]), args[3]);
		outdir = "./output/"+identifier;
		logfile = outdir+"/create.log";
		
		boolean create_data = !args[5].equals("copyfirst");
		
		// Creating directory structure
		if(create_data)
		{
			File foutdir = new File(outdir);
			if(foutdir.exists())
				deleteDirectory(foutdir);
			foutdir.mkdir();
			new File(outdir+"/SimuData").mkdir();
			new File(outdir+"/Measures").mkdir();
		}
		
		// Exporting
		log("@ "+timestamp());
		MyBaseGraph g = new MyDirectedGraph();
		if(create_data)
		{
			log("Creating test data for: "+args[1]+" ["+args[3]+"]");
			if(args[1].contains("_directed"))
				g = new MyDirectedGraph("./data/"+args[1]+".tgf");
			else
				g = new MyGraph("./data/"+args[1]+".tgf");
			log("\t(Nodes: "+Integer.toString(g.vertexSet().size())+", edges: "+Integer.toString(g.edgeSet().size())+")");
			log("\tExport params: exports="+args[4]+", export_size="+args[2]);
		}
		else
		{
			log("Transforming test data for: "+args[1]+" ["+args[3]+"]");
		}
		
		if(args[5].equals("ns09") || args[5].equals("dns09"))
			log("\tPerturb. params: algo="+args[5]+", variants="+args[6]+", alpha_v="+args[7]+", alpha_e="+args[8]);
		else if(args[5].equals("sng"))
			log("\tPerturb. params: algo="+args[5]+", variants="+args[6]+", common_nodes="+args[7]+", additional_nodes="+args[8]+", perturbation_rate="+args[9]);
		else if(args[5].equals("copyfirst"))
			log("\tPerturb. params: algo=copyfirst");
		else if(args[5].equals("sample"))
			log("\tPerturb. params: algo=sample");
		else
			log("\tPerturb. params: algo=clone");
		for(int i = 0; i < Integer.parseInt(args[4]); i++)
		{
			MyBaseGraph sg = new MyDirectedGraph();
			String export_path = outdir+"/SimuData/e"+Integer.toString(i)+".tgf";
			// Creating the export
			if(create_data)
			{
				if(Integer.parseInt(args[2]) > 0)
					sg = g.export(Integer.parseInt(args[2]));
				else
					sg = g;
				sg.writeToTGF(export_path);
				
				log("\tExport: "+export_path+", nodes: "+Integer.toString(sg.vertexSet().size())+", edges: "+Integer.toString(sg.edgeSet().size()));
			}
			else
				log("\tExport: "+export_path);
			
			// Creating perturbed version
			PerturbData rv;
			if(args[5].equals("ns09"))
			{
				for(int j = 0; j < Integer.parseInt(args[6]); j++)
				{
					log("\t\tVariant: v"+Integer.toString(j));
					
					rv = libNS09.perturbate((MyGraph)sg, Float.parseFloat(args[7]), Float.parseFloat(args[8]));
					rv.g_src.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_src.tgf");
					rv.g_tar.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_tar.tgf");
					
					log("\t\t\tg_src nodes: "+rv.g_src.vertexSet().size()+", edges: "+rv.g_src.edgeSet().size());
					log("\t\t\tg_tar nodes: "+rv.g_tar.vertexSet().size()+", edges: "+rv.g_tar.edgeSet().size());
					
					List<Integer> common_vertices = cacheVertexOverlap(rv.g_src, rv.g_tar, outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_overlap.cache");
					log("\t\t\tnode overlap: "+common_vertices.size());
				}
			}
			else if(args[5].equals("dns09"))
			{
				for(int j = 0; j < Integer.parseInt(args[6]); j++)
				{
					log("\t\tVariant: v"+Integer.toString(j));
					
					rv = libDNS09.perturbate((MyDirectedGraph)sg, Float.parseFloat(args[7]), Float.parseFloat(args[8]));
					rv.g_src.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_src.tgf");
					rv.g_tar.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_tar.tgf");
					
					log("\t\t\tg_src nodes: "+rv.g_src.vertexSet().size()+", edges: "+rv.g_src.edgeSet().size());
					log("\t\t\tg_tar nodes: "+rv.g_tar.vertexSet().size()+", edges: "+rv.g_tar.edgeSet().size());
					
					List<Integer> common_vertices = cacheVertexOverlap(rv.g_src, rv.g_tar, outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_overlap.cache");
					log("\t\t\tnode overlap: "+common_vertices.size());
				}
			}
			else if(args[5].equals("sng"))
			{
				for(int j = 0; j < Integer.parseInt(args[6]); j++)
				{
					log("\t\tVariant: v"+Integer.toString(j));
					
					rv = libSNG.perturbate((MyGraph)sg, Integer.parseInt(args[7]), Integer.parseInt(args[8]), Float.parseFloat(args[9]));
					rv.g_src.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_src.tgf");
					rv.g_tar.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_tar.tgf");
					
					log("\t\t\tg_src nodes: "+rv.g_src.vertexSet().size()+", edges: "+rv.g_src.edgeSet().size());
					log("\t\t\tg_tar nodes: "+rv.g_tar.vertexSet().size()+", edges: "+rv.g_tar.edgeSet().size());
					
					List<Integer> common_vertices = cacheVertexOverlap(rv.g_src, rv.g_tar, outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_overlap.cache");
					log("\t\t\tnode overlap: "+common_vertices.size());
				}
			}
			// Copy first
			else if(args[5].equals("copyfirst"))
			{
				for(int j = 1; j < Integer.parseInt(args[6]); j++)
				{
					log("\t\tVariant: v"+Integer.toString(j));
					
					rv = new PerturbData();
					if(args[1].contains("_directed"))
						rv.g_src = new MyDirectedGraph(outdir+"/SimuData/e"+Integer.toString(i)+"_v0_src.tgf");
					else
						rv.g_src = new MyGraph(outdir+"/SimuData/e"+Integer.toString(i)+"_v0_src.tgf");
					if(args[1].contains("_directed"))
						rv.g_tar = new MyDirectedGraph(outdir+"/SimuData/e"+Integer.toString(i)+"_v0_tar.tgf");
					else
						rv.g_tar = new MyGraph(outdir+"/SimuData/e"+Integer.toString(i)+"_v0_tar.tgf");
					
					rv.g_src.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_src.tgf");
					rv.g_tar.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_tar.tgf");
					
					log("\t\t\tg_src nodes: "+rv.g_src.vertexSet().size()+", edges: "+rv.g_src.edgeSet().size());
					log("\t\t\tg_tar nodes: "+rv.g_tar.vertexSet().size()+", edges: "+rv.g_tar.edgeSet().size());
					
					List<Integer> common_vertices = cacheVertexOverlap(rv.g_src, rv.g_tar, outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_overlap.cache");
					log("\t\t\tnode overlap: "+common_vertices.size());
				}
			}
			if(args[5].equals("sample"))
			{
				for(int j = 0; j < Integer.parseInt(args[6]); j++)
				{
					log("\t\tVariant: v"+Integer.toString(j));

					rv = libSPert.perturbate((MyGraph)sg, Float.parseFloat(args[7]), Float.parseFloat(args[8]));
					rv.g_src.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_src.tgf");
					rv.g_tar.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_tar.tgf");

					log("\t\t\tg_src nodes: "+rv.g_src.vertexSet().size()+", edges: "+rv.g_src.edgeSet().size());
					log("\t\t\tg_tar nodes: "+rv.g_tar.vertexSet().size()+", edges: "+rv.g_tar.edgeSet().size());

					List<Integer> common_vertices = cacheVertexOverlap(rv.g_src, rv.g_tar, outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_overlap.cache");
					log("\t\t\tnode overlap: "+common_vertices.size());
				}
			}
			// Cloning
			else
			{
				for(int j = 0; j < Integer.parseInt(args[6]); j++)
				{
					log("\t\tVariant: v"+Integer.toString(j));
					
					rv = new PerturbData();
					rv.g_src = sg.selfcopy();
					rv.g_tar = sg.selfcopy();
					
					rv.g_src.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_src.tgf");
					rv.g_tar.writeToTGF(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_tar.tgf");
					
					log("\t\t\tg_src nodes: "+rv.g_src.vertexSet().size()+", edges: "+rv.g_src.edgeSet().size());
					log("\t\t\tg_tar nodes: "+rv.g_tar.vertexSet().size()+", edges: "+rv.g_tar.edgeSet().size());
					
					List<Integer> common_vertices = cacheVertexOverlap(rv.g_src, rv.g_tar, outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_overlap.cache");
					log("\t\t\tnode overlap: "+common_vertices.size());
				}
			}
		}
	}
	
	// Simulate an algorithm on a dataset
	public static void simulateDeanon(String[] args)
	{
		// Init
		String mark = "", submark = "";
		int[] exclude = null;
		if(args[3].contains("/"))
		{
			mark = args[3].substring(0, args[3].indexOf("/"));
			submark = args[3].substring(args[3].indexOf("/")+1);
			args[3] = mark;
		}
		
		identifier = getIdentifier(args[1], Integer.parseInt(args[2]), args[3]);
		outdir = "./output/"+identifier;
		String simudir = "./output/"+identifier+"/"+args[4];
		if(submark.length() > 0)
			simudir = "./output/"+identifier+"/"+args[4]+"_"+submark;
		new File(simudir).mkdir();
		new File(simudir+"/Results").mkdir();
		new File(simudir+"/Correlations").mkdir();
		new File(simudir+"/Seeds").mkdir();
		new File(simudir+"/Plots").mkdir();
		logfile = simudir+"/simulate.log";
		
		// Start
		log();
		log("@ "+timestamp());
		if(submark.length() > 0)
			log("Simulating on data derived from: "+args[1]+" ["+args[3]+"/"+submark+"]");
		else
			log("Simulating on data derived from: "+args[1]+" ["+args[3]+"]");
		log("\tParameters: algo="+args[4]+", num_rounds="+args[5]+", seed_type="+args[6]+", seed_count="+args[7]);
		if((args[4].equals("ns09") || args[4].equals("dns09") || args[4].equals("blb")) && args.length >= 9 && !args[8].contains("exclude"))
		{
			if(args[4].equals("ns09"))
			{
				libNS09.Theta = Double.parseDouble(args[8]);
				log("\tParameters of "+args[4]+": theta="+libNS09.Theta);
			}
			else if(args[4].equals("dns09"))
			{
				libDNS09.Theta = Double.parseDouble(args[8]);
				log("\tParameters of "+args[4]+": theta="+libDNS09.Theta);
			}
			else if(args[4].equals("blb"))
			{
				libBlb.Theta = Double.parseDouble(args[8].substring(0, args[8].indexOf(",")));
				libBlb.Delta = Double.parseDouble(args[8].substring(args[8].indexOf(",")+1));
				log("\tParameters of "+args[4]+": theta="+ libBlb.Theta+", delta="+ libBlb.Delta);
			}
		}
		if(args[4].equals("grh"))
		{
			libGrh.Theta = Double.parseDouble(args[8]);
			log("\tParameters of "+args[4]+": theta="+libGrh.Theta);
		}
		if(args.length == 10 && args[9].contains("exclude"))
		{
			String arr = args[9].substring(args[9].indexOf('=')+1);
			String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").split(",");
			
			exclude = new int[items.length];

			for (int i = 0; i < items.length; i++) {
			    try {
			        exclude[i] = Integer.parseInt(items[i]);
			    } catch (NumberFormatException nfe) {};
			}
		}

		// Count exports and perturbations
		int export_count = 0;
		int perturb_count = 0;
		for(int i = 0; i<100; i++)
		{
			File f = new File(outdir+"/SimuData/e"+Integer.toString(i)+".tgf");
			if(!f.exists())
				break;
			export_count = i;
			if(export_count == 0)
			{
				for(int j = 0; j<100; j++)
				{
					File f2 = new File(outdir+"/SimuData/e"+Integer.toString(i)+"_v"+Integer.toString(j)+"_src.tgf");
					if(!f2.exists())
						break;
					perturb_count = j;
				}
			}
		}
		export_count++;
		perturb_count++;
		log("\tExports: "+export_count+", perturbed: "+perturb_count);
		
		// Logfiles
		String logpath_runtimes = simudir+"/runtime.csv";
		log_line(logpath_runtimes);
		String dirpath_seeds = simudir+"/Seeds/";
		String logpath_seeds, logpath_seedstat;
		
		// Do the simulations for all data sets
		String simu_id;
		long round_starttime;
		double round_runtime;
		String round_runtimes;
		MyBaseGraph g_src, g_tar;
		GroundTruth gt;
		PropagationData propData;
		for(int export_id = 0; export_id<export_count; export_id++)
			for(int perturb_id = 0; perturb_id <perturb_count; perturb_id ++)
			{
				simu_id = "e"+export_id+"_v"+perturb_id;
				log("Simulating: "+simu_id);
				logpath_seeds = dirpath_seeds+simu_id+".txt";
				log_line(logpath_seeds);
				logpath_seedstat = dirpath_seeds+simu_id+"_stat.csv";
				log_line(logpath_seedstat);
				round_runtimes = simu_id+";";
				
				// Load simulation data
				if(args[1].contains("_directed"))
				{
					g_src = new MyDirectedGraph(outdir+"/SimuData/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src.tgf");
					log("\tg_src nodes: "+g_src.vertexSet().size()+", edges: "+g_src.edgeSet().size()+" (directed)");
					g_tar = new MyDirectedGraph(outdir+"/SimuData/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_tar.tgf");
					log("\tg_tar nodes: "+g_tar.vertexSet().size()+", edges: "+g_tar.edgeSet().size()+" (directed)");
				}
				else
				{
					g_src = new MyGraph(outdir+"/SimuData/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src.tgf");
					log("\tg_src nodes: "+g_src.vertexSet().size()+", edges: "+g_src.edgeSet().size());
					g_tar = new MyGraph(outdir+"/SimuData/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_tar.tgf");
					log("\tg_tar nodes: "+g_tar.vertexSet().size()+", edges: "+g_tar.edgeSet().size());
				}
				
				// Caching common vertices (useful for large networks)
				if(!new File(outdir+"/SimuData/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_overlap.cache").exists())
					cacheVertexOverlap(g_src, g_tar, outdir+"/SimuData/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_overlap.cache");
				gt = new GroundTruth(outdir+"/SimuData/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_overlap.cache", outdir+"/SimuData/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_mapping.cache");
				log("\tnode overlap: "+gt.common_vertices.size());
				if(gt.mappings.size() > 0)
					log("\tmappings: "+gt.mappings.size());
				
				if(exclude != null)
				{
					System.out.println(Arrays.toString(exclude));
					for(int i = 0; i < exclude.length; i++)
					{
						g_tar.removeVertex(new Integer(exclude[i]));
					}
				}

				// Header for the file storing the results
				log_stats(simudir+"/Results/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+".csv", g_src, g_tar, gt);
				
				// Round based simulation
				for(int round_id = 0; round_id < Integer.parseInt(args[5]); round_id++)
				{
					// Timing
					round_starttime = System.nanoTime();
					log("\t#"+round_id);
					
					// Init data: convergence, matches
					propData = new PropagationData();

					// Seeding
					long seed_start_time = System.nanoTime() / 1000000;
					List<Integer> seeds = new ArrayList<Integer>();
					// k-cliques from a selected top percent (new method)
					if(args[6].contains("cliques"))
					{
						int k = Character.getNumericValue(args[6].charAt(0));
						double top_percent = 1.0;
						if(args[6].contains("."))
						{
							String tps = args[6].substring(args[6].indexOf('.'));
							top_percent = Double.parseDouble(tps);
						}
						List<Object> seed_cliques = Seeding.getCliquesFromTop(g_src, g_tar, gt, Integer.parseInt(args[7]), k, top_percent);
						if(seed_cliques.size() > 0)
						{
							log_line(logpath_seeds, seed_cliques.toString());
							if(top_percent < 1.0)
								log("\t    "+seed_cliques.size()+" seed "+k+"-cliques (from top "+((int)(top_percent*100.0))+"%)");
							else
								log("\t    "+seed_cliques.size()+" seed "+k+"-cliques");
							for(Object o : seed_cliques)
							{
								List<Integer> seed = (List<Integer>) o;
								for(Integer v : seed)
								{
									propData.matches.add(v, v);
									seeds.add(v);
								}
							}
						}
					}
					// Nodes crawled with BFS (given top percent of degree)
					else if(args[6].contains("bfs"))
					{
						int bfs_size = Character.getNumericValue(args[6].charAt(0));
						double top_percent = 1.0;
						if(args[6].contains("."))
						{
							String tps = args[6].substring(args[6].indexOf('.'));
							top_percent = Double.parseDouble(tps);
						}
						
						seeds = Seeding.getBFSNodes(g_src, g_tar, gt, Integer.parseInt(args[7]), bfs_size, top_percent);
						if(seeds.size() > 0)
						{
							log_line(logpath_seeds, seeds.toString());
							if(top_percent < 1.0)
								log("\t    "+seeds.size()+" seed BFS nodes (from top "+((int)(top_percent*100.0))+"%)");
							else
								log("\t    "+seeds.size()+" seed BFS nodes");
							for(Integer v : seeds)
								propData.matches.add(v, v);
						}
					}
					// Top nodes
					else if(args[6].contains("top"))
					{
						seeds = Seeding.getTopNodes(g_src, g_tar, gt, Integer.parseInt(args[7]));
						if(seeds.size() > 0)
						{
							log_line(logpath_seeds, seeds.toString());
							log("\t    "+seeds.size()+" seed top nodes");
							for(Integer v : seeds)
								propData.matches.add(v, v);
						}
					}
					// LTA top degree nodes (use this variant only with top_percent < 1.0)
					else if(args[6].contains("lta."))
					{
						// Select interesting nodes
						double top_percent = 1.0;
						String tps = args[6].substring(args[6].indexOf('.'));
						top_percent = Double.parseDouble(tps);

						// Load values
						if(!libLTA.cacheExists(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent, libLTA.LTA_A))
						{
							List<Integer> vertexSet = null;

							vertexSet = new ArrayList<Integer>();

							// Get top-deg nodes
							List<DegIx> degixs = new ArrayList<DegIx>();
							ArrayList<Integer> vs = new ArrayList<Integer>(g_src.vertexSet());
							for(Integer v : vs)
								degixs.add(new DegIx(v, g_src.degreeOf(v)));
							Comparator<DegIx> comparator = new Comparator<DegIx>()
						    {
						        public int compare(DegIx tupleA, DegIx tupleB)
						        {
						            return tupleA.degree.compareTo(tupleB.degree);
						        }
						    };
							Collections.sort(degixs, comparator);
							for(int i = degixs.size()-1; i>=0; i--)
							{
								DegIx dit = degixs.get(i);
								vertexSet.add(dit.index);
								if(vertexSet.size() >= (int)(g_src.vertexSet().size()*top_percent))
									break;
							}
							
							libLTA.writeCache(g_src, outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent, libLTA.LTA_A, vertexSet);
						}
						HashMap<Integer, Double> lta_dict = libLTA.readCache(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent, libLTA.LTA_A);
						
						seeds = Seeding.getDictNodes(g_src, g_tar, gt, Integer.parseInt(args[7]), lta_dict, false);
						if(seeds.size() > 0)
						{
							log_line(logpath_seeds, seeds.toString());
							log("\t    "+seeds.size()+" seed top LTA nodes (from top "+((int)(top_percent*100.0))+"%)");
							for(Integer v : seeds)
								propData.matches.add(v, v);
						}
					}
					// LTA nodes
					else if(args[6].contains("lta"))
					{
						// Load LTA values first
						HashMap<Integer, Double> lta_dict = new HashMap<Integer, Double>();
						if(libLTA.cacheExists(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src", libLTA.LTA_A))
							lta_dict = libLTA.readCache(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src", libLTA.LTA_A);
						else
							lta_dict = libLTA.writeCache(g_src, outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src", libLTA.LTA_A);
						
						seeds = Seeding.getDictNodes(g_src, g_tar, gt, Integer.parseInt(args[7]), lta_dict);
						if(seeds.size() > 0)
						{
							log_line(logpath_seeds, seeds.toString());
							log("\t    "+seeds.size()+" seed bottom LTA nodes");
							for(Integer v : seeds)
								propData.matches.add(v, v);
						}
					}
					// Betweenness centrality
					else if(args[6].contains("betwc"))
					{
						// Select interesting nodes
						double top_percent = 1.0;
						if(args[6].contains("."))
						{
							String tps = args[6].substring(args[6].indexOf('.'));
							top_percent = Double.parseDouble(tps);
						}

						// Load values
						if(!libBCC.betweennessCacheExists(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent))
						{
							List<Integer> vertexSet = null;

							if(top_percent < 1.0)
							{
								vertexSet = new ArrayList<Integer>();

								// Get top-deg nodes
								List<DegIx> degixs = new ArrayList<DegIx>();
								ArrayList<Integer> vs = new ArrayList<Integer>(g_src.vertexSet());
								for(Integer v : vs)
									degixs.add(new DegIx(v, g_src.degreeOf(v)));
								Comparator<DegIx> comparator = new Comparator<DegIx>()
							    {
							        public int compare(DegIx tupleA, DegIx tupleB)
							        {
							            return tupleA.degree.compareTo(tupleB.degree);
							        }
							    };
								Collections.sort(degixs, comparator);
								for(int i = degixs.size()-1; i>=0; i--)
								{
									DegIx dit = degixs.get(i);
									vertexSet.add(dit.index);
									if(vertexSet.size() >= (int)(g_src.vertexSet().size()*top_percent))
										break;
								}
							}
							
							libBCC.writeCaches(g_src, vertexSet, outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent);
						}
						HashMap<Integer, Double> bwc_dict = libBCC.readBetweennessCache(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent);
						
						seeds = Seeding.getDictNodes(g_src, g_tar, gt, Integer.parseInt(args[7]), bwc_dict, true);
						if(seeds.size() > 0)
						{
							log_line(logpath_seeds, seeds.toString());
							log("\t    "+seeds.size()+" seed top betweenness nodes (from top "+((int)(top_percent*100.0))+"%)");
							for(Integer v : seeds)
								propData.matches.add(v, v);
						}
					}
					// Closeness centrality
					else if(args[6].contains("closec"))
					{
						// Select interesting nodes
						double top_percent = 1.0;
						if(args[6].contains("."))
						{
							String tps = args[6].substring(args[6].indexOf('.'));
							top_percent = Double.parseDouble(tps);
						}

						// Load values
						if(!libBCC.closenessCacheExists(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent))
						{
							List<Integer> vertexSet = null;

							if(top_percent < 1.0)
							{
								vertexSet = new ArrayList<Integer>();

								// Get top-deg nodes
								List<DegIx> degixs = new ArrayList<DegIx>();
								ArrayList<Integer> vs = new ArrayList<Integer>(g_src.vertexSet());
								for(Integer v : vs)
									degixs.add(new DegIx(v, g_src.degreeOf(v)));
								Comparator<DegIx> comparator = new Comparator<DegIx>()
							    {
							        public int compare(DegIx tupleA, DegIx tupleB)
							        {
							            return tupleA.degree.compareTo(tupleB.degree);
							        }
							    };
								Collections.sort(degixs, comparator);
								for(int i = degixs.size()-1; i>=0; i--)
								{
									DegIx dit = degixs.get(i);
									vertexSet.add(dit.index);
									if(vertexSet.size() >= (int)(g_src.vertexSet().size()*top_percent))
										break;
								}
							}
							
							libBCC.writeCaches(g_src, vertexSet, outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent);
						}
						HashMap<Integer, Double> clc_dict = libBCC.readClosenessCache(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent);
						
						seeds = Seeding.getDictNodes(g_src, g_tar, gt, Integer.parseInt(args[7]), clc_dict, true);
						if(seeds.size() > 0)
						{
							log_line(logpath_seeds, seeds.toString());
							log("\t    "+seeds.size()+" seed top closeness nodes (from top "+((int)(top_percent*100.0))+"%)");
							for(Integer v : seeds)
								propData.matches.add(v, v);
						}
					}
					// Local clustering coefficient (nodes with high LCC)
					else if(args[6].contains("lcch"))
					{
						// Select interesting nodes
						double top_percent = 1.0;
						if(args[6].contains("."))
						{
							String tps = args[6].substring(args[6].indexOf('.'));
							top_percent = Double.parseDouble(tps);
						}

						// Load values
						if(!libLCC.cacheExists(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent))
						{
							List<Integer> vertexSet = null;

							if(top_percent < 1.0)
							{
								vertexSet = new ArrayList<Integer>();

								// Get top-deg nodes
								List<DegIx> degixs = new ArrayList<DegIx>();
								ArrayList<Integer> vs = new ArrayList<Integer>(g_src.vertexSet());
								for(Integer v : vs)
									degixs.add(new DegIx(v, g_src.degreeOf(v)));
								Comparator<DegIx> comparator = new Comparator<DegIx>()
							    {
							        public int compare(DegIx tupleA, DegIx tupleB)
							        {
							            return tupleA.degree.compareTo(tupleB.degree);
							        }
							    };
								Collections.sort(degixs, comparator);
								for(int i = degixs.size()-1; i>=0; i--)
								{
									DegIx dit = degixs.get(i);
									vertexSet.add(dit.index);
									if(vertexSet.size() >= (int)(g_src.vertexSet().size()*top_percent))
										break;
								}
							}
							
							libLCC.writeCache(g_src, vertexSet, outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent);
						}
						HashMap<Integer, Double> lcc_dict = libLCC.readCache(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent);
						
						seeds = Seeding.getDictNodes(g_src, g_tar, gt, Integer.parseInt(args[7]), lcc_dict, true, 0.2);
						if(seeds.size() > 0)
						{
							log_line(logpath_seeds, seeds.toString());
							log("\t    "+seeds.size()+" seed from top 80% LCC nodes (from top "+((int)(top_percent*100.0))+"% by degree)");
							for(Integer v : seeds)
								propData.matches.add(v, v);
						}
					}
					// Local clustering coefficient
					else if(args[6].contains("lcc"))
					{
						// Select interesting nodes
						double top_percent = 1.0;
						if(args[6].contains("."))
						{
							String tps = args[6].substring(args[6].indexOf('.'));
							top_percent = Double.parseDouble(tps);
						}

						// Load values
						if(!libLCC.cacheExists(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent))
						{
							List<Integer> vertexSet = null;

							if(top_percent < 1.0)
							{
								vertexSet = new ArrayList<Integer>();

								// Get top-deg nodes
								List<DegIx> degixs = new ArrayList<DegIx>();
								ArrayList<Integer> vs = new ArrayList<Integer>(g_src.vertexSet());
								for(Integer v : vs)
									degixs.add(new DegIx(v, g_src.degreeOf(v)));
								Comparator<DegIx> comparator = new Comparator<DegIx>()
							    {
							        public int compare(DegIx tupleA, DegIx tupleB)
							        {
							            return tupleA.degree.compareTo(tupleB.degree);
							        }
							    };
								Collections.sort(degixs, comparator);
								for(int i = degixs.size()-1; i>=0; i--)
								{
									DegIx dit = degixs.get(i);
									vertexSet.add(dit.index);
									if(vertexSet.size() >= (int)(g_src.vertexSet().size()*top_percent))
										break;
								}
							}
							
							libLCC.writeCache(g_src, vertexSet, outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent);
						}
						HashMap<Integer, Double> lcc_dict = libLCC.readCache(outdir+"/Measures/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_src_top="+top_percent);
						
						seeds = Seeding.getDictNodes(g_src, g_tar, gt, Integer.parseInt(args[7]), lcc_dict, true);
						if(seeds.size() > 0)
						{
							log_line(logpath_seeds, seeds.toString());
							log("\t    "+seeds.size()+" seed top LCC nodes (from top "+((int)(top_percent*100.0))+"%)");
							for(Integer v : seeds)
								propData.matches.add(v, v);
						}
					}
					// Random nodes from a selected top percent
					else if(args[6].contains("random"))
					{
						double top_percent = 1.0;
						if(args[6].contains("."))
						{
							String tps = args[6].substring(args[6].indexOf('.'));
							top_percent = Double.parseDouble(tps);
						}
						
						seeds = Seeding.getNodes(g_src, g_tar, gt, Integer.parseInt(args[7]), top_percent);

						if(seeds.size() > 0)
						{
							log_line(logpath_seeds, seeds.toString());
							if(top_percent < 1.0)
								log("\t    "+seeds.size()+" seed nodes (from top "+((int)(top_percent*100.0))+"%)");
							else
								log("\t    "+seeds.size()+" seed nodes");
							for(Integer v : seeds)
								propData.matches.add(v, v);
						}
					}
					
					// Check if we have seeds at all
					if(propData.matches.size() == 0)
					{
						log("\t    Skipping propagation due to the lack of seeds");
						continue;
					}
					else
					{
						long seed_time = System.nanoTime() / 1000000 - seed_start_time;
						DecimalFormat df = new DecimalFormat("########0.00");
						log_line(logpath_seedstat, args[6]+";"+seeds.size()+";"+df.format(seed_time)+";");
					}

					System.out.print("\t    Propagate: [");
					long start_time = System.nanoTime();
					long current_time = 0;
					int num_rounds = 0;
					while (propData.hasConvergence())
					{
						System.out.print("+");

						num_rounds++;

						if (args[4].equals("ns09"))
							libNS09.propagateStep((MyGraph) g_src, (MyGraph) g_tar, propData);
						else if (args[4].equals("sng"))
							libSNG.propagateStep((MyGraph) g_src, (MyGraph) g_tar, propData);
						else if (args[4].equals("dns09"))
							libDNS09.propagateStep((MyDirectedGraph) g_src, (MyDirectedGraph) g_tar, propData);
						else if (args[4].equals("grh"))
						{
							if (num_rounds > libGrh.maxPropagationSteps)
							{
								propData.updateConvergence(false);
								System.out.print("^");
								continue;
							}
							current_time = System.nanoTime();
							if ((current_time - start_time) / 1e9 > libGrh.maxPropagationTime)
							{
								propData.updateConvergence(false);
								System.out.print("^");
								continue;
							}

							libGrh.propagateStep((MyGraph) g_src, (MyGraph) g_tar, propData);
						} else if (args[4].equals("blb"))
							libBlb.propagateStep((MyGraph) g_src, (MyGraph) g_tar, propData);

						// Save more detailed stats if debug mode is on
						if (DEBUG)
						{
							log_matches(simudir + "/Results/e" + Integer.toString(export_id) + "_v" + Integer.toString(perturb_id) + "_(" + round_id + "." + (num_rounds - 1) + ").dat", g_src, gt, propData.matches);
						}
					}
					System.out.print("]\n");

					// Log runtime
					round_runtime = (System.nanoTime() - round_starttime)/1000000000.0;
					round_runtimes += Double.toString(round_runtime)+";";
					log("\t    "+num_rounds+" rounds, "+(new DecimalFormat("#.##").format(round_runtime))+" sec(s) ");

					if(args[4].equals("grh") && num_rounds > libGrh.maxPropagationSteps)
						log("\t      ^ De-anon was forced to stop: too many propagation steps");
					else if(args[4].equals("grh") && (current_time-start_time)/1e9 > libGrh.maxPropagationTime)
						log("\t      ^ De-anon was forced to stop: propagation was running too long");

					// Log round statistics
					log_matches(simudir+"/Results/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+"_("+round_id+").dat", g_src, gt, propData.matches);
					double[] accuracy = log_stats(simudir+"/Results/e"+Integer.toString(export_id)+"_v"+Integer.toString(perturb_id)+".csv", g_src, g_tar, gt, propData.matches);
					log("\t    TP="+(new DecimalFormat("#.##").format(accuracy[2]*100))+"%, FP="+(new DecimalFormat("#.##").format(accuracy[0]*100))+"%, N/A="+(new DecimalFormat("#.##").format(accuracy[1]*100))+"%, FP/TP="+(new DecimalFormat("#.##").format((accuracy[0]/accuracy[2])*100))+"%");
					log("\t    Total error (w/mappings out of GT): "+(new DecimalFormat("#.##").format(accuracy[3]*100))+"%)");
				}
				
				// Log runtimes
				log_line(logpath_runtimes, round_runtimes);
			}
	}
	
	// Analyze simulation results
	public static void analyzeResults(String[] args)
	{
		// Init
		String mark = "", submark = "";
		if(args[3].contains("/"))
		{
			mark = args[3].substring(0, args[3].indexOf("/"));
			submark = args[3].substring(args[3].indexOf("/")+1);
			args[3] = mark;
		}
		
		identifier = getIdentifier(args[1], Integer.parseInt(args[2]), args[3]);
		outdir = "./output/"+identifier;
		String simudir = "./output/"+identifier+"/"+args[4];
		if(submark.length() > 0)
			simudir = "./output/"+identifier+"/"+args[4]+"_"+submark;
		logfile = simudir+"/analyze.log";
		
		// Start
		log();
		log("@ "+timestamp());
		if(submark.length() > 0)
			log("Analyzing results of: "+args[1]+" ["+args[3]+"/"+submark+"]");
		else
			log("Analyzing results of: "+args[1]+" ["+args[3]+"]");
		
		new File(outdir+"/Measures/").mkdir();
		
		// Calculating accuracy -> TP, N/A, FP, FP/TP ratio
		log("Measuring accuracy");
		String path_accuracy = simudir+"/accuracy.csv";
		log_line(path_accuracy);
		boolean breakloop = false;
		double[] overall_accuracy = {0.0, 0.0, 0.0, 0.0};
		int expcnt = 0, pertcnt = 0;
		BufferedReader in;
		for(int ec = 0; ec<100; ec++)
		{
			expcnt = ec; 
			
			for(int pc = 0; pc<100; pc++)
			{
				// Reading scores
				List results = new ArrayList();
				String firstline = "";
				int common_count = 0;
				
				try
				{
					String path = simudir+"/Results/e"+ec+"_v"+pc+".csv";
					File f = new File(path);
					if(!f.exists())
					{
						if(pc == 0)
							breakloop = true;
						break;
					}

					in = new BufferedReader(new FileReader(path));
					while(in.ready())
					{
						String s = in.readLine();
						if(firstline.length() == 0)
						{
							firstline = s;
							continue;
						}
						
						String[] strArray = s.split(";");
						if(common_count == 0)
							common_count = strArray.length;
						int[] intArray = new int[strArray.length];
						for(int i = 0; i < strArray.length; i++)
							intArray[i] = Integer.parseInt(strArray[i]);
						results.add(intArray);
					}
					in.close();
				} catch (Exception e) { e.printStackTrace(); }

				List<Integer> vs = new ArrayList<Integer>();
				String ml = new String(firstline).replace("\r", "").replace("\n", "");
				for(String ix : ml.split(";"))
					if(!ix.equals(""))
						vs.add(Integer.parseInt(ix));

				pertcnt = pc;

				// Load matches (for each run rounds)
				Matches matches[] = new Matches[results.size()];
				for(int run_id = 0; run_id < results.size(); run_id++)
				{
					matches[run_id] = new Matches();

					try
					{
						FileInputStream fis = new FileInputStream(simudir + "/Results/e" + expcnt + "_v" + pertcnt + "_("+run_id+").edat");
						DataInputStream dis = new DataInputStream(fis);
						// Log extended matches
						int len = dis.readInt();
						for(int ctr = 0; ctr < len; ctr++)
						{
							int v_src = dis.readInt();
							int v_tar = dis.readInt();
							matches[run_id].add(v_src, v_tar);
						}
						dis.close();
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}

				// Calculating accuracy
				int[] sum_results = new int[common_count];
				int ctr = 0;
				double[] accuracy = {0.0, 0.0, 0.0, 0.0};
				for(Object o : results)
				{
					int[] result = (int[]) o;
					for(int i = 0; i<result.length; i++)
					{
						sum_results[i] += result[i];
						
						if(result[i] == 1)
							accuracy[2] += 1.0;
						else if(result[i] == 0)
							accuracy[1] += 1.0;
						else
							accuracy[0] += 1.0;
					}

					// Mappings that should not even exist:
					List<Integer> outofgt = new ArrayList<Integer>(matches[ctr].forward.keySet());
					outofgt.removeAll(vs);

					accuracy[3] = accuracy[0] + outofgt.size();

					ctr++;
				}
				
				// Calculate avg. accuracy for the variant (on current perturbations)
				accuracy[0] = accuracy[0] / (common_count*results.size()*1.0);
				accuracy[1] = accuracy[1] / (common_count*results.size()*1.0);
				accuracy[2] = accuracy[2] / (common_count*results.size()*1.0);
				accuracy[3] = accuracy[3] / (common_count*results.size()*1.0);
				log_line(path_accuracy, "e"+ec+"_v"+pc+";"+accuracy[2]*100+";"+accuracy[1]*100+";"+accuracy[0]*100+";"+accuracy[3]*100);
				log("\te"+ec+"_v"+pc+": TP="+(new DecimalFormat("#.##").format(accuracy[2]*100))+"%, FP="+(new DecimalFormat("#.##").format(accuracy[0]*100))+"%, TFP="+(new DecimalFormat("#.##").format(accuracy[3]*100))+"%, N/A="+(new DecimalFormat("#.##").format(accuracy[1]*100))+"%, FP/TP="+(new DecimalFormat("#.##").format((accuracy[0]/accuracy[2])*100))+"%");
				
				// Measuring overall accuracy (over all perturbations)
				overall_accuracy[0] += accuracy[0];
				overall_accuracy[1] += accuracy[1];
				overall_accuracy[2] += accuracy[2];
				overall_accuracy[3] += accuracy[3];
				
				// Log the sum of the scores for each node
				String path2 = simudir+"/Results/e"+ec+"_v"+pc+"_sum.csv";
				log_line(path2);
				log_line(path2, firstline);
				String sum_result_line = "";
				for(int i = 0; i<sum_results.length; i++)
					sum_result_line += sum_results[i]+";";
				log_line(path2, sum_result_line);
			}
			
			if(breakloop)
				break;
		}
		pertcnt++; // <- needed for estimating the exact number of export and variants
		overall_accuracy[0] = overall_accuracy[0]/(expcnt*pertcnt);
		overall_accuracy[1] = overall_accuracy[1]/(expcnt*pertcnt);
		overall_accuracy[2] = overall_accuracy[2]/(expcnt*pertcnt);
		overall_accuracy[3] = overall_accuracy[3]/(expcnt*pertcnt);
		log("\tAvg. accuracy: TP="+(new DecimalFormat("#.##").format(overall_accuracy[2]*100))+"%, FP="+(new DecimalFormat("#.##").format(overall_accuracy[0]*100))+"%, TFP="+(new DecimalFormat("#.##").format(overall_accuracy[3]*100))+"%, N/A="+(new DecimalFormat("#.##").format(overall_accuracy[1]*100))+"%, FP/TP="+(new DecimalFormat("#.##").format((overall_accuracy[0]/overall_accuracy[2])*100))+"%");
		log("\t  TP =\t"+overall_accuracy[2]*100);
		log("\t  FP =\t"+overall_accuracy[0]*100);
		log("\t  TFP =\t"+overall_accuracy[3]*100);
		log("\t  N/A=\t"+overall_accuracy[1]*100);
		log("\t  F/T=\t"+(overall_accuracy[0]/overall_accuracy[2])*100);
		log_line(path_accuracy, "avg;"+overall_accuracy[2]*100+";"+overall_accuracy[1]*100+";"+overall_accuracy[0]*100+";"+overall_accuracy[3]*100);

		log("Measuring runtime");
		// Reading runtimes
		double total_runtime = 0.0;
		try
		{
			String path = simudir+"/runtime.csv";
			in = new BufferedReader(new FileReader(path));
			while(in.ready())
			{
				String s = in.readLine();
			
				String[] strArray = s.split(";");
				double sum_runtime = 0.0;
				for(int i = 1; i<strArray.length; i++)
					sum_runtime += Double.parseDouble(strArray[i]);
				log("\t"+strArray[0]+": "+(new DecimalFormat("#.##").format(sum_runtime))+" sec(s), avg: "+(new DecimalFormat("#.##").format(sum_runtime / (strArray.length-1)))+"sec(s)");
				total_runtime += sum_runtime;
			}
			in.close();
		} catch (Exception e) { e.printStackTrace(); }
		log("\tTotal: "+(new DecimalFormat("#.##").format(total_runtime))+" sec(s)");
		
		// Checking LTA
		if(args.length < 6 || (args.length == 6 && !args[5].equals("no_lta")))
		{
			log("Measuring LTAs");
			if(!args[1].contains("_directed"))
			{
				breakloop = false;
				for(int ec = 0; ec<100; ec++)
				{
					for(int pc = 0; pc<100; pc++)
					{
						File src_tgf = new File(outdir+"/SimuData/e"+ec+"_v"+pc+"_src.tgf");
						if(!src_tgf.exists())
						{
							if(pc == 0)
								breakloop = true;
							break;
						}
						
						if(!libDEG.cacheExists(outdir+"/Measures/e"+ec+"_v"+pc+"_src"))
						{
							log("\te"+ec+"_v"+pc+"_src.deg");
							MyGraph g_src = new MyGraph(outdir+"/SimuData/e"+ec+"_v"+pc+"_src.tgf");
							libDEG.writeCache(g_src, outdir+"/Measures/e"+ec+"_v"+pc+"_src");
						}
						if(!libLTA.cacheExists(outdir+"/Measures/e"+ec+"_v"+pc+"_src", libLTA.LTA_D))
						{
							log("\te"+ec+"_v"+pc+"_src.lta");
							MyGraph g_src = new MyGraph(outdir+"/SimuData/e"+ec+"_v"+pc+"_src.tgf");
							libLTA.writeCache(g_src, outdir+"/Measures/e"+ec+"_v"+pc+"_src", libLTA.LTA_D);
						}
						if(!libLTA.cacheExists(outdir+"/Measures/e"+ec+"_v"+pc+"_src", libLTA.LTA_A))
						{
							log("\te"+ec+"_v"+pc+"_src.lta");
							MyGraph g_src = new MyGraph(outdir+"/SimuData/e"+ec+"_v"+pc+"_src.tgf");
							libLTA.writeCaches(g_src, outdir+"/Measures/e"+ec+"_v"+pc+"_src");
						}
					}
					
					if(breakloop)
						break;
				}
			}
			else
				log("\tNot (yet) supported for directed graphs!");
			
			log("Measuring correlation of (LTA, sum_score)");
			if(!args[1].contains("_directed"))
			{
				String path_correlations = simudir+"/correlations.csv";
				log_line(path_correlations);
				breakloop = false;
				double[] avg_ltas = new double[libLTA.NUM_LTA_VARS+1]; // +1: for deg based correlation
				for(int ec = 0; ec<100; ec++)
				{
					for(int pc = 0; pc<100; pc++)
					{
						File score_file = new File(simudir+"/Results/e"+ec+"_v"+pc+"_sum.csv");
						if(!score_file.exists())
						{
							if(pc == 0)
								breakloop = true;
							break;
						}
						
						log("\te"+ec+"_v"+pc+"_src");
						String line = "e"+ec+"_v"+pc+";";
		
						// Read scores
						HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
						try
						{
							in = new BufferedReader(new FileReader(simudir+"/Results/e"+ec+"_v"+pc+"_sum.csv"));
							String[] header = in.readLine().split(";");
							String[] values = in.readLine().split(";");
							for(int i = 0; i<header.length; i++)
								scores.put(Integer.parseInt(header[i]), Double.parseDouble(values[i]));
							in.close();
						} catch (Exception e) { e.printStackTrace(); }
						
						// Read LTAs and check for correlation
						for(int i = 0; i<libLTA.NUM_LTA_VARS+1; i++)
						{
							HashMap<Integer, Double> lta = new HashMap<Integer, Double>();
							if(i == libLTA.NUM_LTA_VARS)
								lta = libDEG.readCacheAsDoubleDict(outdir+"/Measures/e"+ec+"_v"+pc+"_src");
							else
								lta = libLTA.readCache(outdir+"/Measures/e"+ec+"_v"+pc+"_src", i);
							
							double[] score_vec = new double[scores.size()];
							double[] lta_vec = new double[scores.size()];
							
							// Get scores-LTA pairs + write it to disk + charting
							String ch = "deg";
							if(i < libLTA.NUM_LTA_VARS)
								ch = "lta_v"+String.valueOf(libLTA.LTA_VAR_ID[i]);
							String path_correlations_file = simudir+"/Correlations/e"+ec+"_v"+pc+"_src_"+ch+".csv";
							log_line(path_correlations_file);
							int j = 0;
							for(Integer v : scores.keySet())
							{
								if(lta.get(v) == null)
									System.out.println("Missing LTA: "+v);
								else
								{
									score_vec[j] = scores.get(v);
									lta_vec[j] = lta.get(v);
		
									log_line(path_correlations_file, v+";"+Math.round(score_vec[j])+";"+lta_vec[j]);
		
									j++;
								}
							}
		
							// Measure Pearson Correlation // not used anymore
							//double corr = MyMath.getPearsonCorrelation(score_vec, lta_vec);
							// Measure Spearman Rankcorrelation
							SpearmansCorrelation spc = new SpearmansCorrelation();
							double corr = spc.correlation(score_vec, lta_vec);
							avg_ltas[i] += corr;
							line += corr+";";
							
						}
						log_line(path_correlations, line);
					}
					
					if(breakloop)
						break;
				}
			
				// Calculate average correlation
				String line = "avg;";
				for(int i = 0; i<libLTA.NUM_LTA_VARS+1; i++)
					line += avg_ltas[i] / (expcnt*pertcnt)+";";
				log_line(path_correlations, line);
			}
			else
				log("\tNot (yet) supported for directed graphs!");
		}
		else
			log("LTA measurement skipped!");
	}
	
	// Measure a network
	public static void measureNetwork(String[] args)
	{
		int min_deg = -1;
		outdir = "./output/";
		if(args.length == 3)
			logfile = outdir+"measure_"+args[1]+"_"+args[2]+".log";
		else
		{
			logfile = outdir+"measure_"+args[1]+"_"+args[2]+"_top="+Double.parseDouble(args[3])+".log";
		}
		String cachefile = logfile.replace(".log", "");

		// Start
		log();
		log("@ "+timestamp());
		if(args.length == 3)
			log("Measuring: "+args[1]+" with "+args[2]);
		else
		{
			log("Measuring: "+args[1]+" with "+args[2]+" ("+(Double.parseDouble(args[3])*100)+"% only)");
		}
		MyGraph g = new MyGraph("./data/"+args[1]+".tgf");
		
		// Measuring betweenness or closeness centrality
		if(args[2].contains("betwc") || args[2].contains("closec"))
		{
			// Select interesting nodes
			double top_percent = 1.0;
			if(args.length == 4)
				top_percent = Double.parseDouble(args[3]);

			// Load values
			if(!libBCC.betweennessCacheExists(cachefile) || !libBCC.closenessCacheExists(cachefile))
			{
				log("\tCalculating betweenness and closeness centrality");
				List<Integer> vertexSet = null;

				if(top_percent < 1.0)
				{
					vertexSet = new ArrayList<Integer>();

					// Get top-deg nodes
					List<DegIx> degixs = new ArrayList<DegIx>();
					ArrayList<Integer> vs = new ArrayList<Integer>(g.vertexSet());
					for(Integer v : vs)
						degixs.add(new DegIx(v, g.degreeOf(v)));
					Comparator<DegIx> comparator = new Comparator<DegIx>()
				    {
				        public int compare(DegIx tupleA, DegIx tupleB)
				        {
				            return tupleA.degree.compareTo(tupleB.degree);
				        }
				    };
					Collections.sort(degixs, comparator);
					for(int i = degixs.size()-1; i>=0; i--)
					{
						DegIx dit = degixs.get(i);
						vertexSet.add(dit.index);
						if(vertexSet.size() >= (int)(g.vertexSet().size()*top_percent))
							break;
					}
				}
				
				libBCC.writeCaches(g, vertexSet, cachefile);
				log("\tCaches are saved!");
			}
			else
				log("\tCaches already exists!");
		}
		else if(args[2].contains("lta"))
		{
			log("\tCalculating LTA_A values");
			
			// Check for LTA cache first
			HashMap<Integer, Double> lta_dict = new HashMap<Integer, Double>();
			if(libLTA.cacheExists(cachefile, libLTA.LTA_A))
				log("\tCaches already exists!");
			else
			{
				lta_dict = libLTA.writeCache(g, cachefile, libLTA.LTA_A);
				log("\tLTA values are saved!");
			}
		}
		else if(args[2].contains("deg"))
		{
			log("\tCalculating degree values");
			
			// Check for deg value cache first
			HashMap<Integer, Integer> dict = new HashMap<Integer, Integer>();
			if(libDEG.cacheExists(cachefile))
				log("\tCaches already exists!");
			else
			{
				dict = libDEG.writeCache(g, cachefile);
				log("\tDeg values are saved!");
			}
		}
		else if(args[2].contains("lcc"))
		{
			// Select interesting nodes
			double top_percent = 1.0;
			if(args.length == 4)
				top_percent = Double.parseDouble(args[3]);

			// Load values
			if(!libLCC.cacheExists(cachefile))
			{
				log("\tCalculating LCC values");
				List<Integer> vertexSet = null;

				if(top_percent < 1.0)
				{
					vertexSet = new ArrayList<Integer>();

					// Get top-deg nodes
					List<DegIx> degixs = new ArrayList<DegIx>();
					ArrayList<Integer> vs = new ArrayList<Integer>(g.vertexSet());
					for(Integer v : vs)
						degixs.add(new DegIx(v, g.degreeOf(v)));
					Comparator<DegIx> comparator = new Comparator<DegIx>()
				    {
				        public int compare(DegIx tupleA, DegIx tupleB)
				        {
				            return tupleA.degree.compareTo(tupleB.degree);
				        }
				    };
					Collections.sort(degixs, comparator);
					for(int i = degixs.size()-1; i>=0; i--)
					{
						DegIx dit = degixs.get(i);
						vertexSet.add(dit.index);
						if(vertexSet.size() >= (int)(g.vertexSet().size()*top_percent))
							break;
					}
				}
				
				libLCC.writeCache(g, vertexSet, cachefile);
				log("\tCache is now saved!");
			}
			else
				log("\tCaches already exists!");
		}
	}
}
