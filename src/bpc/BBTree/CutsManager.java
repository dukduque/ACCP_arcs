package bpc.BBTree;

import gurobi.GRBLinExpr;

import java.util.ArrayList;
import java.util.Hashtable;

public class CutsManager {

	/**
	 * \sum{x_i} >=/<= parentNode(\sum{x_i})
	 */
	
	public static final int VEHICLES_CUT = 1 ;


	public static final int  XijBRANCH = 2; 

	public static final int SRI = 3;
	
	public ArrayList<GRBLinExpr> cuts;
	public ArrayList<Character> cutSense;
	public ArrayList<Integer> RHS;
	public Hashtable<String , Integer> cutType;
	public ArrayList<String> cutName;
	
	public Hashtable<String , int[]> cutQSets;
	public Hashtable<String , ArrayList<Integer>> cutQSetsRoutes;
	
	public CutsManager() {
		cuts = new ArrayList<>();
		cutSense = new ArrayList<>();
		RHS = new ArrayList<>();
		cutType = new Hashtable<String, Integer>();
		cutName = new ArrayList<>();
		cutQSets = new Hashtable<>();
		cutQSetsRoutes = new Hashtable<>();
	
	}

	
	
}
