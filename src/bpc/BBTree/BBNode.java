package bpc.BBTree;

import java.util.ArrayList;
import java.util.Hashtable;

import Utilities.Rounder;
import bpc.CG.LP_Manager;
import ColumnGenrators.PulseAlgorithm;
import Graph.Arco;
import Graph.Network;
import Graph.Nodo;
import IO.DataHandler;
import IO.Leg;
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class BBNode {

	
	/**
	 * Parent {@link BBNode} of this node
	 */
	public BBNode parent;
	
	/**
	 * Left {@link BBNode} child
	 */
	public BBNode leftChild;

	/**
	 * Right {@link BBNode} child
	 */
	public BBNode rightChild;
	
	/**
	 * List of indexes of the cuts that this bbnode represent
	 */
	public ArrayList<Integer> cutsIndexes;
	
	/**
	 * Node status
	 */
	public boolean active;
	
	/**
	 * Sum up all the variables of the master problem
	 */
	public double sumX_i;
	
	/**
	 * Feasible   
	 */
	public boolean feasibleXijflows;
	
	/**
	 * Objective function of the node
	 */
	public double objVal;
	
	
	public ArrayList<GRBVar> basicVariables;
	public ArrayList<Integer> basicIndexes;
	
	/**
	 * Number of basic non-degenerated variables
	 */
	private int basicCardinality;
	
	/**
	 * ID of the B&B tree
	 */
	public  int id;
	
	public int xijBranchInThisNode;
	
	/**
	 * Index of the arc (i,j) on the complete list of arcs in class {@link Network}
	 */
	public ArrayList<Integer> branchedXij;
	public ArrayList<Integer> branchedXijValue;
	
	/**
	 * List of the routes ids that were set to 0 due to x_ij branch
	 */
	public ArrayList<Integer> routesInLB;
	
	/**
	 * Pointer of the MP model solved in the node
	 */
	public GRBModel model;

	/**
	 * Prob status of the linear relaxation of the BBnode
	 */
	public String problemStatus;
	
	/**
	 * if the node is the >= branch,  up is 1, 0 other wise.
	 */
	public int up;

	/**
	 * Criterion to  sort the nodes (Descendently) for node selection BFS
	 */
	public double sortCriteria;
	 
	
	
	
	
	/**
	 * Creates a new node of the branch and bound tree. If the parent node
	 * is different from null (i.e., this is the root node), the parent inherits all his 
	 * cuts or branches so far. 
	 * @param id : the ID of the this new node
	 * @param nParent : the parent node in the branch and bound three.
	 * @param baseModel
	 * @param nUp Indicator of the branch type, up (1)  or down (0).
	 * @throws GRBException
	 */
	public BBNode (int id, BBNode nParent, GRBModel baseModel, int nUp) throws GRBException {
		up = nUp;
		feasibleXijflows = false;
		if (nParent==null) {
			parent = null;
			leftChild = null;
			rightChild = null;
			cutsIndexes = new ArrayList<>();
			active = true;
			xijBranchInThisNode = -1;
			branchedXij = new ArrayList<>();
			branchedXijValue = new ArrayList<>();
//			branchdXijRoutesInLB = new ArrayList<>();
			routesInLB = new ArrayList<>();
			this.id = id;
			model = baseModel;
		}else{
			parent = nParent;
			leftChild = null;
			rightChild = null;
			cutsIndexes = new ArrayList<>();
			cutsIndexes.addAll(nParent.cutsIndexes);
			active = true;
			xijBranchInThisNode = -1;
			branchedXij = new ArrayList<>();
			branchedXij.addAll(nParent.branchedXij);
			branchedXijValue = new ArrayList<>();
			branchedXijValue.addAll(nParent.branchedXijValue);
//			branchdXijRoutesInLB = new ArrayList<>();
//			branchdXijRoutesInLB.addAll(parent.branchdXijRoutesInLB);
			routesInLB = new ArrayList<>();
			routesInLB.addAll(parent.routesInLB);
			
			this.id = id;
			model = baseModel;
			sortCriteria = parent.objVal;
		}
		
	}
	
	
	public BBNode getParent(){
		return parent;
	}
	
	public BBNode getLeftChild(){
		return leftChild;
	}
	
	public BBNode getRightChild(){
		return rightChild;
	}
	public void setSum_X_i(double s){
		sumX_i = Rounder.round6Dec(s);
	}
	public void setBasicVariables(ArrayList<GRBVar> vars){
		basicVariables = new ArrayList<>();
		basicVariables.addAll(vars);
	}
	public void setBasicCardinality(int s){
		basicCardinality = s;
	}

	public boolean isInteger() {
		if(sumX_i == basicCardinality && feasibleXijflows){
			return true;
		}
		return false;
	}


	public boolean sumXiIsInteger() {
		if(sumX_i == (int)sumX_i){
			return true;
		}
		return false;
	}
	
	
	public String toString(CutsManager cm ) throws GRBException {
		String msn = "Id: " + id;
		msn+= "|Xb|=" + basicCardinality + "\tSumXi: " +sumX_i + "\tObjval: " + objVal;
		msn+= "\t cuts: " + cutsIndexes.size() + "\t";
		msn+= "branch" + branchedXij .size() + "\t";
		
//		for (int i = 0; i < cutsIndexes.size(); i++) {
//			int ci = cutsIndexes.get(i);
//			if (cm.cutType.get(cm.cutName.get(ci)) != cm.VEHICLES_CUT) {
//				int terminos = 0;
//				GRBLinExpr glin = cm.cuts.get(ci);
//				for (int j = 0; j < glin.size(); j++) {
//					msn += glin.getCoeff(j) + ""
//							+ glin.getVar(j).get(GRB.StringAttr.VarName)
//							+ " + ";
//					terminos++;
//				}
//				msn += cm.cutSense.get(ci) + " " + cm.RHS.get(ci) + "\n";
//				msn += " tiene " + terminos + "terminos \n";
//			}
//		}
		msn+= " EXE: " + ((System.currentTimeMillis()-LP_Manager.tnow)/1000.0);
		for (int i = 0; i < branchedXij.size(); i++) {
			int x = branchedXij.get(i);
			msn += "Arc("+ x+ ") =" +branchedXijValue.get(i) + "\t"					;
		}
		//msn+= "---------------------------------------------------------------\n";

		return msn;
	}
	@Override
	public String toString() {
		return sortCriteria + "";
	}

	public BBNode searchNextNode() {
		if(parent==null){
			return null;
		}
		if (parent.rightChild != null && parent.rightChild.active) {
			return parent.rightChild;
		} else if (parent.leftChild != null && parent.leftChild.active) {
			return parent.leftChild;
		} else {
			return parent.searchNextNode();
		}
		
	}


	public void updateRouteInLB(LP_Manager algorithm, Network network) {
		if (routesInLB.size()==0) {
			return;
		}
		int biggerIndex = routesInLB.get(routesInLB.size()-1);
		ArrayList<Integer> routesXij;
		for (int a = 0; a < branchedXij.size(); a++) {
			int arc_id = branchedXij.get(a);
			int v_i = network.getArc(arc_id).get_v_i().id;
			int v_j = network.getArc(arc_id).get_v_j().id;
			int val = branchedXijValue.get(a);
			if (val == 0) {
				routesXij = algorithm.routes_per_arc[arc_id];
				for (int j = routesXij.size() - 1; j >= 0; j--) {
					int route = routesXij.get(j);
					if (route > biggerIndex) {
						binaryInsertionForIntArrayList(routesInLB, route);
					} else {
						j = -1;
					}
				}
			} else {
				int NOTUsing_arc_id = -1;
				// Updated xij' routes
				ArrayList<Integer> magicIndex_v_i = network.getNodes().get(v_i).MagicIndex;
				for (int k = 0; k < magicIndex_v_i.size(); k++) {
					NOTUsing_arc_id = magicIndex_v_i.get(k);
					routesXij = algorithm.routes_per_arc[NOTUsing_arc_id];
					if (network.getArc(NOTUsing_arc_id).get_v_j().id != v_j && routesXij != null) {
						for (int j = routesXij.size() - 1; j >= 0; j--) {
							int route = routesXij.get(j);
							if (route > biggerIndex) {
								binaryInsertionForIntArrayList(routesInLB,route);
							} else {
								j = -1;
							}
						}
					}
				}
				// Update xi'j routes
				ArrayList<Integer> rMagicIndex_v_j = network.getNode(v_j).rMagicIndex;
				for (int k = 0; k < rMagicIndex_v_j.size(); k++) {
					NOTUsing_arc_id = rMagicIndex_v_j.get(k);
					routesXij = algorithm.routes_per_arc[NOTUsing_arc_id];
					if (network.getArc(NOTUsing_arc_id).get_v_i().id != v_i && routesXij != null) {	
						for (int j = routesXij.size() - 1; j >= 0; j--) {
							int route = routesXij.get(j);
							if (route > biggerIndex) {
								binaryInsertionForIntArrayList(routesInLB,route);
							} else {
								j = -1;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param set
	 * @param theInt
	 * @return true if the int was successfully inserted
	 */
	private boolean binaryInsertionForIntArrayList(ArrayList<Integer> set, int theInt){
		if (set.size()==0) {
			set.add(theInt);
			return true;
		}
	
		boolean cond = true;
		int l = 0;
		int r = set.size();
		int m = (int) ((l + r) / 2);
		int mVal =set.get(m);
		
		while(cond){
			if(r-l>1){
				if (theInt < mVal) {
					r = m;
					m = (int) ((l + r) / 2);
				} else if (theInt> mVal) {
					l = m;
					m = (int) ((l + r) / 2);
				} else {
					return false;
				}
				mVal = set.get(m);
				
			}else{
				cond = false;
				if (l == m) {
					set.add(theInt < mVal ? l : l + 1, theInt);
				} else if (r == m) {
					System.out.println("esto no pasa !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11 LSET ALGO ");
				} else {
					System.err.println( " insert label, error");
				}
				return false;
			}
		}
		return false;
	
	}


	public void AddAllNewRoutesInLB(ArrayList<Integer> arrayList) {
		for (int i = 0; i < arrayList.size(); i++) {
			int route = arrayList.get(i);
			binaryInsertionForIntArrayList(routesInLB, route);
		}
		
	}


	public void printSOl(DataHandler data) throws GRBException {
		int[] pilotsPerday = new int[8];
		Leg leg = null;
		for (int i = 0; i < basicIndexes.size(); i++) {
			int index = basicIndexes.get(i);
			ArrayList<Integer> pairing = LP_Manager.pool.get(index);
//			System.out.println("Pairing " + i);	
			int[] pilotsPerdayBoolean = new int[8];
			for (int j = 0; j < pairing.size(); j++) {
				leg = data.getLegsToSolve().get(pairing.get(j));
				int day = numDay(leg.getDayWeek());
				if (pilotsPerdayBoolean[day]==0) {
					pilotsPerdayBoolean[day]= 1;
					pilotsPerday[day]++;
				}
				if(pairing.size()==1){
					System.out.println(i+ "/" + leg);
					pilotsPerday[day]--;
				}
			}
//			System.out.println("Y CON EL DEADHEAD");
			ArrayList<Integer> arcs = LP_Manager.pool_of_arcs.get(index);
			for (int j = 0; j < arcs.size(); j++) {
				Arco arc = PulseAlgorithm.network.getArcs().get(arcs.get(j));
				if (arc.getType() == Arco.TYPE_FIGHT) {
					Nodo tail = arc.get_v_j();
					leg = data.getLegsToSolve().get(tail.getLegId());
//					System.out.println(+i + "/" + leg + "/"+arc.getTypeName());
				} else if (arc.getType() == Arco.TYPE_DEADHEAD) {
					Nodo tail = arc.get_v_i();
					leg = data.getLegsToSolve().get(tail.getLegId());
//					System.out.println(i + "/" + leg + "/"+arc.getTypeName());
				}else{
//					System.out.println(i +"/"+ arc.getTail().getTypeString()
//							+"/"+ arc.getHead().getTypeString() 
//							+"/"+ arc.getTail().getStation()+"/"
//							+ arc.getTail().getTime()+"/"
//							+ arc.getHead().getStation()+"/"
//							+ arc.getHead().getTime()+ "/-/-/"+arc.getTypeName());
//					System.out.println("->" + i + " " + arc + " In: " + arc.getTail().getStation() + " " + arc.getHead().getStation());
				}
			}
			
		}
		
	
		
		for (int i = 1; i < pilotsPerday.length; i++) {
			System.out.println( "day " + i + " PIL->" + pilotsPerday[i]);
		}
		
//		System.out.println("model ctrs");
//		for (int i = 0; i < model.getConstrs().length; i++) {
//			System.out.println(model.getConstr(i).get(GRB.StringAttr.ConstrName) +  " Slack->  "+ model.getConstr(i).get(GRB.DoubleAttr.Slack));
//		}
	}
	private int numDay(String dayWeek){
		if (dayWeek.equals("LU")) {
			return  1;
		} else if (dayWeek.equals("MA")) {
			return   2;
		} else if (dayWeek.equals("MI")) {
			return   3;
		} else if (dayWeek.equals("JU")) {
			return   4;
		} else if (dayWeek.equals("VI")) {
			return   5;
		} else if (dayWeek.equals("SA")) {
			return   6;
		} else if (dayWeek.equals("DO")) {
			return   7;
		}else{
			System.err.println("DIA RARO " + dayWeek);
		}
		return -1;
	}


	public void printCurrentPairings(DataHandler data) throws GRBException {
		Leg leg = null;
		for (int i = 0; i < basicIndexes.size(); i++) {
			int index = basicIndexes.get(i);
			double val = basicVariables.get(i).get(GRB.DoubleAttr.X);
			ArrayList<Integer> pairing = LP_Manager.pool.get(index);
//			System.out.println("Pairing " + i);	
			int[] pilotsPerdayBoolean = new int[8];
			for (int j = 0; j < pairing.size(); j++) {
				leg = data.getLegsToSolve().get(pairing.get(j));
				int day = numDay(leg.getDayWeek());
				if (pilotsPerdayBoolean[day]==0) {
					pilotsPerdayBoolean[day]= 1;
				
				}
				if(pairing.size()==1)
				System.out.println(i+ "/" + leg+"/"+index+"/"+ val);
			}
//			System.out.println("Y CON EL DEADHEAD");
			ArrayList<Integer> arcs = LP_Manager.pool_of_arcs.get(index);
			for (int j = 0; j < arcs.size(); j++) {
				Arco arc = PulseAlgorithm.network.getArcs().get(arcs.get(j));
				if (arc.getType() == Arco.TYPE_FIGHT) {
					Nodo tail = arc.get_v_j();
					leg = data.getLegsToSolve().get(tail.getLegId());
					System.out.println(+i + "/" + leg + "/"+arc.getTypeName()+"/"+index+"/"+ val);
//					System.out.println("" + i + " " + arc + " In: " + arc.getTail().getStation() + " " + arc.getHead().getStation());
				} else if (arc.getType() == Arco.TYPE_DEADHEAD) {
					Nodo tail = arc.get_v_i();
					leg = data.getLegsToSolve().get(tail.getLegId());
					System.out.println(i + "/" + leg + "/"+arc.getTypeName()+"/"+index+"/"+ val);
//					System.out.println("*" + i + " " + arc + " In: " + arc.getTail().getStation() + " " + arc.getHead().getStation());
				}else{
//					System.out.println(i +"/"+ arc.getTail().getTypeString()
//							+"/"+ arc.getHead().getTypeString() 
//							+"/"+ arc.getTail().getStation()+"/"
//							+ arc.getTail().getTime()+"/"
//							+ arc.getHead().getStation()+"/"
//							+ arc.getHead().getTime()+ "/-/-/"+arc.getTypeName());
//					System.out.println("->" + i + " " + arc + " In: " + arc.getTail().getStation() + " " + arc.getHead().getStation());
				}
			}
			
		}
		
	}


	public ArrayList<Leg> getUncoveredLegs(DataHandler data) {
		ArrayList<Leg> list = new ArrayList<>();
		Leg leg = null;
		for (int i = 0; i < basicIndexes.size(); i++) {
			int index = basicIndexes.get(i);
			ArrayList<Integer> pairing = LP_Manager.pool.get(index);
			if (pairing.size() == 1) {
				leg = data.getLegsToSolve().get(pairing.get(0));
//				System.out.println(i + "/" + leg);
				list.add(leg);
			}
		}

		return list;
	}
	
	
	
	
}
