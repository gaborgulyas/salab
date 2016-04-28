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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mygraph.MyBaseGraph;

import org.apache.commons.collections.map.MultiValueMap;

public class PerturbData
{
	public MyBaseGraph g_src;
	public MyBaseGraph g_tar;
	public List<Integer> common_vertices;
	public MultiValueMap mapping, all_mapping;
	public HashMap<Integer, Integer> model_choices;
	public HashMap<Integer, Double> lta_dict;
	
	public PerturbData()
	{
		super();
	}
	
	public PerturbData(MyBaseGraph src, MyBaseGraph tar, MultiValueMap mvm, MultiValueMap all_mvm)
	{
		super();
		g_src = src;
		g_tar = tar;
		mapping = mvm;
		all_mapping = all_mvm;
		common_vertices = new ArrayList<Integer>();
		model_choices = new HashMap<Integer, Integer>();
		lta_dict = new HashMap<Integer, Double>();
	}
	
	public PerturbData(MyBaseGraph src, MyBaseGraph tar, List<Integer> cvs, MultiValueMap mvm, MultiValueMap all_mvm)
	{
		super();
		g_src = src;
		g_tar = tar;
		common_vertices = cvs;
		mapping = mvm;
		all_mapping = all_mvm;
		model_choices = new HashMap<Integer, Integer>();
		lta_dict = new HashMap<Integer, Double>();
	}
}
