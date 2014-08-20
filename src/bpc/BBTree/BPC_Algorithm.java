package bpc.BBTree;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;

import java.util.ArrayList;

import Graph.Network;
import IO.DataHandler;
import IO.Leg;
import bpc.CG.CG;
import bpc.CG.LP_Manager;
import bpc.CuttingGenerators.SubSetRowInequalities;


public class BPC_Algorithm {

	public static final String  INFEASIBLE = "infeasible";
	public static final String  BOUND = "bound";
	public static final String  OPTIMALITY = "optimality";
	
	public static int nodeSelection;
	/**
	 * Best first search.
	 */
	public static final int ns_BFS = 1001;
	/**
	 * Depth first search.
	 */
	public static final int ns_DFS = 1002;
	/**
	 * DSF until a integer solution is found.
	 */
	public static final int ns_HYB = 1003;
	
	/**
	 * Select the best candidate and goes DFS until it is not possible to keep branching. Afterwards,
	 * another good candidate is selected.
	 */
	public static final int ns_BLS = 1004;
	
	/**
	 * Policy for branching variables (see {@link BPC_Algorithm} BP_ constants ):
	 */
	public static int currentBranchPolicy;
	public static final int BP_STRONG_BRANCHING=2001;
	public static final int BP_PSEUDOCOST_BRANCHING=2002;
	public static final int BP_RANDOM_BRANCHING=2003;
	public static final int BP_MOST_INFEASIBLE_BRANCHING=2004;
	
	
	
	
	//Cutting attributes and parameters
	private CutsManager CM;
	private CG cgSolver;
	private LP_Manager algorithm;
	private BBNode current;
	
	//Subset row inequalities
	public static final double minViolation = 0.1;
	public static final int maxSRIcuts= 50;
	
	
	public ArrayList<BBNode> activeNodes;
	public ArrayList<BBNode> nonActiveNodes;
	public ArrayList<BBNode> nodes;
	 
	public BBNode root;
	public BBNode bestNode;
	
	public static double BB_primalBound;
	public static double BB_bestbound;
	public int intSols;
	public int improvedIntSols;
	
	public GRBEnv globalEnv;
	public GRBModel baseModel;
	
	public SubSetRowInequalities subsetCuts;
	
	private DataHandler data;
	
	private Network network;
	
	public BPC_Algorithm(DataHandler data, Network network) throws GRBException {
		this.data = data;
		globalEnv = new GRBEnv(null);
		globalEnv.set(GRB.IntParam.OutputFlag, 0);
		globalEnv.set(GRB.DoubleParam.MIPGap, 0.05);
		globalEnv.set(GRB.DoubleParam.TimeLimit, 300);
//		globalEnv.set(GRB.IntParam.MIPFocus, 3);
//		globalEnv.set(GRB.IntParam.Method, 1);
		globalEnv.set(GRB.IntParam.Threads, 2);
//		globalEnv.set(GRB.IntParam.Presolve, 2);
//		globalEnv.set(GRB.IntParam.Cuts, 3);
//		globalEnv.set(GRB.DoubleParam.Heuristics, 0.5);
		
		
		baseModel = new GRBModel(globalEnv);
		CM = new CutsManager();
		algorithm = new LP_Manager(CM, data, network);
		cgSolver = new CG(data, network, algorithm, CM, globalEnv, baseModel);
		this.network = network;
		//control parameters
		currentBranchPolicy = BP_MOST_INFEASIBLE_BRANCHING;
		nodeSelection =  ns_BLS;
		
		//B&B info
		BB_primalBound = Double.POSITIVE_INFINITY;
		BB_bestbound = Double.POSITIVE_INFINITY;
		root = new BBNode(0, null, baseModel,1);
		activeNodes = new ArrayList<BBNode>();
		nonActiveNodes = new ArrayList<BBNode>();
		nodes = new ArrayList<BBNode>();
		activeNodes.add(root);
		nodes.add(root);
		
		subsetCuts = new SubSetRowInequalities(baseModel, algorithm, minViolation);
		
	}
	
	/**
	 * Solves the Integer ACPP, i.e., call the branch & price & cut procedure.
	 * @throws GRBException Model problems
	 * @throws InterruptedException 
	 */
	public void solveIP() throws GRBException, InterruptedException{
		
		int iter = 0;
		while(activeNodes.size()>0){
			iter++;
			current = selectNode(activeNodes); //Select a node according to the nodeSelection policy
			current.updateRouteInLB(algorithm, network);
			algorithm.setUpNetwork(current); //"build" the sub-problem network for this node
			algorithm.setUpModel(current);//"build" the master problem for this node
			
			int  probStatus = cgSolver.runCG(current);
			
			baseModel.getEnv().set(GRB.IntParam.OutputFlag, 1);
			cgSolver.solveIPwithLPColumns(current);
			algorithm.getMPRelaxSolution(current.model);
			current.basicIndexes = new ArrayList<Integer>();
			current.basicIndexes.addAll(algorithm.getBasicsIndexes());
			current.printSOl(data);
	

//			branch(probStatus, iter);
//			current.printCurrentPairings(data);
			printAdvance(iter);
			if (activeNodes.size() > 0) {
				BB_bestbound = activeNodes.get(activeNodes.size() - 1).sortCriteria;
			}
			algorithm.cleanModel(current, current.model);
			break;
		}
		
		
		System.out.println("SOLVE TO OPTIMALIY!!!!!");
		System.out.println( " UB: " + BB_primalBound );
//		System.out.println("Optimal Node " + bestNode.id);
//		System.out.println(bestNode.toString(CM));
//		bestNode.printSOl(data);
	}

	
	
