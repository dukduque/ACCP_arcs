package bpc.BBTree;

import java.util.ArrayList;

import bpc.CG.LP_Manager;
import gurobi.GRBException;
import Graph.Network;
import IO.DataHandler;
import IO.Leg;


public class MainACPC {
	
	/**
	 * Input file
	 */
	private static String inputFile;
	
	public static void main(String[] args) {
		try {
			inputFile = "schedule201403.xml";
			DataHandler data = new DataHandler(inputFile);
			data.ReadXLM(inputFile);
			
			String[] days = { "LU", "MA", "MI", "JU", "VI", "SA", "DO" };
			ArrayList<Leg> remainingLegs = null;
			for (int i = 0; i < days.length; i++) {
				
				String solving_day = days[i];
				System.out.println("/______________________\n\nHOY ES!!!!!!!:_    " + solving_day);
				data.buildInstanceForSolvingDay(solving_day, remainingLegs);
				Network network = new Network(data.getLegsToSolve(), data.getBases(), DataHandler.PILOT, data.getAllLegs());
				network.checkNetworkCorrectness();
				BPC_Algorithm bpc = new BPC_Algorithm(data, network);
				bpc.solveIP();
				remainingLegs  = bpc.getUncoveredLegs();
				System.out.println("total EXE time: "+ (System.currentTimeMillis() - LP_Manager.tnow)/ 1000.0);
			}
		} catch (GRBException e) {

			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		
	
	}
}
