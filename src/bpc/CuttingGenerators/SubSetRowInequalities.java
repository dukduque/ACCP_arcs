package bpc.CuttingGenerators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import Utilities.VRPTWCG.Rounder;
import bpc.BBTree.BBNode;
import bpc.BBTree.CutsManager;
import bpc.CG.LP_Manager;
import IO.DataHandler;

public class SubSetRowInequalities {

	
	public static double minViolation;
	public static double maxNodeTimesInSet;
	private GRBModel model;
	private LP_Manager algorithm;
	
	
	private int[] point;
	private ArrayList<Integer> FS_i;
	private ArrayList<Integer> FS_j;
	private ArrayList<Double> FS_xij;

	//CYCLES-------------------------------------------------------------------------------
	int[][] marks;
	int[] marks2;
	int[] visited;
	//-------------------------------------------------------------------------------------
	
	
	// Set Q relate
	// nodes-------------------------------------------------------------------
	public ArrayList<String> Qcurrentkeys;
	public ArrayList<String> QnodeSetKeys;
	public Hashtable<String, int[]> QnodeSet;
	public Hashtable<String, ArrayList<Integer>> Qroutes;
	public Hashtable<String, GRBLinExpr> Qcuts;
	public Hashtable<String, Double> Qviolations;
	public Hashtable<String, Boolean> QAdded;
	private int numberOfSets;
	private int[] nodeTimesInSet;
	//-------------------------------------------------------------------------------------
	
	public SubSetRowInequalities(GRBModel nModel, LP_Manager nAlgorithm , double nMinViolation) {
		model = nModel;
		algorithm = nAlgorithm;
		minViolation = nMinViolation;
		
	}
	
	public void buildFS(){
		point = new int[DataHandler.numLegs+2];
		FS_i = new ArrayList<>();
		FS_j = new ArrayList<>();
		FS_xij = new ArrayList<>();
		String arc;
		int v_i,v_j;
		double value =0 ;
		for (int i = 0; i < algorithm.X_ijVars.size(); i++) {
			//TODO todo esto se comento y cambio en el arreglo de los arcos del commite B&P not working due to..!!!
			arc = null;//algorithm.X_ijVars.get(i);
//			value = algorithm.X_ij.get(arc);
			v_i = 1;//;algorithm.X_ij_tail.get(arc);
			v_j = 1;//;algorithm.X_ij_head.get(arc);
			if(v_i!=0 && v_j!=0){
				//Add i,j
				int whereToAdd = point[v_i];
				FS_i.add(whereToAdd,v_i);
				FS_j.add(whereToAdd,v_j);
				FS_xij.add(whereToAdd,algorithm.X_ij.get(arc));
				upDatePointer(v_i);
				
				//Add j,i
				whereToAdd = point[v_j];
				FS_i.add(whereToAdd,v_j);
				FS_j.add(whereToAdd,v_i);
				FS_xij.add(whereToAdd,algorithm.X_ij.get(arc));
				upDatePointer(v_j);
			}
		}
	}
	private void upDatePointer(int v_i) {
		for (int i = v_i+1; i < point.length; i++) {
			point[i]++;
		}
		
	}
	
	public void detect3SizeCycles(){
		
		QnodeSet = new Hashtable<>();
		QnodeSetKeys = new ArrayList<>();
		nodeTimesInSet = new int[DataHandler.numLegs+1];
		
		marks2 = new int[point.length-1];
		int[] involved = {-1,-1,-1};
		int numInvolved = 0;
		for (int i = 1; i <= DataHandler.numLegs; i++) {
			marks = new int[point.length-1][point.length-1];
//			searchCycles(i, i, numInvolved, involved, 3, -1, 0);
//			marks2[i]=1;
		}
	
	}
	
	

	public void  cycleSepAlgorithm() {
//		System.out.println("COMEZAMOS");
		int minSize = 3;
		int maxSize = 15;
		QnodeSet = new Hashtable<>();
		QnodeSetKeys = new ArrayList<>();
		Qroutes = new Hashtable<>();
		Qcuts  = new Hashtable<>();
		Qviolations = new Hashtable<>();
		buildFS();
		marks2 = new int[point.length-1];
		marks = new int[point.length-1][point.length-1];
		visited = new int[point.length-1];
		int numInvolved = 0;
		int[] involved = new int [maxSize]; 
		for (int i = 1; i <= DataHandler.numLegs; i++) {
//			System.out.println(i);
//			marks = new int[point.length-1][point.length-1];
			searchCycles(i, i, numInvolved, involved, minSize, maxSize, -1, 0);
			marks2[i]=1;
		}
//		System.out.println("TERMINAMOS");
//		System.out.println(QnodeSetKeys.size());
	}
	
	
	