	private boolean  cut(int probStatus) throws GRBException, InterruptedException {

//		int number_of_added_cuts = subsetCuts.evaluateSetsAll(CM, current, maxSRIcuts);
//		if (number_of_added_cuts > 0) {
//			current.model.update();
//			algorithm.setupModelForQ(current);
//			current.updateRouteInLB(algorithm);
//			probStatus = cgSolver.runCG(current);
//			return true;
//		} else {
//			return false;
//		}
//		TODO ESTO FUE COMENTADO PARA CORRER SIN CORTES; PERO ESTA BIEN
		return false;
	}

	

	/**
	 * Given the last problem status, branch in the variables of the current BBNode.
	 * @param probStatus The problem status of master problem related to the current BBNode
	 * @throws GRBException 
	 * @throws InterruptedException 
	 */
	private void branch(int probStatus, int iter) throws GRBException, InterruptedException {
			boolean reOptNode = true;
			while (reOptNode) {
				if (probStatus == GRB.INFEASIBLE) {
					current.problemStatus = INFEASIBLE;
					reOptNode = false;
					System.out.println("B&P - pruned by Infeasibility");
				} else if (current.objVal >= BB_primalBound) {
					current.problemStatus = BOUND;
					reOptNode = false;
					System.out.println("B&P - pruned by bound ");
				} else if (current.isInteger()) {
					intSols++;
					if (current.objVal < BB_primalBound) {
						improvedIntSols++;
						bestNode = current;
						BB_primalBound = current.objVal;
						reOptNode = false;
						System.out.println("INT sol!!! " + BB_primalBound);
					} else {
						System.err.println("ESTO\n No \n deberia \n PASAR !!!! \n BRANCHHHH ERRORRRR!!!!!!!!!!!!!!!!!!");
					}
				} else {
//					subsetCuts.cycleSepAlgorithm();
//					reOptNode =cut(probStatus);
					reOptNode=false;
			
					if (!reOptNode) {
						branchInX_ijVars();
						reOptNode = false;
			
					}
					
				}

		}
		current.active = false;
		activeNodes.remove(current);
		nonActiveNodes.add(current);

	}

	private void branchInX_ijVars() throws GRBException {
		ArrayList<Integer>[] branchingListOfRoutes =  cgSolver.genXijBranching(current, currentBranchPolicy);
		
		//Left
		BBNode leftChild = new BBNode(nodes.size(), current, current.model,0);
		leftChild.parent=current;
		current.leftChild = leftChild;
		addBBNode(leftChild);
		leftChild.AddAllNewRoutesInLB(branchingListOfRoutes[0]);
		leftChild.branchedXij.add(current.xijBranchInThisNode);
		leftChild.branchedXijValue.add(0);
		
		//Right 
		BBNode rightChild = new BBNode(nodes.size(), current, current.model,1);
		rightChild.parent=current;
		current.rightChild = rightChild;
		addBBNode(rightChild);
		rightChild.AddAllNewRoutesInLB(branchingListOfRoutes[1]);
		rightChild.branchedXij.add(current.xijBranchInThisNode);
		rightChild.branchedXijValue.add(1);

	}


