package ColumnGenrators;

import java.util.ArrayList;
import java.util.Hashtable;

import javax.print.attribute.standard.Fidelity;
import javax.xml.crypto.Data;

import bpc.BBTree.BBNode;
import bpc.CG.CG;
import bpc.CG.LP_Manager;
import Graph.Arco;
import Graph.FinalLeg;
import Graph.Label;
import Graph.Network;
import Graph.Nodo;
import IO.DataHandler;

public class PulseAlgorithm {

	
	public static final int generatorID = 56234;

	public static double rcForPool = 0;
	
	public static double beam;
	
	public static Network network;
	private DataHandler data;
	private CG cg;
	private LP_Manager algorithm;
	private FinalLeg finalNode;
	
	public PulseAlgorithm(DataHandler nData, Network network, LP_Manager algo, CG cg){
		PulseAlgorithm.network = network;
		this.cg = cg;
		this.data = nData;
		this.algorithm = algo;
		finalNode = (FinalLeg) network.getNodes().get(network.getNodes().size()-1);
		finalNode.setCG(cg);
	}
	
	public void run(BBNode bbnode, boolean exact){
		
		calcBounds();
//		System.out.println("Bounds-->backward: " + network.getNodes().get(0).b_SP + " vs forward: "+ network.getNodes().get(network.getNodes().size()-1).f_SP );
		runPulse(exact);
		sendCols();
		
	}
	
	private void runPulse(boolean extact) {

		if (extact) {
			rcForPool =0.9*CG.primalBound;
			CG.primalBound = 0;
			beam = 1;
		}else{
			rcForPool =0.9*CG.primalBound;
			CG.primalBound = 0;
			beam = 1;
		}
		
		ArrayList<Integer> path_nodes = new ArrayList<Integer>();
		ArrayList<Integer> path_arcs = new ArrayList<Integer>();
		network.getNodes().get(0).pulse(DataHandler.PILOT, 1, 0, 0, 0,0, path_nodes, path_arcs, null);
	}

	private void sendCols() {
//		double tnow = System.currentTimeMillis();
		cg.Sort(cg.pool);
//		 tnow = System.currentTimeMillis()- tnow;
//		System.out.println("Only Sort: " + tnow/1000.0);
		
		ArrayList<String> newCols = cg.getPool();
		Hashtable<String, Double> colsRC = cg.getRoutesPoolRC();
		Hashtable<String, Double> colsDist = cg.getRoutesPoolDist();
		Hashtable<String, Integer> generators = cg.getGenerator();
		Hashtable<String, ArrayList<Integer>> path_arcs= cg.netPathArcs;
		
		CG.minRC = 1;
		for (int i = 0; i < Math.min(500, newCols.size()); i++) {
			String key = newCols.get(i);
			if (colsRC.get(key) < cg.minRC) {
				cg.minRC = colsRC.get(key);
				
//				System.out.println("best col->"  + key + " "  + LP_Manager_forVRPTW.pool.size());
//				System.out.println("En las cols, el RC me da " + cg.minRC + " en pulso " + cg.primalBound);
			}
			if (colsRC.get(key) < cg.rcCriteriaExact) {
				String col = newCols.get(i);
				col = col.substring(1, col.length() - 1);
				String[] colSplit = col.split(", ");
				ArrayList<Integer> dummyPath = new ArrayList<>();
				for (int j = 0; j < colSplit.length; j++) {
					int node = Integer.parseInt(colSplit[j]);
					dummyPath.add(node);

				}
				algorithm.addRoutesToArcs(path_arcs.get(key),null);
				algorithm.addRoutesToNodes(dummyPath);
				LP_Manager.pool.add(dummyPath);
				LP_Manager.pDist.add(colsDist.get(key));
				LP_Manager.generator.add(generators.get(key));
				LP_Manager.pool_of_arcs.add(path_arcs.get(key));

			}
		}

		cg.resetPool();
		
	}



	private void calcBounds() {
		resetBounds();
		// find backward SP
		Nodo v_i, v_j = null;
		Arco arc = null;
		int arcIndex = -1;
		
		network.getNodes().get(Network.source).f_SP = 0;
		for (int i = 0; i <network.getNodes().size(); i++) {
			v_i = network.getNodes().get(i);
			for (int j = 0; j < v_i.MagicIndex.size(); j++) {
				arcIndex = v_i.MagicIndex.get(j);
				arc = network.getArcs().get(arcIndex);
				v_j = arc.get_v_j();
				if(v_i.f_SP + arc.c_ij < v_j.f_SP){
					v_j.f_SP = v_i.f_SP + arc.c_ij;
				}
			}
		}
		
		
		network.getNodes().get(network.sink).b_SP = 0;
		for (int i = network.getNodes().size() - 1; i >= 0; i--) {
			v_i = network.getNodes().get(i);
			for (int j = 0; j < v_i.rMagicIndex.size(); j++) {
				arcIndex = v_i.rMagicIndex.get(j);
				arc = network.getArcs().get(arcIndex);
				v_j = arc.get_v_i();

				if (v_i.b_SP + arc.c_ij < v_j.b_SP) {
					v_j.b_SP = v_i.b_SP + arc.c_ij;
				}
			}
		}
//		if(network.getNodes().get(0).b_SP != network.getNodes().get(network.getNodes().size()-1).f_SP){
//			System.err.println("SP dan diferentes");
//			System.err.println("Bounds-->backward: " + network.getNodes().get(0).b_SP + " vs forward: "+ network.getNodes().get(network.getNodes().size()-1).f_SP );
//			
//		}
		
		
		
	}

	private void resetBounds() {
		Nodo nn = null;	
		for (int i = 0; i < network.getNodes().size(); i++) {
			nn = network.getNodes().get(i);
			nn.b_SP=Double.POSITIVE_INFINITY;
			nn.f_SP=Double.POSITIVE_INFINITY;
			nn.setLabels(new ArrayList<Label>());
		}
		
	}
}
