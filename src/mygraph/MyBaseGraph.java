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

import java.util.Set;
import java.util.SortedSet;
import org.jgrapht.Graph;

public interface MyBaseGraph extends Graph
{
	public SortedSet<Integer> neighborsOf(Integer v);
	
	public Set<Integer> getLargestConnectedComponent();
	
	public void retainLargestConnectedComponent();
	
	public void loadFromTGF(String path);
	
	public void writeToTGF(String path);
	
	public MyBaseGraph export(int export_size);
	
	public MyBaseGraph selfcopy();
	
	public int degreeOf(Integer v);
}
