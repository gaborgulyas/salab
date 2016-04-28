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

// SimpleDirectedGraph<Integer, MyEdge>
public class MyDirectedGraph extends SimpleDirectedGraph implements MyBaseGraph
{
	/*
	 * Constructors
	 * */
	public MyDirectedGraph()
	{
		super(MyEdge.class);
	}
	
	public MyDirectedGraph(String path)
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
		return this.inDegreeOf(v)+this.outDegreeOf(v);
	}
	
	// Get in-neighbors of a node
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
	
	// Get in-neighbors of a node
	public SortedSet<Integer> inNeighborsOf(Integer v)
	{
		Set<MyEdge> es = this.incomingEdgesOf(v);
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
	
	// Get out-neighbors of a node
	public SortedSet<Integer> outNeighborsOf(Integer v)
	{
		Set<MyEdge> es = this.outgoingEdgesOf(v);
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
	
	// Get largest (undirected) connected component
	public Set<Integer> getLargestConnectedComponent()
	{
		Set<Integer> largestConnectedSet = new TreeSet();
		Set<Integer> connectedSet;
		List<Integer> vs = new ArrayList(vertexSet());
		
		while(vs.size() > 0)
		{
			connectedSet = new TreeSet();
			
			Queue<Integer> queue = new LinkedList();
			Integer v0 = vs.get(0);
			queue.offer(v0);
			connectedSet.add(v0);
			while(queue.size() > 0)
			{
				Integer v1 = queue.poll();
				Set<Integer> nbrs = this.neighborsOf(v1);
				for(Integer v2 : nbrs)
				{
					if(connectedSet.contains(v2))
						continue;
					connectedSet.add(v2);
					queue.offer(v2);
				}
			}
			
			if(connectedSet.size() > largestConnectedSet.size())
				largestConnectedSet  = new TreeSet(connectedSet);
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
					if(this.vertexSet().size() >= 1100000)
						break;
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
	public MyDirectedGraph export(int export_size)
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
		MyDirectedGraph sg = new MyDirectedGraph();
		queue.offer(start_v);
		sg.addVertex(start_v);
		
		// Extract nodes
		while(queue.size() > 0 && export_size > 0)
		{
			Integer v1 = queue.poll();
			Set<Integer> nbrs = this.neighborsOf(v1);
			for(Integer v2 : nbrs)
			{
				if(!sg.containsVertex(v2))
				{
					sg.addVertex(v2);
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
			// In edges
	        Set<MyEdge> in_nbrs = this.incomingEdgesOf(v1);
			for(MyEdge e : in_nbrs)
			{
				if(sg.containsVertex(e.source()) && !v1.equals(e.source()) && !sg.containsEdge(e.source(), v1))
					sg.addEdge(e.source(), v1);
			}
	        
	        // Out edges
	        Set<MyEdge> out_nbrs = this.outgoingEdgesOf(v1);
	        for(MyEdge e : out_nbrs)
			{
				if(sg.containsVertex(e.target()) && !v1.equals(e.target()) && !sg.containsEdge(v1, e.target()))
					sg.addEdge(v1, e.target());
			}
	    }
		
		return sg;
	}
	
	// Copy self
	public MyDirectedGraph selfcopy()
	{
		return (MyDirectedGraph) clone();
	}
	
	private static final long serialVersionUID = 1L;
}
