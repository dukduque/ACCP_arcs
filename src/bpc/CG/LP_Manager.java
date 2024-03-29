package bpc.CG;

import gurobi.GRB;
import gurobi.GRB.IntAttr;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBExpr;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import Utilities.Rounder;
import bpc.BBTree.BBNode;
import bpc.BBTree.BPC_Algorithm;
import bpc.BBTree.CutsManager;
import Graph.Arco;
import Graph.Network;
import Graph.Nodo;
import IO.DataHandler;
import IO.Leg;
/**
 * Main Logic of the CG
 * @author Daniel
 *
 */
public class LP_Manager {
		

	/**
	 * Decimal part of the division between the barrier and the bound step.
	 */
	public static double decimal = 0;
	
	
	public static long tnow;
	long t_end;
	long exactTime;
	double phase1bound;
	int phase1pool;
	
	
	/**
	 * The pool of routes as ArrayList of nodes ids 
	 */
	public static ArrayList<ArrayList<Integer>> pool;
	
	public static ArrayList<ArrayList<Integer>> pool_of_arcs;
	public static ArrayList<Double> pDist;
	public static ArrayList<Integer> generator;
		

	
	/**
	 * Contains for each arc, a list of all the routes indexes that uses the related arc,
	 */
	public ArrayList<Integer>[] routes_per_arc;
	
	/**
	 * Contains for every node, a list of all the routes index that uses the related node.
	 */
	public ArrayList<Integer>[] routes_per_node;

	
	
	/**
	 * Table that contains the current value of x_ij variables
	 */
	public Hashtable<Integer, Double> X_ij;

//	/**
//	 * Table that contains a pointer to the arc in the network
//	 */
//	public Hashtable<Integer, Arco> X_ij_arc;
//	
	
//	/**
//	 * Table that contains the tail id of the arc i,j
//	 */
//	public  Hashtable<Integer, Integer> X_ij_tail;
//
//	/**
//	 * Table that contains the head id of the arc i,j
//	 */
//	public Hashtable<Integer, Integer> X_ij_head;

	/**
	 * Names of the variables x_ij that take value
	 */
	public ArrayList<Integer> X_ijVars;
	
	
	public static  int maxIterations = 0;
	public static int maxIterationsTS = 30;
	
	public static double aBound=Double.POSITIVE_INFINITY;
	
	public static double alpha;
	public static int beam;
	
	public static Thread[] threads ;
	
	
	//public static boolean phaseII;
	public static int pulseNegPaths;
	
	/**
	 * Number of MPVars in the model
	 */
	public int MPvars;
	
	/**
	 * GRB variables of the master problem
	 */
	private ArrayList<GRBVar> mp_vars;
	
	//Stabilization
	private GRBVar[] y_plus;
	private GRBVar[] y_minus;
	private double[] d_plus;
	private double[] d_minus;
	private double[] e_plus;
	private double[] e_minus;
	private double boxlengh;
	private double boxcenter[];
	
	//LP
	private GRBLinExpr realFO;
	
	
	
	private ArrayList<Integer> basicIndexes;
	private ArrayList<GRBVar> basicVariables;
	private ArrayList<Double> basicVariablesVals;
	private ArrayList<Integer> ceroRCIndexes;
	boolean firstTimeHEu = true;
	private int beamini = 100;
	private  CutsManager CM;
	private double sumXi;
	
	public double[] pi;
	public static ArrayList<Double> mu_q;
	