	private void searchCycles(int root, int v_i, int numInvolved, int[] involved, int minSize , int MaxCycleSize, int pointer, int charge) {
		if (root!=v_i) {

			visited[v_i]  =1;
						
		}
		if (root == v_i && numInvolved >= minSize && numInvolved<= MaxCycleSize) {
			int[] clone = involved.clone();
			genSubGroups(clone, numInvolved);
//			if (numInvolved==3) {
//				String key = genQsetKey(clone, numInvolved);
//				if(QnodeSet.containsKey(key)==false){
////					System.out.println(key);
//					QnodeSetKeys.add(key);
//					QnodeSet.put(key, clone);
//				}
//			}else{
//				
//			}
			
		} else if( numInvolved<MaxCycleSize) {
			involved[numInvolved] = v_i;
			numInvolved++;
			int whereToLook = point[v_i];
			int whereToStopLooking = point[v_i + 1];
			for (int i = whereToLook; i < whereToStopLooking; i++) {
				int v_j = FS_j.get(i);
				if (v_j != 0 && marks2[v_j] ==0  && visited[v_j] ==0  && marks[v_i][v_j]==0) { 
					marks[v_j][v_i]=1;
					searchCycles(root, v_j, numInvolved, involved, minSize,MaxCycleSize, i, 0);
					marks[v_j][v_i]=0;
				}
			}
			numInvolved--;
			involved[numInvolved]=-1;

		}
		visited[v_i] =0;
	}

	

	private void genSubGroups(int[] bigCycle, int numBig) {
//		String key1 = genQsetKey(bigCycle, numBig);
//		System.out.println("*" + key1);
		int[]  clone  = new int[3];
		for (int i = 0; i < numBig-2; i++) {
			clone[0] = bigCycle[i];
			for (int j = i+1; j < numBig-1; j++) {
				clone[1] = bigCycle[j];
				for (int k = j+1; k < numBig; k++) {
					clone[2] = bigCycle[k];
					String key = genQsetKey(clone, 3);
					if(QnodeSet.containsKey(key)==false){
//						System.out.println("*"+ key);
						QnodeSetKeys.add(key);
						QnodeSet.put(key, clone);
					}
				}
			}
		}
		
		
		
	}

