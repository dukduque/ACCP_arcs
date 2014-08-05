package Graph;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.naming.NoPermissionException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import bpc.CG.LP_Manager;
import IO.DataHandler;
import IO.Leg;

/**
 * Network for the RCSPP as the subproblem of the CG procedure for the ACPP.
 * @author Eslava, D.M., Duque, D.
 */
public class Network {
	
	/**
	 * Id of the source node of the RCSPP network
	 */
	public static int source;
	
	/**
	 * Id of the sink node of the RCSPP network
	 */
	public static int sink;
	
	/**
	 * Number of nodes of the RCSPP network
	 */
	public static int numNodes;
	
	/**
	 * Number of arcs of the RCSPP network
	 */
	public static int numArcs;
	
	
	/**
	 * ArrayList that contains the nodes ({@link Nodo}) of the network
	 */
	private  ArrayList<Nodo> nodes_graph;
	
	/**
	 * ArrayList that contains the arcs ({@link Arco}) of the network
	 */
	private  ArrayList<Arco> arc_graph;
	
	/**
	 * List of all station names
	 */
	private ArrayList<String> stations_keys;
	private Hashtable<String, Station> stations;
	
	private ArrayList<String> baseStations;
	
	/**
	 * Arreglo que guarda los nodos visitados
	 */
	static int[] visited;
		
	/**
	 * Metodo contructor
	 */
	public Network(ArrayList<Leg> legs, Hashtable<String, String> bases , int crewMemeber) {
		// Crea el arreglo de nodos
		nodes_graph = new ArrayList<Nodo>();
		arc_graph = new ArrayList<Arco>();
		stations_keys = new ArrayList<String>();
		stations = new Hashtable<String, Station>();
		baseStations = new ArrayList<String>();
		
		loadBaseStations();
		createNodes(legs, bases);
		Sort(nodes_graph);
		
		for (int i = 0; i < nodes_graph.size(); i++) {
			nodes_graph.get(i).id = i;
			if (nodes_graph.get(i).getLegId() >= 0) {
				legs.get(nodes_graph.get(i).getLegId()).updateNodesRepresentation(nodes_graph.get(i));
			}
		}
		
		SortNodesInStations();
		
		source = 0;
		sink = nodes_graph.size() - 1;
		//Crea el arreglo visisted
		visited = new int[nodes_graph.size()];
		
		//Crea el arreglo de arcos
		
		createArcs(crewMemeber);
		numNodes = nodes_graph.size();
		numArcs = arc_graph.size();
		
		System.out.println("Nodes: " + nodes_graph.size());
		System.out.println("Arcs: " + arc_graph.size());
		Arco arc = null;
		Nodo tail, head = null;
		for (int i = 0; i < arc_graph.size(); i++) {
			arc = arc_graph.get(i);
			arc.setId(i);
			tail = arc.getTail();
			head = arc.getHead();
			if (tail.id == source) {
				head.rMagicIndex.add(0, i);
			} else {
				head.rMagicIndex.add(i);
			}
			if (head.id == sink) {
				tail.MagicIndex.add(0, i);
			} else {
				tail.MagicIndex.add(i);
			}
		}
	}
	
	
	


