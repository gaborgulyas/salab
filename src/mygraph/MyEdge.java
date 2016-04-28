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

import org.jgrapht.graph.DefaultEdge;

public class MyEdge extends DefaultEdge
{
	public Integer source()
	{
		return (Integer)this.getSource();
	}
	
	public Integer target()
	{
		return (Integer)this.getTarget();
	}
}