	private String genQsetKey(int[] involved, int numInvolved) {
		String key = "";
		int tempi;
		for (int i = 0; i < numInvolved; i++) {
			for (int j = i+1; j <  numInvolved; j++) {
				if(involved[j]<involved[i]){
					tempi = involved[i];
					involved[i] =involved[j];
					involved[j] = tempi;
				}
			}
			key+=involved[i]+",";
		}
		return key;
	}

	
	
	
	/**
	 * Evaluate 
	 * @throws GRBException 
	 */
	public void evaluateSets() throws GRBException{
		buildFS();
		detect3SizeCycles();
		
		Qroutes = new Hashtable<>();
		Qcuts  = new Hashtable<>();
		Qviolations = new Hashtable<>();
		ArrayList<Integer> routesForCut;
		double sumXi = 0;
		int[] routesUtilisation;
		
		
		for (int i = 0; i < QnodeSetKeys.size(); i++) {
			sumXi = 0;
			String setKey = QnodeSetKeys.get(i);
			int[] set  = QnodeSet.get(setKey);
			routesForCut = new ArrayList<>();
			routesUtilisation = new int[algorithm.pool.size()];
			GRBLinExpr  cut = new GRBLinExpr();
			for (int j = 0; j < set.length; j++) {
				int node  = set[j];
				ArrayList<Integer> routesOfNode = algorithm.routes_per_node[node];
				for (int k = 0; k < routesOfNode.size(); k++) {
					int route = routesOfNode.get(k);
					if(routesUtilisation[route] == 0){
						for (int l = j+1; l < set.length; l++) {
							int compareNode = set[l];
							if(algorithm.pool.get(route).contains(compareNode)){
								routesForCut.add(route);
								routesUtilisation[route]++;
								cut.addTerm(1, model.getVar(route));
								l=set.length;
							}
						}
					}
				}
			}
			if(cut.getValue()  >= 1+ 0.01) {
				Qcuts.put(setKey, cut);
				Qviolations.put(setKey, cut.getValue());
				Qroutes.put(setKey,routesForCut);
				System.out.println("Aceptado (" + set[0]+ "," + set[1]+ "," + set[2]+ ") tiene " + routesForCut.size() + " variables y el corte da" + sumXi + " == " +cut.getValue());
			}else{
//				System.out.println("Rejected (" + set[0]+ "," + set[1]+ "," + set[2]+ ") tiene " + routesForCut.size() + " variables y el corte da" + sumXi + " == " +cut.getValue());
				QnodeSet.remove(QnodeSetKeys.get(i));
				QnodeSetKeys.remove(i);
				i--;
			}
		}
//		System.out.println("ApprovedCuts: " +  Qcuts.size());
	}
	boolean firstTime = false;
	/**
	 * Evaluate 
	 * @param cM 
	 * @throws GRBException 
		 */
	public int evaluateSetsAll(CutsManager CM, BBNode current, int maxCuts) throws GRBException{
		Qcurrentkeys = new ArrayList<>();
		
		if (!firstTime) {
			firstTime = true;
			findSets();
//			cycleSepAlgorithm();
			Qroutes = new Hashtable<>(100000);
			Qcuts  = new Hashtable<>(100000);
			Qviolations = new Hashtable<>(100000);
			QAdded = new Hashtable<>(100000);
			buildUp(CM, current);
		}else{
			updateNoAddedCuts(CM, current);
		}
		int number_of_added_cuts=0;
		int[] QcutsPerNode = new int[DataHandler.numLegs+1];
		for (int i = 0; i < Math.min(Qcurrentkeys.size(),maxCuts); i++) {
			String name = Qcurrentkeys.get(i);
//			if(nodesSaturation(QcutsPerNode, QnodeSet.get(name))){
				QAdded.put(name, true);
				//System.out.println("corte->"  + name + " vio: " + Qviolations.get(name));
				GRBLinExpr exp = Qcuts.get(name);
				CM.cuts.add(exp);
				CM.cutSense.add(GRB.LESS_EQUAL);
				CM.RHS.add(1);
				CM.cutName.add(name);
				CM.cutType.put(name, CutsManager.SRI);
				int[] set = QnodeSet.get(name);
				CM.cutQSets.put(name, set);
				ArrayList<Integer> routesOfSet = Qroutes.get(name);
				CM.cutQSetsRoutes.put(name, routesOfSet);
				current.cutsIndexes.add(CM.cuts.size() - 1);
				algorithm.addCut(current, current.model, CM.cuts.size() - 1);
				number_of_added_cuts++;
//			}
		}
		Qcurrentkeys.clear();
		
		return number_of_added_cuts;
	}


	
	private boolean nodesSaturation(int[] qcutsPerNode, int[] set) {
		for (int i = 0; i < set.length; i++) {
			int node = set[i];
			if (qcutsPerNode[node]>=10) {
				return false;
			}
		}
		for (int i = 0; i < set.length; i++) {
			int node = set[i];
			qcutsPerNode[node]++;
		}
		return true;
	}

	private void updateNoAddedCuts(CutsManager cM, BBNode current) throws GRBException {
		
		for (int i = 0; i < QnodeSetKeys.size(); i++) {
			String name = QnodeSetKeys.get(i);
			if(!QAdded.get(name)){
				int[] set = QnodeSet.get(name);
				ArrayList<Integer> routes = Qroutes.get(name);
				int lastRoute = routes.get(routes.size()-1);
				for (int k = lastRoute+1; k < algorithm.pool.size() ; k++) {
					ArrayList<Integer> dummyPath = algorithm.pool.get(k);
					int counter = 0;
					for (int j = 0; j < set.length; j++) {
						int node = set[j];
						if (dummyPath.contains(node)) {
							counter++;
						}
						if(counter>=2){
							j=10000000;
							Qroutes.get(name).add(k);
							Qcuts.get(name).addTerm(1, model.getVar(k));
						}
					}
					
				}
				if(Qcuts.get(name).getValue()>= 1+ minViolation){
					Qviolations.put(name, Qcuts.get(name).getValue());
					addCutKey(name);
//					System.out.println("Aceptado (" + set[0]+ "," + set[1]+ "," + set[2]+ ") tiene " + " variables y el corte da" + " == " +Qcuts.get(name).getValue());
				}
			}
		}

	}