	/**
	 * Crea los nodos de la red
	 * @param legs nombre del archivo de la instancia
	 */
	public void createNodes(ArrayList<Leg> legs, Hashtable<String, String> bases){
		//Inicializa tres nodos artificales
		// Node 0. Represent the aggregated base where a pairing begins and ends
		Nodo source = new Nodo(0,0,"NN",0,"NN","NN", 0,0,"NN",0,0,-1);
		nodes_graph.add(source);
		
		
		for (int i = 0; i < legs.size(); i++) {
			Leg l = legs.get(i);
			String dayWeek = l.getDayWeek();
			int flightNumer = l.getFlightNumer();
			String from = l.getFrom();
			String to = l.getTo();
			int departure = l.getDeparture();
			int arrive = l.getArrive();
			int secAircraft = l.getSecAircraft();
			String airCraftType = l.getAirCraftType();
			int nextday = l.getNextday();
			
    		//Node that represents the opportunity of beginning the duty with this fight
    		Nodo ltemp1 = new Nodo((i + 1), flightNumer, dayWeek, 1,from, bases.get(from), departure , secAircraft, airCraftType, nextday, Nodo.NODE_TYPE_OPP, l.getId());
			//Node that represents the departure of the fight
    		Nodo ltemp2 = new Nodo((i + 1), flightNumer, dayWeek, 1,from, bases.get(from) ,departure,  secAircraft, airCraftType, nextday, Nodo.NODE_TYPE_DEP,l.getId());
			if(!stations.containsKey(from)){
				stations.put(from, new Station(from));
				stations_keys.add(from);
			}
			stations.get(from).OppNodes.add(ltemp1);
			stations.get(from).DepNodes.add(ltemp2);
	        		
			//Node that represents the arrival of the fight
    		Nodo ltemp3 = new Nodo((i + 1), flightNumer, dayWeek, 1,to, bases.get(to) , arrive,  secAircraft, airCraftType, nextday, Nodo.NODE_TYPE_ARR,l.getId());
    		if(!stations.containsKey(to)){
				stations.put(to, new Station(to));
				stations_keys.add(to);
			}
			stations.get(to).ArrNodes.add(ltemp3);
			
			
			nodes_graph.add(ltemp1);
			nodes_graph.add(ltemp2);
			nodes_graph.add(ltemp3);
					
			// Adding fight and deadhead arcs
			arc_graph.add(new Arco(ltemp2, ltemp3, Arco.TYPE_FIGHT));
			arc_graph.add(new Arco(ltemp2, ltemp3, Arco.TYPE_DEADHEAD));

		}
	      
		FinalLeg end = new FinalLeg(nodes_graph.size(),0,"NN",52,"NN",0,0,"NN",0);
		nodes_graph.add(end);
	}
	
	/**
	 * Crea los arcos de la red.
	 */
	public void createArcs(int crewMember){
		// Begin pairing arcs
		for (int i = 0; i < baseStations.size(); i++) {
			String bs = baseStations.get(i);
			Station s = stations.get(bs);
	
			for (int j = 0; j < s.DepNodes.size(); j++) {
				Nodo l_temp = s.DepNodes.get(j);
				arc_graph.add(new Arco(nodes_graph.get(0), l_temp, Arco.TYPE_PAIRING_START));
			}
		}

		// End pairing arcs 
		for (int i = 0; i < baseStations.size(); i++) {
			String bs = baseStations.get(i);
			Station s = stations.get(bs);
			for (int j = 0; j < s.ArrNodes.size(); j++) {
				Nodo l_temp = s.ArrNodes.get(j);
				arc_graph.add(new Arco(l_temp, nodes_graph.get(getNodes().size() - 1), Arco.TYPE_PAIRING_END));
			}
		}
		
		//Fight and deadhead arcs
			// Added in the nodes creation
				
		
		// Wait arcs between opp nodes at each station and duty arcs 
		for (int i = 0; i < stations_keys.size(); i++) {
			String str_s = stations_keys.get(i);
			Station sta = stations.get(str_s);
			for (int j = 0; j < sta.OppNodes.size(); j++) {
				arc_graph.add(new Arco(sta.OppNodes.get(j), sta.DepNodes.get(j), Arco.TYPE_DUTY));
				for (int k = j+1; k <sta.OppNodes.size(); k++) {
					if(sta.OppNodes.get(j).getAircraftType().equals(sta.OppNodes.get(k).getAircraftType())){
						arc_graph.add(new Arco(sta.OppNodes.get(j), sta.OppNodes.get(k), Arco.TYPE_WAIT));
						k = sta.OppNodes.size();
					}
				}
			}
		}

		// Wait arcs between fights, ie, fight connections and rest (short and long rest)
		for (int i = 0; i < stations_keys.size(); i++) {
			String str_s = stations_keys.get(i);
			Station sta = stations.get(str_s);
			for (int j = 0; j < sta.ArrNodes.size(); j++) {
				Nodo arr  =  sta.ArrNodes.get(j);
				boolean longRest = false;
				boolean dutyReplenish = false;
				for (int k = 0; k < sta.DepNodes.size(); k++) {
					Nodo dep = sta.DepNodes.get(k);
					if(arr.getAbsTime()<dep.getAbsTime()){
						if(arr.getAircraftType().equals(dep.getAircraftType())){
							int deltaTime = dep.getAbsTime() - arr.getAbsTime();
							if(arr.getSecAircraft()==dep.getSecAircraft()){
								if(deltaTime>= DataHandler.minConnectionTime[crewMember] && deltaTime< DataHandler.minRestTime[crewMember]){
									arc_graph.add(new Arco(arr, dep, Arco.TYPE_WAIT));
								}
							}else{
								if(deltaTime>= DataHandler.minConnectionTimeDifPlane[crewMember] && deltaTime< DataHandler.minRestTime[crewMember]){
									arc_graph.add(new Arco(arr, dep, Arco.TYPE_WAIT));
								}
							}
							if (deltaTime>= DataHandler.minRestTime[crewMember] && deltaTime< DataHandler.maxIdealRestTime[crewMember] ){
								arc_graph.add(new Arco(arr, dep, Arco.TYPE_REST));
							}
							if(deltaTime>=DataHandler.maxIdealRestTime[crewMember]  && deltaTime< DataHandler.minDutyReplenishTime[crewMember]  &&  !longRest){
								longRest=true;
								arc_graph.add(new Arco(arr, sta.OppNodes.get(k), Arco.TYPE_REST));
								
								if(arr.getAircraftType().equals(sta.OppNodes.get(k).getAircraftType())==false){
									System.out.println("PORQUE" + arr + " \n " + sta.OppNodes.get(k));
								}
							}
							if(deltaTime>= DataHandler.minDutyReplenishTime[crewMember] && !dutyReplenish){
								dutyReplenish = true;
								arc_graph.add(new Arco(arr, sta.OppNodes.get(k), Arco.TYPE_DUTY_REPLENISH));
							}
						}
					}
					
				}
			}
		}
	}
	
	
	private void loadBaseStations() {
	
		baseStations.add("BOG");
		baseStations.add("EOH");
		baseStations.add("MDE");
		
	}


