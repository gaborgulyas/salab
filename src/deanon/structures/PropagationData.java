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
package deanon.structures;


import java.util.SortedSet;
import java.util.TreeSet;


public class PropagationData
{
	// Generic
	private boolean convergence;
	public Matches matches;

	// SNG specific stuff
	public SortedSet<String> pastCandidates;
	public double[][] dissims_src, dissims_tar;
	
	// Grh specific stuff
	public long last_match_size = 0;
	
	public PropagationData()
	{
		convergence = true;
		matches = new Matches();
		pastCandidates = new TreeSet<String>();
	}
	
	public boolean hasConvergence()
	{
		return convergence;
	}
	
	public void updateConvergence(boolean v)
	{
		convergence = v;
	}
}
