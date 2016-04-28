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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MyMath
{
	// Calculate Pearson Correlation
    public static double getPearsonCorrelation(double[] scores1, double[] scores2)
    {
        double result = 0;
        double sum_sq_x = 0;
        double sum_sq_y = 0;
        double sum_coproduct = 0;
        double mean_x = scores1[0];
        double mean_y = scores2[0];
        for(int i=2; i<scores1.length+1; i+=1)
        {
            double sweep = Double.valueOf(i-1)/i;
            double delta_x = scores1[i-1]-mean_x;
            double delta_y = scores2[i-1]-mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        double pop_sd_x = (double) Math.sqrt(sum_sq_x/scores1.length);
        double pop_sd_y = (double) Math.sqrt(sum_sq_y/scores1.length);
        double cov_x_y = sum_coproduct / scores1.length;
        result = cov_x_y / (pop_sd_x*pop_sd_y);
        return result;
    }
    
	// Standard deviation
	public static double standardDeviation(List<Double> items)
	{
		double mean = 0.0;
		for(Double d : items)
			mean += d;
		mean = mean / items.size();
 
		double sdsum = 0.0;
		for(Double d : items)
			sdsum += Math.pow((d - mean), 2);
		return Math.sqrt(sdsum / (items.size()-1));
	}

	// MD5 calculation
	public static String md5_digest(String input)
	{
		MessageDigest md5;
		String retval = new String("");
		byte[] digest_ba;
		
		try
		{
			md5 = MessageDigest.getInstance("MD5");
			md5.reset();
			md5.update(input.getBytes());
			digest_ba = md5.digest();
			for(int i = 0; i<digest_ba.length; i++)
				retval = retval.concat(Integer.toHexString(0xFF & digest_ba[i]));
		} catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
		
		return retval;
	}

}
