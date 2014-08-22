package bpc.CG;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;

import java.util.ArrayList;
import java.util.Hashtable;

import ColumnGenrators.PulseAlgorithm;
import Graph.Network;
import IO.DataHandler;
import Utilities.Rounder;
import bpc.BBTree.BBNode;
import bpc.BBTree.BPC_Algorithm;
import bpc.BBTree.CutsManager;

public class CG {

	public static boolean stabilize = false;
	
	
	public static double primalBound = Double.POSITIVE_INFINITY;
	private LP_Manager algorithm;
	private String ini;
	private DataHandler data;
	private  CutsManager CM;
	
	
	public Hashtable<String, Double> routesPoolRC;
	public Hashtable<String, Double> routesPoolDist;
	public ArrayList<String> pool;
	public Hashtable<String, ArrayList<Integer>> netPath;
	public Hashtable<String, ArrayList<Integer>> netPathArcs;
	public Hashtable<String, Integer> generator;
	
	
	long t_end;
	long exactTime;
	double phase1bound;
	int phase1pool;
	
	public static final double rcCriteriaHeuristics = -0.1;
	public static final double rcCriteriaExact = -0.01;
	
	/**
	 * This control parameter is set to <code>true</code> if the subproblem is solved via heuristic procedures.
	 */
	public static boolean phaseI;
	
	public static double minRC;


	public static boolean stabilityOptimal;
	
	private Network network;
	private PulseAlgorithm pulse;
	
	
	public CG(DataHandler nData, Network network, LP_Manager algo,  CutsManager cM, GRBEnv gEnv, GRBModel baseModel) throws GRBException {
		this.CM = cM;
		this.network = network;
		algorithm = algo;
		algorithm.initializePool();
		algorithm.InitializeMP(gEnv , baseModel);
		data = nData;
		ini = "s";
		phaseI = false;
		minRC = -Double.POSITIVE_INFINITY;
		pulse = new PulseAlgorithm(nData, network, algo, this);
		pool = new ArrayList<>();
		routesPoolRC = new Hashtable<>(1000);
		routesPoolDist = new Hashtable<>(1000);
		generator = new Hashtable<>(1000);
		netPath = new Hashtable<>();
		netPathArcs = new Hashtable<>();
	}
	
	
	
	


	/**
	 * Solves the CG procedure for a given node of the B&P&C tree
	 * @param currentNode B&P&C node that is being solved
	 * @return the problem status
	 * @throws GRBException
	 * @throws InterruptedException 
	 */
	public int runCG(BBNode currentNode) throws GRBException, InterruptedException{
		int probStatus =-1;
		if(currentNode.getParent()==null){
			probStatus =solCGRootNode(currentNode);
		} else{
			probStatus = solCG(currentNode);
		}
		if (probStatus!=GRB.INFEASIBLE) {
			
			saveMPSolution(currentNode);
		}
		return  probStatus;
		
	}





	/**
	 * 
	 * @param currentNode
	 * @return
	 * @throws GRBException
	 */
	private void saveMPSolution(BBNode currentNode) throws GRBException {
		boolean bol = algorithm.saveX_ijVariables(currentNode);
		currentNode.feasibleXijflows = bol;
		currentNode.setSum_X_i(algorithm.getSumOfXi());
		currentNode.setBasicCardinality(algorithm.getBasicsIndexes().size());
		currentNode.setBasicVariables(algorithm.getBasicVariables());
		currentNode.objVal = Rounder.round6Dec(currentNode.model.get(GRB.DoubleAttr.ObjVal));
		currentNode.basicIndexes = new ArrayList<Integer>();
		currentNode.basicIndexes.addAll(algorithm.getBasicsIndexes());

	}


