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

import java.util.*;

public class Matches
{
	  public Map<Integer, Integer> forward;
	  public Map<Integer, Integer> reverse;

	  public Matches()
	  {
		  forward = new Hashtable<Integer, Integer>();
		  reverse = new Hashtable<Integer, Integer>();
	  }
	  
	  public void add(Integer v1, Integer v2)
	  {
		  // Return if existing match
		  if(forward.containsKey(v1) && forward.get(v1).equals(v2) && reverse.containsKey(v2) && reverse.get(v2).equals(v1))
			  return;
		  
		  // Remove existing hits in order to avoid duplicates
		  if(reverse.containsKey(v2))
			  forward.remove(reverse.get(v2));
		  if(forward.containsKey(v1))
			  reverse.remove(forward.get(v1));
		  
		  // Register matching
		  forward.put(v1, v2);
		  reverse.put(v2, v1);
	  }

	  public Integer get(Integer key)
	  {
		  return forward.get(key);
	  }

	  public Integer getReverse(Integer key)
	  {
	    return reverse.get(key);
	  }
	  
	  public boolean isMapped(Integer key)
	  {
		  return forward.containsKey(key);
	  }
	  
	  public boolean isReverseMapped(Integer value)
	  {
		  return reverse.containsKey(value);
	  }
	  
	  public int size()
	  {
		  return forward.size();
	  }

}
