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
import java.io.*;
import java.util.*;

import analysis.libBCC;

import deanon.Deanon;

import mygraph.*;

public class SAL
{
	public static String version = "1.0.0";

	public static void printHelp()
	{
		System.out.println();
		System.out.println("  SALab "+version+" (Structural Anonymity Lab framework)");
		System.out.println("  ===========");
		System.out.println("  Copyright (c) 2013-2016 Gabor Gulyas -- licenced under GNU GPLv3 (see licence.txt)");
		System.out.println("  For possible updates and more information see: https://github.com/gaborgulyas/salab");
		System.out.println();
		System.out.println("  Functions and parametering:");
		System.out.println("   Create data: %app% create_data NETWORK EXP_SIZE MARK EXP_CNT PERT_ALGO PERT_CNT PERT_PM1 PERT_PM2 ...");
		System.out.println("     e.g.: %app% create_data epinions 10000 \"test\" 5 clone 5");
		System.out.println("     e.g.: %app% create_data epinions 10000 \"test\" 5 copyfirst 4");
		System.out.println("     e.g.: %app% create_data epinions 10000 \"test\" 5 ns09 5 0.5 0.75  # alpha_v alpha_e");
		System.out.println("     e.g.: %app% create_data epinions 10000 \"test\" 5 sample 5 1.0 0.6  # sampling probability for nodes and edges");
		System.out.println("     e.g.: %app% create_data epinions 10000 \"test\" 5 sng 5 100 25 0.01  # v_overlap v_add pert_rate");
		System.out.println("   Simulation: %app% simulate NETWORK EXP_SIZE MARK(S) SIMU_ALGO NUM_ROUNDS SEED_TYPE SEED_CNT [PARAMS]");
		System.out.println("     e.g.: %app% simulate epinions 10000 \"test/blb_random.01\" blb 1 random.01 10");
		System.out.println("     e.g.: %app% simulate epinions 10000 \"test/blb_random.01\" blb 1 random.01 10 0.1,0.5 # with theta,delta");
		System.out.println("     e.g.: %app% simulate epinions 10000 \"test\" ns09 1 top 100");
		System.out.println("     e.g.: %app% simulate epinions 10000 \"test/s=10\" ns09 1 cliques 10");
		System.out.println("     e.g.: %app% simulate epinions 10000 \"test/s=10\" ns09 1 cliques 10 1.0 # with theta");
		System.out.println("   Analysis: %app% analyze NETWORK EXP_SIZE MARK(S) SIMU_ALGO");
		System.out.println("     e.g.: %app% analyze epinions 10000 \"test\" ns09");
		System.out.println("     e.g.: %app% analyze epinions 10000 \"test\" ns09 no_lta");
		System.out.println("     e.g.: %app% analyze epinions 10000 \"test/s=10\" ns09");
		System.out.println("   Export: %app% export SRC_NET TAR_NET EXP_SIZE");
		System.out.println("     e.g.: %app% export livejournal lj_100k 100000");
		System.out.println("     e.g.: %app% export livejournal lj_100k 100000 directed");
		System.out.println("   Measure: %app% measure SRC_NET MEASURE [TOP_PERCENT]");
		System.out.println("   Summarize: %app% summarize SRC_NET");
		System.out.println();
		System.exit(0);
	}

	/**
	 * StructuralAnonymityLabs
	 * Ver: 0.9b
	 */
	public static void main(String[] args)
	{
		if(args.length == 0)
			printHelp();

		if(args[0].charAt(0) == '@')
		{
			Deanon.DEBUG = true;
			args[0] = args[0].substring(1);
		}
		
		if(args.length > 0 && args[0].equals("create_data"))
			Deanon.createTestData(args);
		else if(args.length > 0 && args[0].equals("simulate"))
			Deanon.simulateDeanon(args);
		else if(args.length > 0 && args[0].equals("analyze"))
			Deanon.analyzeResults(args);
		else if(args.length > 0 && args[0].equals("export"))
			Deanon.exportSubnet(args);
		else if(args.length > 0 && args[0].equals("measure"))
			Deanon.measureNetwork(args);
		else if(args.length > 0 && args[0].equals("summarize"))
			Deanon.summarizeNetwork(args);
		else
			printHelp();
	}

}
