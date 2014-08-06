package Graph;
import java.util.ArrayList;

import ColumnGenrators.PulseAlgorithm;
import bpc.CG.CG;

public class FinalLeg extends Nodo {
	int fLegID;
	ArrayList<Integer> bestPath;
	private CG  cg;
	
	public FinalLeg(int NodeId, int flightNumer, String dayWeek, 
			int Week, String place, int time, int secAircraft, 
			String airCraftType, int nextDay){
		super(NodeId, flightNumer, dayWeek, Week, place, place, time, secAircraft, airCraftType, nextDay, 0,-1);
		fLegID =NodeId ;
		

		// TODO Auto-generated constructor stub
	}

	public void pulse(int crewType, double fo, int dayFightTime, int dayServTime,
			int numDuties, int restTime, ArrayList<Integer> path, ArrayList<Integer> path_arcs,  String personelBase) {
		boolean mejoro  = false;
		if (fo < CG.primalBound) {
			mejoro = true;
			path.add(fLegID);
			bestPath =new ArrayList<Integer>();
			bestPath.addAll(path);
			path.remove(path.size() - 1);
			CG.primalBound = fo;
		}
		if(fo< PulseAlgorithm.rcForPool || mejoro){
			addPairingToCG_Pool(fo, path, path_arcs);
		} 
	}

	@SuppressWarnings("unchecked")
	private void addPairingToCG_Pool(double fo, ArrayList<Integer> path, ArrayList<Integer> path_arcs) {
		ArrayList<Integer> dummyPath = new ArrayList<>();
		Nodo node;
		Arco arc;
		int type=-1;
		for (int i = 0; i < path.size(); i++) {
			node = PulseAlgorithm.network.getNodes().get(path.get(i));
			arc =  PulseAlgorithm.network.getArcs().get(path_arcs.get(i));
			type = arc.getType();
			if(arc.get_v_i().equals(node)){
				if(node.getType() == Nodo.NODE_TYPE_DEP && type == Arco.TYPE_FIGHT){
					dummyPath.add(node.getLegId());
				}
				if(node.getType() == Nodo.NODE_TYPE_DEP && type != Arco.TYPE_FIGHT && type != Arco.TYPE_DEADHEAD){
					System.err.println("ERROR EN LA RED");
				}
			}else{
				System.err.println("no coiciden las colas del arco y el nodo del path");
				throw new NullPointerException();
			}
		}
		String key = dummyPath.toString();
		
		cg.pool.add(key);
		if (!cg.routesPoolRC.containsKey(key)) {
			cg.generator.put(key, PulseAlgorithm.generatorID);
			cg.routesPoolDist.put(key, 1.0);
			cg.routesPoolRC.put(key, fo);
			cg.netPath.put(key, (ArrayList<Integer>) path.clone());
			cg.netPathArcs.put(key, (ArrayList<Integer>) path_arcs.clone());
//			System.out.println(fo);
		}else{
			if(fo<cg.routesPoolRC.get(key)){
//				System.out.println("ESTOY SI PASA? PORQUE EL RC CAMBIO? "+ cg.routesPoolRC.get(key) + " vs llegando: " + fo+ " - > " + path);
//				System.out.println("old path: " + cg.netPath.get(key));
//				System.out.println("new Path: " + path);
				cg.routesPoolRC.put(key, fo);
			}
		}
	}


	public void setCG(CG cg) {
		this.cg = cg;
	}

}
