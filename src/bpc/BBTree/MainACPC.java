package bpc.BBTree;

import bpc.CG.LP_Manager;
import gurobi.GRBException;
import Graph.Network;
import IO.DataHandler;


public class MainACPC {
	
	/**
	 * Input file
	 */
	private static String inputFile;
	
	public static void main(String[] args) {
		inputFile = "schedule201403.xml";
		DataHandler data = new DataHandler(inputFile);
		data.ReadXLM(inputFile);
		Network network = new Network(data.getLegs(), data.getBases(), DataHandler.PILOT);
		network.checkNetworkCorrectness();
		try {
			BPC_Algorithm bpc = new BPC_Algorithm(data,network);
			bpc.solveIP();
			System.out.println("total EXE time: " + (System.currentTimeMillis()-LP_Manager.tnow)/1000.0);
		} catch (GRBException e) {

			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	
	}
}
