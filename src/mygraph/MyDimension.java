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

public class MyDimension
{
	public int col, row;
	
	public MyDimension()
	{
		col = 0;
		row = 0;
	}
	
	public MyDimension(int r, int c)
	{
		col = c;
		row = r;
	}
}