	private void addBBNode(BBNode bbNode) {
		nodes.add(bbNode);
		double mVal;
		
		if(activeNodes.size() == 0){
			activeNodes.add(bbNode);
			return;
		}else if(activeNodes.size() == 1){
			mVal = activeNodes.get(0).sortCriteria;
			if( bbNode.sortCriteria> mVal){
				activeNodes.add(0, bbNode);
			}else if(bbNode.sortCriteria  < mVal){
				activeNodes.add(1, bbNode);
			}else{
				if(bbNode.up < activeNodes.get(0).up){
					activeNodes.add(0, bbNode);
				}else if(bbNode.up > activeNodes.get(0).up){
					activeNodes.add(1,bbNode);
				}else{
					activeNodes.add(1,bbNode);
				}
				
			}
			return;
		}else{
			boolean cond = true;
			int l = 0;
			int r = activeNodes.size();
			int m = (int) ((l + r) / 2);
			mVal = activeNodes.get(m).sortCriteria;
			double cScore = bbNode.sortCriteria;
			while(cond){
				if(r-l>1){
					if (cScore > mVal) {
						r = m;
						m = (int) ((l + r) / 2);
					} else if (cScore < mVal) {
						l = m;
						m = (int) ((l + r) / 2);
					} else if (bbNode.up < activeNodes.get(m).up) {
						r = m;
						m = (int) ((l + r) / 2);
					} else if (bbNode.up > activeNodes.get(m).up) {
						l = m;
						m = (int) ((l + r) / 2);
					}else if(bbNode.id > activeNodes.get(m).id){
						r = m;
						m = (int) ((l + r) / 2);
					}else if(bbNode.id < activeNodes.get(m).id){
						l = m;
						m = (int) ((l + r) / 2);
					}
					else {
						activeNodes.add(m, bbNode);
						System.out.println("Hay un empate RARO!!!!!!!");
						//System.err.println(VertexPulse.class + " method: inser label: \n" + " this should not happend because for 2 paths with the same dist there is a dominance test" );
						return;
					}
					mVal = activeNodes.get(m).sortCriteria;
				}else{
					cond = false;
					if (l == m) {
						if(cScore == mVal)
						{
							if(bbNode.up==activeNodes.get(m).up){
								activeNodes.add(bbNode.id > activeNodes.get(m).id ? l : l + 1, bbNode);
							}else{
								activeNodes.add(bbNode.up < activeNodes.get(m).up ? l : l + 1, bbNode);
							}
							
						}else{
							activeNodes.add(cScore > mVal ? l : l + 1, bbNode);
						}					
					} else if (r == m) {
						System.out.println("esto no pasa !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11 LSET ALGO ");
//						L.add(cScore > mVal ? r : Math.min(r + 1, L.size()), np);
					} else {
//						System.err.println(VertexPulse.class +  " insert label, error");
					}
					return;
				}
			}
			
		}
		
	}

	/**
	 * Selects one of the active nodes of the B&P&C tree to be solve
	 * @param activeNodes list of all B&P&C nodes
	 * @return the selected B&B node to be solved
	 */
	private BBNode selectNode(ArrayList<BBNode> activeNodes) {
		if(activeNodes.size()==1){
			return activeNodes.get(0);
		}else{
			if(nodeSelection  ==  ns_BFS){
				return activeNodes.get(activeNodes.size() - 1);
			} else if (nodeSelection == ns_DFS) {
				BBNode selected = null;
				if (current.rightChild != null && current.rightChild.active) {
					selected = current.rightChild;
				} else if (current.leftChild != null && current.leftChild.active) {
					selected = current.leftChild;
				} else {
					selected = current.searchNextNode();
				}
				if(selected == null){
					return activeNodes.get(activeNodes.size() - 1); 
				}
				return selected;
			}else if (nodeSelection == ns_BLS) {
				BBNode selected = null;
				if (current.rightChild != null && current.rightChild.active) {
					selected = current.rightChild;
				} else if (current.leftChild != null && current.leftChild.active) {
					selected = current.leftChild;
				} else {
					selected = activeNodes.get(activeNodes.size() - 1);
				}
				return selected;
			}
			else {
				if (BB_primalBound<Double.POSITIVE_INFINITY) {
					return activeNodes.get(activeNodes.size() - 1);
				}else{
					BBNode selected = null;
					if (current.rightChild != null && current.rightChild.active) {
						selected = current.rightChild;
					} else if (current.leftChild != null && current.leftChild.active) {
						selected = current.leftChild;
					} else {
						selected = current.searchNextNode();
					}
					if(selected == null){
						return activeNodes.get(activeNodes.size() - 1); 
					}
					return selected;
				}
				
			}
		}
	}
	private void printAdvance(int iter) {
		System.out.println("---------------------------------------------------------------------------------------------------------------------------");
		String msn = "Iteracion de Branch and bound # " + iter + "\tpool " + algorithm.pool.size() ;
		msn += "\tZ_node: " + algorithm.aBound+ "\tBesBound: ";
		
		msn += (Math.round(BB_bestbound*100)/100.0);
		
		msn += "\tBestSol: " + BB_primalBound;
		msn +=  "\tgap: " +Math.round(((BB_primalBound-BB_bestbound)/BB_primalBound)*1000)/1000.0;
		msn += "\tActNodes: " + activeNodes.size() + "\t";
		msn += current.xijBranchInThisNode+ " -> ";
		
		System.out.println(msn);
		
		
	}
	/**
	 * The method returns the primal bound of the global search
	 * @return the branch and bound best solution
	 */
	public double getObjVal() {
		return  BB_primalBound;
	}

	public ArrayList<Leg> getUncoveredLegs() {
		ArrayList<Leg> list = current.getUncoveredLegs(data);
		return list;
	}
	
	
}