		public  void solveIPwithLPColumns(BBNode currentNode) throws GRBException {
		algorithm.disableStabilization();
		for (int i = 0; i <	algorithm.getMPvars().size(); i++) {
			algorithm.getMPvar(i).set(GRB.CharAttr.VType, GRB.BINARY);
		}
		currentNode.model.update();
		currentNode.model.optimize();
		int probStatus = currentNode.model.get(GRB.IntAttr.Status);
		if (probStatus != GRB.INFEASIBLE) {
			double aBound = currentNode.model.get(GRB.DoubleAttr.ObjVal);
			BPC_Algorithm.BB_primalBound = aBound;
			System.out.println("DIO " + aBound + " pairings");
		}
//		for (int i = 0; i < currentNode.model.getVars().length; i++) {
//			currentNode.model.getVars()[i].set(GRB.CharAttr.VType, GRB.CONTINUOUS);
//		}
		currentNode.model.update();
		
	}


	
	private int solCG(BBNode currentNode) throws GRBException, InterruptedException {
		int probStatus=0;
		int iter=0;
		int exactP = 0;
		double shift = 0;
		minRC = -Double.POSITIVE_INFINITY;
		while (!optimal()) {
			iter++;
			probStatus = algorithm.runMP(currentNode, currentNode.model);
			
			if(probStatus!= GRB.INFEASIBLE){
				algorithm.getMPRelaxSolution(currentNode.model);
				network.updateDualInfo(algorithm,data);
				exactP++;
				pulse.run(currentNode, true);

				
				long tend1 = System.currentTimeMillis();
//				System.out.println("ITER " + iter + " PoolSize "+ algorithm.pool.size() + " FO: " + algorithm.aBound+ " \tRC: " + algorithm.minRC + "  TIME : "	+ (tend1 - algorithm.tnow) / 1000.0);
				
			}else{
				return probStatus;
			}

		}
		
		long tend = System.currentTimeMillis();
		double val = algorithm.getSumOfXi();
//		System.out.println("TIME : " + (tend-algorithm.tnow)/1000.0 );
//		String msn = "Intancia: " +ini +"\nTiempo total: " +(tend-algorithm.tnow)/1000.0  +" \nBound: " + algorithm.aBound;
//		msn += " Max iteration " + algorithm.maxIterationsTS + " \n";
////		msn += "Phase I: \t" + " bound: " + algorithm.phase1bound  + "  pool: " + algorithm.phase1pool + " time:  " + (algorithm.t_end-algorithm.tnow)/1000.0 +"\n";
//		msn += "Phase 2: \t" + " bound: " + algorithm.aBound +"  pool: " + algorithm.pool.size()  +"  time " +(tend-algorithm.tnow)/1000.0 +"\n";
//		msn += "Exact problems solved: " +  exactP + " in " +(tend-algorithm.t_end)/1000.0 +"\n";
//		msn += "MP realxation basic variables: " + algorithm.getBasicsIndexes().size()+" sum_i{x_i} = "+ val +"\n";
//		System.out.println(msn);
		
		return probStatus;
		//exact
		//MailSender.mandarCorreoUsuario("d.duque25@gmail.com",  ini,msn , "d.duque25@gmail.com");
		//MailSender.mandarCorreoUsuario("d.duque25@gmail.com",  ini,msn ,"leo-loza@uniandes.edu.co");
		
		
	}

	/**
	 * CG procedure for the root node of the B&P&C tree.
	 * @param currentNode root node
	 * @return the problem status
	 * @throws GRBException
	 * @throws InterruptedException
	 */
	private int solCGRootNode(BBNode currentNode) throws GRBException, InterruptedException {
		double totalExactPulseTime=0;
		double totalHeuPulseTime=0;
		double totalMPTime = 0;
		int probStatus=0;
		int iter=0;
		int exactP = 0;
		minRC = -Double.POSITIVE_INFINITY;
		cgSetUp();
		phaseI = false;
		while (!optimal()) { 
			stabilityOptimal= true;
			iter++;
			double tnowMP = System.currentTimeMillis();
			probStatus = algorithm.runMP(currentNode, currentNode.model);
			totalMPTime += System.currentTimeMillis()-tnowMP;
			algorithm.getMPRelaxSolution(currentNode.model);
			network.updateDualInfo(algorithm,data);
			if (phaseI){
				double tnowPulse = System.currentTimeMillis();
				pulse.run(currentNode, false);
				totalHeuPulseTime += System.currentTimeMillis()-tnowPulse;
				long tend1 = System.currentTimeMillis();
//				System.out.println("ITER "+iter+" PoolSize "+algorithm.pool.size()+" FO: "+algorithm.aBound+ " \tOrgFO: " +algorithm.getOriginalFOValue() +
//						"\tRC: "+minRC+  "  TIME : " + (tend1-algorithm.tnow)/1000.0 
//						+ " \tMP: " + totalMPTime/1000.0 + " SP: " + totalHeuPulseTime/1000.0 + " \tphase I:" + phaseI+ " stab: " + stabilize);
			}
			else {
				exactP ++;
				double tnowPulse = System.currentTimeMillis();
				pulse.run(currentNode, true);
				totalExactPulseTime += System.currentTimeMillis()-tnowPulse;
				long tend1 = System.currentTimeMillis();
//				System.out.println("ITER "+iter+" PoolSize "+algorithm.pool.size()+" FO: "+algorithm.aBound+ " \tOrgFO: " +algorithm.getOriginalFOValue() +
//						"\tRC: "+minRC+  "  TIME : " + (tend1-algorithm.tnow)/1000.0 
//						+ " \tMP: " + totalMPTime/1000.0 + " SP: " + totalExactPulseTime/1000.0 + " \tphase I:" + phaseI+ " stab: " + stabilize);
			}
		}
		
		long tend = System.currentTimeMillis();
		double val = algorithm.getSumOfXi();
		System.out.println("TIME : " + (tend-algorithm.tnow)/1000.0 );
		String msn = "Intancia: " +ini +"\nTiempo total: " +(tend-algorithm.tnow)/1000.0  +" \nBound: " + algorithm.aBound;
		msn += " Max iteration " + LP_Manager.maxIterations + " \n";
		msn += "Phase I: \t" + " bound: " + algorithm.phase1bound  + "  pool: " + algorithm.phase1pool + " time:  " + (algorithm.t_end-algorithm.tnow)/1000.0 +"\n";
		msn += "Phase 2: \t" + " bound: " + algorithm.aBound +"  pool: " + algorithm.pool.size()  +"  time " +(tend-algorithm.tnow)/1000.0 +"\n";
		msn += "Exact problems solved: " +  exactP + " in " +(tend-algorithm.t_end)/1000.0 +"\n";
		msn += "MP realxation basic variables: " + algorithm.getBasicsIndexes().size()+" sum_i{x_i} = "+ val +"\n";
		System.out.println(msn);
		
		//exact
		//MailSender.mandarCorreoUsuario("d.duque25@gmail.com",  ini,msn , "d.duque25@gmail.com");
		//MailSender.mandarCorreoUsuario("d.duque25@gmail.com",  ini,msn ,"leo-loza@uniandes.edu.co");
		return probStatus;
		
	}