	private  ArrayList<String>  buildUp(CutsManager cM, BBNode current) throws GRBException {
		ArrayList<String> retorno = new ArrayList<>();
		ArrayList<Integer> routesForCut;
		double sumXi = 0;
		int[] routesUtilisation;
		ArrayList<Integer> routesOfNode = null;
		
		for (int i = 0; i < QnodeSetKeys.size(); i++) {
			sumXi = 0;
			String setKey = QnodeSetKeys.get(i);
			int[] set  = QnodeSet.get(setKey);
			routesForCut = new ArrayList<>();
			routesUtilisation = new int[algorithm.pool.size()];
			GRBLinExpr  cut = new GRBLinExpr();
			for (int j = 0; j < set.length; j++) {
				int node  = set[j];
				try {
					routesOfNode = algorithm.routes_per_node[node];
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(setKey + " ALOOOOO" + " node " + node);
				}
				
				for (int k = 0; k < routesOfNode.size(); k++) {
					int route = routesOfNode.get(k);
					if(routesUtilisation[route] == 0){
						for (int l = j+1; l < set.length; l++) {
							int compareNode = set[l];
							if(algorithm.pool.get(route).contains(compareNode)){
								routesForCut.add(route);
								routesUtilisation[route]++;
								cut.addTerm(1, model.getVar(route));
								l=set.length+100;
							}
						}
					}
				}
			}
//			System.out.println(setKey);
			
			if (cut.getValue() >=0.8) {
				Qcuts.put(setKey, cut);
				Qviolations.put(setKey, cut.getValue());
				Qroutes.put(setKey, routesForCut);
				QAdded.put(setKey, false);
				if(cut.getValue()  >= 1+ minViolation) {
					addCutKey(setKey);
//					System.out.println("Aceptado (" + set[0]+ "," + set[1]+ "," + set[2]+ ") tiene " + routesForCut.size() + " variables y el corte da" + sumXi + " == " +cut.getValue());
				}
			}else{
				QnodeSet.remove(QnodeSetKeys.get(i));
				QnodeSetKeys.remove(i);
				i--;
			}
		}
		return retorno;
	}

	private void findSets() {
		QnodeSet = new Hashtable<>(100000);
		QnodeSetKeys = new ArrayList<>(10000);
		int numNodes = DataHandler.numLegs+1;
		for (int i = 1; i < numNodes; i++) {
			for (int j = i+1; j < numNodes; j++) {
				for (int k = j+1; k < numNodes; k++) {
//					if (DataHandler.r.nextDouble()<0.8) {
						int[] set = {i,j,k};
						String key = genQsetKey(set, set.length);
						QnodeSet.put(key, set);
						QnodeSetKeys.add(key);
//					}
				}
			}
		}
	}



	private void addCutKey(String key) {
		double newC1 =Qviolations.get(key);
		double newC2 =Qroutes.get(key).size();
		double mVal;
		double mVal2;
		if(Qcurrentkeys.size() == 0){
			Qcurrentkeys.add(key);
			return;
		}else if(Qcurrentkeys.size() == 1){
			mVal = Qviolations.get(Qcurrentkeys.get(0));
			mVal2 = Qroutes.get(Qcurrentkeys.get(0)).size();
			if( newC1> mVal){
				Qcurrentkeys.add(0, key);
			}else if(newC1 < mVal){
				Qcurrentkeys.add(1, key);
			}else{
				if(newC2 > mVal2){
					Qcurrentkeys.add(0, key);
				}else if(newC2 < mVal2){
					Qcurrentkeys.add(1,key);
				}else{
					Qcurrentkeys.add(1,key);
				}
				
			}
			return;
		}else{
			boolean cond = true;
			int l = 0;
			int r = Qcurrentkeys.size();
			int m = (int) ((l + r) / 2);
			mVal = Qviolations.get(Qcurrentkeys.get(m));
			mVal2 = Qroutes.get(Qcurrentkeys.get(m)).size();
			double cScore = newC1;
			while(cond){
				if(r-l>1){
					if (cScore > mVal) {
						r = m;
						m = (int) ((l + r) / 2);
					} else if (cScore < mVal) {
						l = m;
						m = (int) ((l + r) / 2);
					} else if (newC2 > mVal2) {
						r = m;
						m = (int) ((l + r) / 2);
					} else if (newC2 < mVal2) {
						l = m;
						m = (int) ((l + r) / 2);
					}else {
						Qcurrentkeys.add(m, key);
						return;
					}
					mVal = Qviolations.get(Qcurrentkeys.get(m));
					mVal2 = Qroutes.get(Qcurrentkeys.get(m)).size();
				}else{
					cond = false;
					if (l == m) {
						if(cScore == mVal)
						{
							Qcurrentkeys.add(newC2>mVal2?l:l + 1, key);
						}else{
							Qcurrentkeys.add(cScore > mVal ? l : l + 1, key);
						}					
					} else if (r == m) {
						System.out.println("esto no pasa !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11 LSET ALGO ");
//						L.add(cScore > mVal ? r : Math.min(r + 1, L.size()), np);
					} else {
						System.err.println( " insert label, error");
					}
					return;
				}
			}
			
		}
		
	}
	
	


	
	
	

}
