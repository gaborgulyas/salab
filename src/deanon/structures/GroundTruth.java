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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.map.MultiValueMap;

import deanon.Deanon;

public class GroundTruth
{
	public List<Integer> common_vertices;
	public MultiValueMap mappings;
	
	public GroundTruth(String cv_path, String mvm_path)
	{
		File fcv = new File(cv_path);
		if(fcv.exists())
			common_vertices = Deanon.readIntListFile(cv_path);
		else
			common_vertices = new ArrayList<Integer>();
		File fmvm = new File(mvm_path);
		if(fmvm.exists())
			mappings = Deanon.readMultiValueMapFile(mvm_path);
		else
			mappings = new MultiValueMap();
	}
	
	public int getScore(Integer v1, Integer v2)
	{
		if(common_vertices.contains(v1))
			if(v1.equals(v2))
				return +1;
			else
				return -1;
		else if(mappings.keySet().contains(v1))
		{
			List<Integer> mapped_to = (ArrayList<Integer>)mappings.getCollection(v1);
			if(mapped_to.contains(v2))
				return +1;
			else
				return -1;
		}
		
		return 0;
	}
}