	public boolean optimal() throws GRBException {

		if (minRC > rcCriteriaExact && !phaseI && stabilityOptimal) {
			return true;
		} else if (minRC > rcCriteriaHeuristics && phaseI) {
			phaseI = false;
			stabilize = true;
			algorithm.initializeStabilizationParameters();
			t_end = System.currentTimeMillis();
			phase1bound = algorithm.aBound;
			phase1pool = algorithm.pool.size();
			return false;
		}else if(minRC >  rcCriteriaHeuristics  && !phaseI && !stabilityOptimal){
			stabilize = false;
			return false;
		}
		return false;
	}

	

	private void cgSetUp() {
		phaseI = true;
	}

	


	/**
	 * Call the algorithm to generate the branch over a x_ij variables. 
	 * Left branch is:  \sum{k\in\Ompega'}(B_{i,j,k}*x_k) <=0
	 * Right branch is: \sum{k\in\Ompega'}(\sum{i'\in\N}(B_{i',j,k}*x_k)  + \sum{j'\in\N}(B_{i,j',k}*x_k)) <=0
	 * @param currentBBNode the BBNode that is fractional
	 * @param branchPolicy branch policy 
	 * @return
	 */
	public ArrayList<Integer>[] genXijBranching(BBNode currentBBNode, int branchPolicy) {
		return algorithm.genXijBranching(currentBBNode, branchPolicy);
	}
	
	
	public void Sort(ArrayList<String> set) {
		QS(set, 0, set.size() - 1);
	}

	public int colocar(ArrayList<String> e, int b, int t) {
		int i;
		int pivote;
		double valor_pivote;
		String temp;

		pivote = b;
		//valor_pivote = DataHandler.pi[e[pivote].id] ;
		valor_pivote = routesPoolRC.get(e.get(pivote)) ;
		for (i = b + 1; i <= t; i++) {
			if (routesPoolRC.get(e.get(i))<valor_pivote) {
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

	public void QS(ArrayList<String> e, int b, int t) {
		int pivote;
		if (b < t) {
			pivote = colocar(e, b, t);
			QS(e, b, pivote - 1);
			QS(e, pivote + 1, t);
		}
	}


	/**
	 * @return the routesPoolRC
	 */
	public Hashtable<String, Double> getRoutesPoolRC() {
		return routesPoolRC;
	}


	/**
	 * @return the routesPoolDist
	 */
	public Hashtable<String, Double> getRoutesPoolDist() {
		return routesPoolDist;
	}


	/**
	 * @return the pool
	 */
	public ArrayList<String> getPool() {
		return pool;
	}

	/**
	 * @return the generator
	 */
	public Hashtable<String, Integer> getGenerator() {
		return generator;
	}

	public void resetPool() {

		pool = new ArrayList<>();
		generator = new Hashtable<>();
		routesPoolRC = new Hashtable<>(1000);
		routesPoolDist = new Hashtable<>(1000);
		netPath = new Hashtable<>(1000);
		netPathArcs = new Hashtable<>(1000);
	}


}