	private void SortNodesInStations() {
		for (int i = 0; i < stations_keys.size(); i++) {
			String key = stations_keys.get(i);
			stations.get(key).sortAll();
		}
	}


	private void Sort(ArrayList<Nodo> set) {
		QS(set, 0, set.size() - 1);
	}

	public int colocar(ArrayList<Nodo> e, int b, int t) {
		int i;
		int pivote;
		int valor_pivote;
		Nodo temp;

		pivote = b;
		//valor_pivote = DataHandler.pi[e[pivote].id] ;
		valor_pivote = e.get(pivote).getSortCriteria();
		for (i = b + 1; i <= t; i++) {
			if (  e.get(i).getSortCriteria()< valor_pivote) {
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

	public void QS(ArrayList<Nodo> e, int b, int t) {
		int pivote;
		if (b < t) {
			pivote = colocar(e, b, t);
			QS(e, b, pivote - 1);
			QS(e, pivote + 1, t);
		}
	}


	public void checkNetworkCorrectness() {
		for (int i = 0; i < arc_graph.size(); i++) {
			Arco a = arc_graph.get(i);
			if (a.getTail().id >= a.getHead().id) {
				System.err.println("NOP TOPO" + a.getType()  +  "\n" + a.getTail() +  "\n" + a.getHead()  + "\n over : " + a.getTail().getNextday());
			}
			if(a.getTail().getAbsTime()> a.getHead().getAbsTime()){
				System.err.println("TIME ERROR");
			}
			a.checkTypeArcCorrectness(stations);
		}

	}





	public void updateDualInfo(LP_Manager algorithm, DataHandler data) {
		Arco arc = null;
		for (int i = 0; i < arc_graph.size(); i++) {
			arc = arc_graph.get(i);
			if( arc.getType() == Arco.TYPE_FIGHT){
				arc.c_ij = -algorithm.pi[arc.getTail().getLegId()];
			}
		}
		
	}





	public  ArrayList<Nodo> getNodes() {
		return nodes_graph;
	}

	public  ArrayList<Arco> getArcs() {
		return arc_graph;
	}


	
}