	private DataHandler data;
	private Network network;
	
	
	/**
	 * Builds an instances of the LP manager
	 * @param cM
	 */
	public LP_Manager(CutsManager cM, DataHandler data, Network network) {
		this.data = data;
		this.network = network;
		tnow = System.currentTimeMillis();
		alpha = 1;
		beam= beamini;
		pool = new ArrayList<>();
		pDist = new ArrayList<>();
		generator = new ArrayList<>();
		pool_of_arcs = new ArrayList<>();
		routes_per_arc  = new ArrayList[Network.numArcs];
		routes_per_node = new ArrayList[Network.numNodes];


		basicIndexes = new ArrayList<>();
		ceroRCIndexes = new ArrayList<>();
		basicVariables = new ArrayList<>();
		basicVariablesVals = new ArrayList<>();
		CM = cM;
		threads = new Thread[1];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread();
		}
		
		
		
	}


	public void initializePool() {
		ArrayList<Leg> legs = data.getLegsToSolve();
		for (int i = 0; i < legs.size(); i++) {
			ArrayList<Integer> dummyPath = new ArrayList<>();
			dummyPath.add(i);
			//TODO initial paths does not have arcs
			addRoutesToArcs(null, legs.get(i));
			addRoutesToNodes(dummyPath);
			pool.add(dummyPath);
			pDist.add(1.0);
			generator.add(-1);
			pool_of_arcs.add(new ArrayList<Integer>());
		}
	}

	public void InitializeMP(GRBEnv env, GRBModel model) throws GRBException {
		realFO= new GRBLinExpr();
		
		// Stabilization variables
		y_minus = new GRBVar[data.getLegsToSolve().size()];
		y_plus = new GRBVar[data.getLegsToSolve().size()];
		initializeStabilizationParameters();
		
		//Master problem variables
		mp_vars = new ArrayList<>();
		GRBVar[] x = new GRBVar[pool.size()];
		//Create variables 
		for (int i = 0; i < x.length; i++) {
			x[i] = model.addVar(0,  1, pDist.get(i), GRB.CONTINUOUS, "x["+i+"]");
			mp_vars.add(x[i]);
			realFO.addTerm(1, x[i]);
		}
		//add variables
		model.update();
		
		//Add constraints
		ArrayList<Leg> legs = data.getLegsToSolve();
		for (int j = 0; j < legs.size(); j++) {
			y_minus[j] = model.addVar(0, e_minus[j], -d_minus[j] ,GRB.CONTINUOUS,"y-"+j);
			y_plus[j] = model.addVar(0, e_plus[j], d_plus[j] , GRB.CONTINUOUS,"y+"+j);
			model.update();
			GRBLinExpr expr = new GRBLinExpr();
			for (int i = 0; i < pool.size(); i++) {
				int a = 0;
				for (int k = 0; k < pool.get(i).size(); k++) {
					if(pool.get(i).get(k)==j){
						a=1;
						k=100000;
					}
				}
				expr.addTerm(a, x[i]);
			}
			expr.addTerm(-1, y_minus[j]);
			expr.addTerm(1, y_plus[j]);
			model.addConstr(expr, GRB.EQUAL , 1, "Leg"+j);
		}
		model.update();
		
		MPvars= pool.size();
	}


	
	public void initializeStabilizationParameters() {
		boxlengh = CG.stabilize?0.01:Double.POSITIVE_INFINITY;
		d_plus = new double [data.getLegsToSolve().size()];
		d_minus = new double [data.getLegsToSolve().size()];
		e_minus = new double [data.getLegsToSolve().size()];
		e_plus = new double [data.getLegsToSolve().size()];
		boxcenter = new double[data.getLegsToSolve().size()];
		for (int i = 0; i < d_plus.length; i++) {
			d_minus[i] = CG.stabilize?0.02:0;
			d_plus[i] =CG.stabilize?0.03:0;
						
			e_minus[i]  = CG.stabilize?10:0;
			e_plus[i]  = CG.stabilize?10:0;
			
			boxcenter[i] = (d_plus[i]+d_minus[i])/2.0;
		}
	}


	public int runMP(BBNode currentNode, GRBModel model) throws GRBException {
		int n = data.getLegsToSolve().size();
		pi = new double[n];
		
		//Create new variables and add them to the model
		GRBVar[] x = new GRBVar[pool.size() - MPvars];
		for (int i = this.MPvars; i < pool.size(); i++) {
			x[i - MPvars] = model.addVar(0.0, 1.0 ,  pDist.get(i) , GRB.CONTINUOUS, "x[" + (i) + "]");
			realFO.addTerm(1, x[i-MPvars]);
			mp_vars.add(x[i-MPvars]);
		}
		model.update(); 

	     
		//---------------------------------------------------------------------------------------
	    //Change the set covering constrains by adding the new variables
		for (int j = 0; j < n; j++) {
			for (int i = this.MPvars; i < pool.size(); i++) {
				int a = 0;
				for (int k = 0; k < pool.get(i).size(); k++) {
					if (pool.get(i).get(k) == j ) {
						a = 1;
						k = 1000000;
					}
				}
				model.chgCoeff(model.getConstr(j), x[i - this.MPvars], a);
			}
		}
		model.update();
	
		//Update all cuts (expressions) and current ctrs with the new generated variables 
		if (MPvars < pool.size()) {
			if(currentNode.cutsIndexes.size()>0){
				for (int i = 0; i < currentNode.cutsIndexes.size(); i++) {
					int cutIndex = currentNode.cutsIndexes.get(i);
					String cutName = CM.cutName.get(cutIndex);
					if (CM.cutType.get(cutName) == CM.VEHICLES_CUT) {
						updateVehiclesCut(model, cutIndex, n + i);
					} else if(CM.cutType.get(cutName) == CM.SRI){
						updateSRI(currentNode, model ,  cutIndex , cutName,  n + i);
					}
				}
				model.update();
			}
		}


		model.optimize();
		MPvars = pool.size();
		int probStatus = model.get(GRB.IntAttr.Status);
		if (probStatus != GRB.INFEASIBLE) {

			aBound = model.get(GRB.DoubleAttr.ObjVal);
			for (int i = 0; i < n; i++) {
				pi[i] = model.getConstrs()[i].get(GRB.DoubleAttr.Pi);
//				System.out.print(Rounder.round3Dec(pi[i+1])+"/");
				if(y_minus[i].get(GRB.DoubleAttr.X)>0 || y_plus[i].get(GRB.DoubleAttr.X)>0){
					CG.stabilityOptimal = false;
				}
			}
//			System.out.println();
			updateStabilization();
		}
		return probStatus;
	}

	
	private void updateStabilization() throws GRBException {
		
		double lambda = 0.2;
		
		for (int i = 0; i < DataHandler.numLegsToSolve; i++) {
			
			if (pi[i] >= d_plus[i]) {
				boxcenter[i] = pi[i] * lambda + (1 - lambda) * boxcenter[i];
				d_plus[i] = boxcenter[i] + boxlengh * 0.5;
				d_minus[i] = boxcenter[i] - boxlengh * 0.5;
				e_plus[i] = e_plus[i] * 1.1;
			} else if (pi[i] <= d_minus[i]) {
				boxcenter[i] = pi[i] * lambda + (1 - lambda) * boxcenter[i];
				d_plus[i] = boxcenter[i] + boxlengh * 0.5;
				d_minus[i] = boxcenter[i] - boxlengh * 0.5;
				e_minus[i] = e_minus[i] * 1.1;
			}else{
				e_minus[i] = Math.max(e_minus[i] * 0.9, 0.5);
				e_plus[i] = Math.max(e_plus[i] * 0.9, 0.5);
			}

			if (CG.stabilize == false) {
				e_plus[i] = 0;
				e_minus[i] = 0;
//				d_plus[i] = Double.POSITIVE_INFINITY;
//				d_minus[i] = -Double.POSITIVE_INFINITY;
			}
			
			y_plus[i].set(GRB.DoubleAttr.Obj, d_plus[i]);
			y_minus[i].set(GRB.DoubleAttr.Obj, -d_minus[i]);
			y_minus[i].set(GRB.DoubleAttr.UB, e_minus[i]);
			y_plus[i].set(GRB.DoubleAttr.UB, e_plus[i]);
//			System.out.print(boxcenter[i]+"/");
		}
//		System.out.println();
	}
	
	
	
	private void updateVehiclesCut(GRBModel model, int cutIndex, int ctrIndex) throws GRBException {
		int start =CM.cuts.get(cutIndex).size();
		for (int k = start; k < model.getVars().length; k++) {
			CM.cuts.get(cutIndex).addTerm(1, model.getVar(k));
			model.chgCoeff(model.getConstr(ctrIndex), model.getVar(k), 1);
		}
		
	}


	private void updateSRI(BBNode currentNode, GRBModel model, int cutIndex, String cutName, int ctrIndex) throws GRBException {
		int[] set = CM.cutQSets.get(cutName);
		if(pool.size()!=model.getVars().length){
			System.err.println("ESTO NO DEBER�A ASAR EN EL UPdate de SRI");
		}
		if (set!=null) {
			ArrayList<Integer> routes = CM.cutQSetsRoutes.get(cutName);
			int lastRoute = routes.get(routes.size()-1);
			for (int k = lastRoute+1; k < pool.size() ; k++) {
				ArrayList<Integer> dummyPath = pool.get(k);
				int counter = 0;
				for (int j = 0; j < set.length; j++) {
					int node = set[j];
					if (dummyPath.contains(node)) {
						counter++;
					}
					if(counter>=2){
						j=10000000;
						CM.cutQSetsRoutes.get(cutName).add(k);
						CM.cuts.get(cutIndex).addTerm(1, model.getVar(k));
						model.chgCoeff(model.getConstr(ctrIndex), model.getVar(k), 1);
					}
				}
				
			}
			
		}else{
			System.err.println(LP_Manager.class + " updateSRI() -> Missing set of a SRI");
		}
		
	}


	/**
	 * This method gets the problem basic (non degenerate) variables information.<\n> 
	 * basicIndexes -> indexes of the variable in the pool of paths.\n 
	 * basicVariables -> Variable object from mp_vars.\n 
	 * basicVarablesVal-> numerical value at the current solution. 
	 * @throws GRBException
	 */
	public void getMPRelaxSolution(GRBModel model) throws GRBException {
		
		double suma = 0.0;
		basicIndexes.clear();
		ceroRCIndexes.clear();
		basicVariables.clear();
		basicVariablesVals.clear();

		for (int i = 0; i < mp_vars.size(); i++) {
			double rc = 1;
			try {
				 rc = mp_vars.get(i).get(GRB.DoubleAttr.RC);
			} catch (Exception e) {
//				System.out.println("La variable que jode es la " + i + " hay en el pool: " + pool.size()+"el prob status es igual " + model.get(GRB.IntAttr.Status));
			}

			double val = mp_vars.get(i).get(GRB.DoubleAttr.X);
			if (val > 0) {
				suma = Rounder.round9Dec(suma + val);
				basicIndexes.add(i);
				basicVariables.add(mp_vars.get(i));
				basicVariablesVals.add(val);
			}
			if (rc <= 0.000001) {
				ceroRCIndexes.add(i);
			}
		}
		sumXi = suma;
	}
	
	/**
	 * This method saves the value of the x_ij variables. In this case, of the time-space network
	 * of related to the subproblem of the ACPP
	 * @param currentNode The node of the B&B tree
	 * @throws GRBException If the value of MP variables is not available .get(GRB.DoubleAttr.X);
	 * @return true if the x_ij variables are feasible (i.e. x_ij \in {0,1})
	 */
	public boolean saveX_ijVariables(BBNode currentNode) throws GRBException{
		X_ijVars = new ArrayList<>(Network.numArcs);
		X_ij = new Hashtable<>(Network.numArcs);
		for (int j = 0; j < basicIndexes.size(); j++) {
			int i = basicIndexes.get(j);
			double val= basicVariables.get(j).get(GRB.DoubleAttr.X);
			updateX_ij(i, val, currentNode);
		}
		for (int i = 0; i < X_ijVars.size(); i++) {
			double val = X_ij.get(X_ijVars.get(i));
			//TODO revisar esta condici�n
			if(val<=0.999 || val >=1.0001){
				return false;
			}
		}
		return true;
	}
	
	
	public ArrayList<Integer> getBasicsIndexes(){
		return basicIndexes;
	}
	


	public void setUpModel(BBNode current) throws GRBException {
		addCurrentCuts(current,current.model, 0);
//		for (int i = 0; i < current.branchdXijRoutesInLB.size(); i++) {
//			for (int j = 0; j < current.branchdXijRoutesInLB.get(i).size(); j++) {
//				current.model.getVar(current.branchdXijRoutesInLB.get(i).get(j)).set(GRB.DoubleAttr.UB, 0.0);
//			}
//		}
		for (int i = 0; i < current.routesInLB.size(); i++) {
			mp_vars.get(current.routesInLB.get(i)).set(GRB.DoubleAttr.UB, 0.0);
//			current.model.getVar(current.routesInLB.get(i)).set(GRB.DoubleAttr.UB, 0.0);
		}
		current.model.update();
//		setupModelForQ(current);
	}
	
	
