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
package deanon.structures.tuples;

public class DictTuple
{
	public Integer index;
	public Double value;
	
	public DictTuple(Integer v, Double val)
	{
		index = new Integer(v);
		value = new Double(val);
	}
	
}
