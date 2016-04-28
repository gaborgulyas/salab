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

import java.io.*;
import java.util.*;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.BreadthFirstIterator;

import deanon.Deanon;

// SimpleGraph<Integer, MyEdge>
public class MyGraph extends SimpleGraph implements MyBaseGraph
{
	/*
	 * Constructors
	 * */
	public MyGraph()
	{
		super(MyEdge.class);
	}
	
	public MyGraph(String path)
	{
		super(MyEdge.class);
		this.loadFromTGF(path);
	}
	
	/*
	 * Basic functions
	 * */
	// Get degree of a node
	public int degreeOf(Integer v)
	{
		return super.degreeOf(v);
	}
	
	// Get neighbors of a node
	public SortedSet<Integer> neighborsOf(Integer v)
	{
		Set<MyEdge> es = this.edgesOf(v);
		SortedSet<Integer> vs  = new TreeSet<Integer>();
	    for(MyEdge e : es)
	    {
	    	if(!e.target().equals(v))
	    		vs.add(e.target());
	    	else if(!e.source().equals(v))
	    		vs.add(e.source());
	    }
	    
	    return vs;
	}
	
	// Get largest connected component
	public Set<Integer> getLargestConnectedComponent()
	{
		Set<Integer> largestConnectedSet = new TreeSet();
		Set<Integer> connectedSet;
		List<Integer> vs = new ArrayList(vertexSet());
		
		while(vs.size() > 0)
		{
			connectedSet = new TreeSet();
			BreadthFirstIterator<Integer, MyEdge> bfsi = new BreadthFirstIterator<Integer, MyEdge>(this, vs.get(0));
			while(bfsi.hasNext())
				connectedSet.add(bfsi.next());
			if(connectedSet.size() > largestConnectedSet.size())
				largestConnectedSet = connectedSet;
			vs.removeAll(connectedSet);
		}
		
		return largestConnectedSet;
	}
	
	// Retain only nodes in LCC
	public void retainLargestConnectedComponent()
	{
		Set<Integer> lcc = getLargestConnectedComponent();
		Set<Integer> vs = new TreeSet(vertexSet());
		for(Integer v : vs)
			if(!lcc.contains(v))
				removeVertex(v);
	}

	/*
	 * IO functions
	 * */
	// Read from file
	public void loadFromTGF(String path)
	{
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(path));
			char[] buf = new char[102400];
	        int numRead=0;
	        String lineBuffer = new String("");
	        
			int last_sec = 0;
			while((numRead=in.read(buf)) != -1)
			{
				lineBuffer = lineBuffer + (String.valueOf(buf, 0, numRead));
	            buf = new char[102400];
	            
	            lineBuffer.replace("\r", "");
	            String[] lines = lineBuffer.substring(0, lineBuffer.lastIndexOf("\n")).split("\n");
	            lineBuffer = lineBuffer.substring(lineBuffer.lastIndexOf("\n")+1);
	            
	            for(String s : lines)
	            {
					if(s.charAt(0) == '#')
						continue;
					String[] vs = s.split("\\s+");
					Integer v1 = Integer.parseInt(vs[0]);
					Integer v2 = Integer.parseInt(vs[1]);
					this.addVertex(v1);
					if(v1.equals(v2))
						continue;
					this.addVertex(v2);
					this.addEdge(v1, v2);
					
					// Limit: at most 1.1M nodes
					if(this.vertexSet().size() >= 600000)
						break;
					
					if(Deanon.DEBUG)
					{
						Calendar cal = Calendar.getInstance();
						if(cal.get(Calendar.SECOND) % 5 == 0 && cal.get(Calendar.SECOND) != last_sec)
						{
							System.out.println("\t"+this.vertexSet().size()+" nodes, "+this.edgeSet().size()+" edges");
							last_sec = cal.get(Calendar.SECOND);
						}
					}
	            }
			}
			in.close();
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	// Write to file
	public void writeToTGF(String path)
	{
		BufferedWriter out;
		try
		{
			out = new BufferedWriter(new FileWriter(path));
			Set<MyEdge> es = this.edgeSet();
			for(MyEdge e : es)
				out.write(e.source().toString()+" "+e.target().toString()+"\n");
			out.close();
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	// Export subnetwork
	public MyGraph export(int export_size)
	{
		// Get starting node
		Set<Integer> vs = this.vertexSet();
		Integer start_v = 0;
		
		int size = vs.size();
		int item = new Random().nextInt(size);
		int i = 0;
		for(Integer v : vs)
		{
		    if (i == item)
		    	start_v = v;
		    i = i + 1;
		}

		Queue<Integer> queue = new LinkedList();
		MyGraph sg = new MyGraph();
		Map<Integer, Object> all_nbrs = new HashMap<Integer, Object>();
		queue.offer(start_v);
		sg.addVertex(start_v);
		all_nbrs.put(start_v, this.neighborsOf(start_v));
		
		// Extract nodes
		while(queue.size() > 0 && export_size > 0)
		{
			Integer v1 = queue.poll();
			Set<Integer> nbrs = this.neighborsOf(v1);
			for(Integer v2 : nbrs)
			{
				if(!sg.vertexSet().contains(v2))
				{
					sg.addVertex(v2);
					all_nbrs.put(v2, this.neighborsOf(v2));
					queue.offer(v2);
					export_size--;
				}
				
				if(export_size < 0)
				{
					queue.clear();
					break;
				}
			}
		}
		
		// Extract edges
		Set<Integer> sg_vs = sg.vertexSet();
		for(Integer v1 : sg_vs)
	    {
	        Set<Integer> nbrs = (Set<Integer>)all_nbrs.get(v1);
	        for(Integer v2 : nbrs)
			{
				if(sg.vertexSet().contains(v2) && !v1.equals(v2))// && sg.getEdge(v1, v2) == null && sg.getEdge(v2, v1) == null)
					sg.addEdge(v1, v2);
			}
	    }
		
		return sg;
	}
	
	// Copy self
	public MyGraph selfcopy()
	{
		return (MyGraph) clone();
	}
	
	private static final long serialVersionUID = 1L;
}