//	public void setupModelForQ(BBNode current) throws GRBException{
//		if (mu_q==null) {
//			mu_q = new ArrayList<>();
//		}
//		GraphManager.QsetsUtilizationMT = new int[mu_q.size()][DataHandler.numThreads+1];
//		for (int i = 0; i < GraphManager.customers.length; i++) {
//			GraphManager.customers[i].Q_i.clear();
//		}
//		int counter = 0;
//		for (int i = DataHandler.n; i < current.model.getConstrs().length; i++) {
//			String nam = current.model.getConstrs()[i].get(GRB.StringAttr.ConstrName);
//			if (CM.cutType.get(nam)==CM.SRI) {
//				int[] set_q = CM.cutQSets.get(nam);
//				for (int j = 0; j < set_q.length; j++) {
//					int node = set_q[j];
//					GraphManager.customers[node].Q_i.add(counter);
//				}
//				counter++;
//			}
//		}
//	}


	public void setUpNetwork(BBNode currentNode) {
		DataHandler.forbidden = new int[Network.numArcs];
		for (int i = 0; i <  currentNode.branchedXij.size(); i++) {
			int arc_id = currentNode.branchedXij.get(i);
			int branchedValue = currentNode.branchedXijValue.get(i);
			if(branchedValue==0){
				//The arc of the branch is set to 0, then it is forbidden
				DataHandler.forbidden[arc_id] = 1;
			}else{
				int n_i = network.getArc(arc_id).get_v_i().id;
				int n_j = network.getArc(arc_id).get_v_j().id;
				int n_arc_id = -1;
				// forbid xij' arcs
				ArrayList<Integer> magicIndex_v_i = network.getNodes().get(n_i).MagicIndex;
				
				for (int k = 0; k < magicIndex_v_i.size(); k++) {
					n_arc_id = magicIndex_v_i.get(k);
					if (network.getArc(n_arc_id).get_v_j().id != n_j) {
						DataHandler.forbidden[n_arc_id] = 1;
					}
				}
				
				// forbid xij' arcs
				ArrayList<Integer> rMagicIndex_v_j = network.getNode(n_j).rMagicIndex;
				for (int k = 0; k < rMagicIndex_v_j.size(); k++) {
					n_arc_id = rMagicIndex_v_j.get(k);
					if (network.getArc(n_arc_id).get_v_i().id != n_i) {
						DataHandler.forbidden[n_arc_id] = 1;
					}
				}
			}
		}
	}


	
	/**
	 * This methods sums up the dual variables related to the vehicle cut/branching
	 * @param current the node that is being solved
	 * @return the dual charge of the vehicles cut
	 * @throws GRBException
	 */
	public double getVehCutDuals(BBNode current, GRBModel model) throws GRBException {
		double shift=0.0;
		String cutname;
		int cutType = -1;
		for (int i = DataHandler.numLegsToSolve; i < model.getConstrs().length; i++) {
			cutname = model.getConstrs()[i].get(GRB.StringAttr.ConstrName);
			cutType = CM.cutType.get(cutname);
			if(cutType==CM.VEHICLES_CUT ){
				shift+=model.getConstrs()[i].get(GRB.DoubleAttr.Pi);
			}
		}
//		System.out.println("shift: =  " + shift);
		return shift;
	}

	/**
	 * Generates a vehicle cut/branching, i.e., the sum of all variables must respect and integer value of vehicles 
	 * @return The gurobi expression
	 */
	public GRBLinExpr genVehCut(GRBModel model) {
		GRBLinExpr cut = new GRBLinExpr();
		
		int numVars = model.getVars().length;
		for (int i = 0; i < numVars; i++) {
			cut.addTerm(1, model.getVar(i));
		}
		return cut;
	}

	/**
	 * this method branches on a xij fractional variables. The method selects one x_ij fractional variable and 
	 * generates two arraylist of integers that contains the indexes of the master problem (MP) variables that need to be set to zero.
	 * For branch x_ij = 0, the corresponding MP variables are those that the associated path contains the arc.
	 * For branch x_ij = 1, the corresponding MP variables are those that the associated path contains the arcs (i',j) and (i,j').
	 * The variable x_ij is chose according to the branch policy given as parameter. 
	 * @param currentBBNode The branch and bound node that is being branched
	 * @param branchPolicy the branching policy
	 * @param model the gurobi model
	 * @return two lists of the routes that must be set to zero, one for each branch.
	 */
	public ArrayList<Integer>[] genXijBranching(BBNode currentBBNode, int branchPolicy) {
		boolean selected = false;
		int arc_id = -1;
		double minVarVal = 0.0;
		if (X_ijVars != null && X_ij.size() > 0) {
			if (BPC_Algorithm.BP_RANDOM_BRANCHING == branchPolicy) {
				
			} else if (BPC_Algorithm.BP_PSEUDOCOST_BRANCHING == branchPolicy) {


			} else if(BPC_Algorithm.BP_STRONG_BRANCHING  == branchPolicy){
				
			}else if(BPC_Algorithm.BP_MOST_INFEASIBLE_BRANCHING == branchPolicy){			
				double minDesVsdest= Double.POSITIVE_INFINITY;
				double centerVal = 0.1;
				for (int i = 0; i < X_ijVars.size() ; i++) {
					int testing_arc_id  = X_ijVars.get(i);
					double varVal = (X_ij.get(testing_arc_id));
					if (varVal < 1 && varVal > 0
							&& Math.abs(varVal - centerVal) < minDesVsdest
							&& network.getArc(testing_arc_id).get_v_i().id != Network.source
							&& network.getArc(testing_arc_id).get_v_j().id != Network.sink
							&& network.getArc(testing_arc_id).get_v_j().getTime()!=Nodo.NODE_TYPE_OPP){
//							&& X_ij_arc.get(strInd).getType()!=Arco.TYPE_FIGHT
//							&& X_ij_arc.get(strInd).getType()==Arco.TYPE_DEADHEAD) {
						minDesVsdest =Math.abs(varVal - centerVal) ; 
						minVarVal = varVal;
						arc_id = X_ijVars.get(i);
					}
				}
			}
			//Start the expression build up
			if (arc_id==-1) {
				System.out.println(X_ijVars);
				System.out.println(X_ij);
			}
			System.out.println(arc_id + "-> " + minVarVal + " \t " + network.getArc(arc_id));
			System.out.println(basicIndexes);
			int n_i = network.getArc(arc_id).get_v_i().id;
			int n_j =network.getArc(arc_id).get_v_j().id;
			currentBBNode.xijBranchInThisNode = arc_id;
			
			//left expression, xij=0. All routes using the arc i,j come here
			ArrayList<Integer> routesToLB_left = new ArrayList<>();
			ArrayList<Integer> routesUsingXij = routes_per_arc[arc_id];
			for (int i = 0; i < routesUsingXij.size(); i++) {
				int number  = routesUsingXij.get(i);
				if (number < MPvars) {
					routesToLB_left.add(number);
				}else{
					System.err.println("BRANCIHN ERROR Xij");
				}
			}
		
			//Right expression, xij=1. All routes using (i,j') and (i',j) come here
			ArrayList<Integer> routesToLB_right = new ArrayList<>();
			ArrayList<Integer> routesNOTUsingXij;
			int NOTUsing_arc_id = -1;
			
			// Include xij' routes
			ArrayList<Integer> magicIndex_v_i = network.getNodes().get(n_i).MagicIndex;
			for (int k = 0; k < magicIndex_v_i.size(); k++) {
				NOTUsing_arc_id = magicIndex_v_i.get(k);
				routesNOTUsingXij = routes_per_arc[NOTUsing_arc_id];
				if (network.getArc(NOTUsing_arc_id).get_v_j().id != n_j && routesNOTUsingXij != null) {
					for (int i = 0; i < routesNOTUsingXij.size(); i++) {
						int number = routesNOTUsingXij.get(i);
						if (number < MPvars) {   
							routesToLB_right.add(number);
						}
					}
				}
			}
			
			// Include xi'j routes
			ArrayList<Integer> rMagicIndex_v_j = network.getNode(n_j).rMagicIndex;
			for (int k = 0; k < rMagicIndex_v_j.size(); k++) {
				NOTUsing_arc_id = rMagicIndex_v_j.get(k);
				routesNOTUsingXij = routes_per_arc[NOTUsing_arc_id];
				if (network.getArc(NOTUsing_arc_id).get_v_i().id != n_i && routesNOTUsingXij != null) {
					for (int i = 0; i < routesNOTUsingXij.size(); i++) {
						int number = routesNOTUsingXij.get(i);
						if (number < MPvars) {
							routesToLB_right.add(number);
						}
					}
				}
			}

			Sort(routesToLB_left);
			Sort(routesToLB_right);
			ArrayList[] expresiones = { routesToLB_left, routesToLB_right };
			return expresiones;

		} else {
			System.out.println("No vars or there is a mistake");
		}
		return null;

	}


	/**
	 * Delete all cuts and branching to be able to update the master problem for the next node.
	 * @throws GRBException at removing a given constraint or at the model update.
	 */
	public void cleanModel( BBNode currentNode ,GRBModel model) throws GRBException {
		int numConts = model.getConstrs().length;
		for (int i = numConts-1; i >=DataHandler.numLegsToSolve; i--) {
			GRBConstr crt= model.getConstr(i);
			model.remove(crt);
		}
		model.update();
		
//		for (int i = 0; i < current.branchdXijRoutesInLB.size(); i++) {
//			for (int j = 0; j < current.branchdXijRoutesInLB.get(i).size(); j++) {
//				current.model.getVar(current.branchdXijRoutesInLB.get(i).get(j)).set(GRB.DoubleAttr.UB, 1);
//			}
//		}
		
		
		for (int i = 0; i < currentNode.routesInLB.size(); i++) {
			int routeID = currentNode.routesInLB.get(i);
			if (routeID<MPvars) {
				model.getVar(routeID).set(GRB.DoubleAttr.UB, 1);
			}
		}
		model.update();
	
	}

	public void addCurrentCuts(BBNode currentNode,GRBModel model, int currentNumOfCuts) throws GRBException {
		for (int i = currentNumOfCuts; i < currentNode.cutsIndexes.size(); i++) {
			int j =  currentNode.cutsIndexes.get(i);
			model.addConstr(CM.cuts.get(j), CM.cutSense.get(j),CM.RHS.get(j), CM.cutName.get(j));
		}
		model.update();
	}

	public void addCut(BBNode currentNode, GRBModel model, int cutIndex)throws GRBException {
		int j = cutIndex;
		model.addConstr(CM.cuts.get(j), CM.cutSense.get(j), CM.RHS.get(j),CM.cutName.get(j));
	}

	
	private void updateX_ij(int routeIndex, double varValue, BBNode currentNode){

		ArrayList<Integer> pairingArcs = pool_of_arcs.get(routeIndex);
		for (int i = 0; i < pairingArcs.size(); i++) {
			Arco arc = network.getArcs().get(pairingArcs.get(i));
			if(arc.getType() != Arco.TYPE_DEADHEAD){
			int node_i = arc.get_v_i().id;
			int node_j = arc.get_v_j().id;
			
			int key= arc.getId();
//			X_ij_arc.put(key, arc);
			if (X_ij.get(key)!=null) {
				double newVal  = Rounder.round9Dec(X_ij.get(key)+varValue);
				X_ij.put(key, newVal);
			}else{
				X_ijVars.add(key);
				X_ij.put(key,  (varValue));
			}
			if (X_ij.get(key)>1.000001) {
//				System.out.println("\nERROR: La variables x_("+ node_i+ ", "+node_j+") toma un valor mayor a 1: " + X_ij.get(key));
			}
			}
		}

	}
	
	public void addRoutesToArcs(ArrayList<Integer> dummyPath_of_Arcs, Leg leg) {
		if(dummyPath_of_Arcs!=null){
			for (int j = 0; j < dummyPath_of_Arcs.size()-1; j++) {
				int arc_id = dummyPath_of_Arcs.get(j);
				if (routes_per_arc[arc_id] == null) {
					routes_per_arc[arc_id] = new ArrayList<Integer>();
				}
				routes_per_arc[arc_id].add(pool.size());
			}
		}else{
			//if is null it's coming from the initialization and a single leg 
			//pairing (even if fictional) must be set up
			int depNode = leg.getDepNodeId();
			int arrNode = leg.getArrNodeId();
			int arc_id = network.getArcByNodes(depNode,arrNode,Arco.TYPE_FIGHT).getId();
			routes_per_arc[arc_id] = new ArrayList<>();
			routes_per_arc[arc_id].add(pool.size());
		}
	}
	
	
	//TODO ese esta mal porque los nodos no son legs
	public void addRoutesToNodes(ArrayList<Integer> dummyPath) {
		for (int j = 1; j < dummyPath.size()-1; j++) {
			int v_i = dummyPath.get(j);
			if(routes_per_node[v_i] == null){
				routes_per_node[v_i] = new ArrayList<>();
			}
			routes_per_node[v_i].add(pool.size());
		}
	}


	public ArrayList<GRBVar> getBasicVariables() {
		return basicVariables;
	}


	public double getSumOfXi() {
		return sumXi;
	}


	public void printXij() {
		for (int i = 0; i < X_ijVars.size(); i++) {
			System.out.println(i+"\t X("+X_ijVars.get(i)+")= " + X_ij.get(X_ijVars.get(i)));
		}
		
	}
	
	public void Sort(ArrayList<Integer> set) {
		QS(set, 0, set.size() - 1);
	}

	public int colocar(ArrayList<Integer> e, int b, int t) {
		int i;
		int pivote;
		double valor_pivote;
		int temp;

		pivote = b;
		//valor_pivote = DataHandler.pi[e[pivote].id] ;
		valor_pivote = (e.get(pivote)) ;
		for (i = b + 1; i <= t; i++) {
			if (e.get(i) < valor_pivote) {
				pivote++;
				temp = e.get(i);
				e.set(i, e.get(pivote));
				e.set(pivote,temp);
			}
		}
		temp =  e.get(b);
		e.set(b, e.get(pivote));
		e.set(pivote,temp);
		return pivote;
	}

	public void QS(ArrayList<Integer> e, int b, int t) {
		int pivote;
		if (b < t) {
			pivote = colocar(e, b, t);
			QS(e, b, pivote - 1);
			QS(e, pivote + 1, t);
		}
	}
	
	public double getOriginalFOValue() throws GRBException{
		return realFO.getValue();
	}


	public void disableStabilization() throws GRBException {
		for (int i = 0; i < d_plus.length; i++) {
			y_plus[i].set(GRB.DoubleAttr.Obj, d_plus[i]);
			y_minus[i].set(GRB.DoubleAttr.Obj, -d_minus[i]);
			y_minus[i].set(GRB.DoubleAttr.UB, 0);
			y_plus[i].set(GRB.DoubleAttr.UB, 0);
		}
	}
	
	public ArrayList<GRBVar> getMPvars(){
		return mp_vars;
	}
	
	public GRBVar getMPvar(int index){
		return mp_vars.get(index);
	}
	
}

