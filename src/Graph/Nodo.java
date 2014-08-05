package Graph;
import java.util.ArrayList;

import bpc.CG.CG;
import ColumnGenrators.PulseAlgorithm;
import IO.DataHandler;

/**
 * Clase que representa un vuelo
 * 
 * @author dm.eslava258
 * 
 */
public class Nodo {
	
	public static final int NODE_TYPE_OPP = 1;
	public static final int NODE_TYPE_DEP = 2;
	public static final int NODE_TYPE_ARR = 3;
	
	public static final int maxNumLabels =100;
	
	/**
	 * Id of the node
	 */
	public int id;
	
	/**
	 * Id of the corresponding leg
	 */
	private int legId;
	
	/**
	 * Date from
	 */

	/**
	 * Dayweek
	 */
	private int dayWeek;

	/**
	 * Flight number
	 */
	private int flightNumber;

	/**
	 * station
	 */
	private String  station;
	
	/**
	 * station
	 */
	private String  base;

	/**
	 * Departure and arrive
	 */
	private int time;

/**
	 * Secaircraft. Número avión
	 */
	private int secAircraft;

	/**
	 * Air Craft type. Flota
	 */
	private String airCraftType;

	/**
	 * Next day and local flight
	 */
	private int nextDay;

	/**
	 * Absolute time of the fight. This is the time in minutes including the day
	 */
	private int absTime;
	
	
	/**
	 * ArrayList which stores a list of indexes of the arcs where this node is
	 * tail
	 */
	public ArrayList<Integer> MagicIndex;
	public ArrayList<Integer> rMagicIndex;	
	
	/**
	 * dual variable
	 */
	private double dualVar;
	
	public double f_SP = Double.POSITIVE_INFINITY;
	public double b_SP = Double.POSITIVE_INFINITY;
	
	
	/**
	 * week of the month at where the fight take place
	 */
	private int week;
	private int type; 
	
	private boolean firstTime;
	
	private ArrayList<Label> labels;

	
	
	/**
	 * Constructor method 
	 */
	public Nodo(int NodeId, int flightNumer, String dayWeek, int Week, String place, String base,
			int time, int secAircraft, String airCraftType, int nextDay , int type, int nLegId) {
		// Inicializa los parámetros
		this.id = NodeId;
		this.flightNumber = flightNumer;
		this.station = place;
		this.base = base;
		this.secAircraft = secAircraft;
		this.airCraftType = airCraftType;
		this.nextDay = nextDay;
		this.week = Week;
		this.time = time;
		this.type  = type;
		if (dayWeek.equals("LU")) {
			this.dayWeek = 1;
		} else if (dayWeek.equals("MA")) {
			this.dayWeek = 2;
		} else if (dayWeek.equals("MI")) {
			this.dayWeek = 3;
		} else if (dayWeek.equals("JU")) {
			this.dayWeek = 4;
		} else if (dayWeek.equals("VI")) {
			this.dayWeek = 5;
		} else if (dayWeek.equals("SA")) {
			this.dayWeek = 6;
		} else if (dayWeek.equals("DO")) {
			this.dayWeek = 7;
		}else{
			if(id == Network.source){
				this.week = 0;
				this.dayWeek = 0;
			}else{
				this.week  = 52;
				this.dayWeek = 0;
			}
		}
		if(nextDay == 1 && type == NODE_TYPE_ARR && this.dayWeek<7){
			this.dayWeek++;
		}else if(nextDay == 1 && type == NODE_TYPE_ARR && this.dayWeek==7){
			this.dayWeek =1;
			this.week ++ ;
			
		}
	
		MagicIndex = new ArrayList<Integer>();
		rMagicIndex = new ArrayList<Integer>();
		if(NODE_TYPE_DEP == type ){
			dualVar =-1 + 2*DataHandler.rnd.nextDouble();
		}
		absTime = (this.week-1)*7*1440 + (this.dayWeek-1)*1440 + time;
		labels = new ArrayList<Label>();
		legId = nLegId;
	}

	
	
	/**
	 * Metodo del pulso
	 */
	public void pulse(int crewType, double fo, int dayFightTime, int dayServTime,
			int consecutiveDuties, int restTime, ArrayList<Integer> path, ArrayList<Integer> path_arcs,  String personelBase) {
		if(firstTime){
			firstTime = false;
			sort(MagicIndex);
		}
		
		
		if (fo + b_SP <= CG.primalBound 
				&& dayFightTime<= DataHandler.MaxFlightTime_Day[crewType] 
				&& dayServTime<= DataHandler.MaxServTime_Day[crewType] 
				&& consecutiveDuties<= DataHandler.maxNumDuties[crewType]
				&& checkLabels(fo,dayFightTime,dayServTime,consecutiveDuties) == false) {
			path.add(id);
			Nodo next = null;
			Arco arc = null;
			
			for (int i = 0; i < MagicIndex.size()*PulseAlgorithm.beam; i++) {
				arc = PulseAlgorithm.network.getArcs().get(MagicIndex.get(i));
				if(!arc.isForbiden()){
					next = arc.getHead();
					path_arcs.add(arc.getId());
					double nFO = fo + arc.c_ij;
					int newDutyFightTtime = dayFightTime+ arc.flightTime;
					int newDutyServTtime = dayServTime + arc.serviveTime;
					int newRestTime  = restTime + arc.restTime;
					int newConsecutiveDuties =  consecutiveDuties + arc.begDuty;
	
					if(arc.getType() == Arco.TYPE_PAIRING_START){
						next.pulse(crewType, nFO , newDutyFightTtime, newDutyServTtime, newConsecutiveDuties, newRestTime, path, path_arcs,next.base);
					}
					else if(arc.getType() == Arco.TYPE_PAIRING_END){
						if(base.equals(personelBase)){
							next.pulse(crewType, nFO , newDutyFightTtime, newDutyServTtime, newConsecutiveDuties, newRestTime, path, path_arcs,personelBase);
						}
					}else if(arc.getType()==Arco.TYPE_FIGHT){
						newRestTime=0;
						next.pulse(crewType, nFO , newDutyFightTtime, newDutyServTtime, newConsecutiveDuties, newRestTime, path, path_arcs,personelBase);
					}else if(arc.getType()==Arco.TYPE_DEADHEAD){
						newRestTime=0;
						next.pulse(crewType, nFO , newDutyFightTtime, newDutyServTtime, newConsecutiveDuties, newRestTime, path, path_arcs,personelBase);
					}else if(arc.getType()==Arco.TYPE_WAIT){
						newRestTime=0;
						next.pulse(crewType, nFO , newDutyFightTtime, newDutyServTtime, newConsecutiveDuties, newRestTime, path, path_arcs,personelBase);
					}else if(arc.getType() == Arco.TYPE_DUTY){
						newRestTime = 0;
						next.pulse(crewType, nFO , newDutyFightTtime, newDutyServTtime, newConsecutiveDuties, newRestTime, path, path_arcs,personelBase);
					}else if (arc.getType() == Arco.TYPE_REST){
						newDutyFightTtime= 0 ;
						newDutyServTtime = 0 ;
						next.pulse(crewType, nFO , newDutyFightTtime, newDutyServTtime, newConsecutiveDuties, newRestTime, path, path_arcs,personelBase);
					}else if (arc.getType() == Arco.TYPE_DUTY_REPLENISH){
						newConsecutiveDuties =0;
						newDutyFightTtime= 0 ;
						newDutyServTtime = 0 ;
						next.pulse(crewType, nFO , newDutyFightTtime, newDutyServTtime, newConsecutiveDuties, newRestTime, path, path_arcs,personelBase);
					}	
					else{
						System.out.println("Que arco es??? " + arc.getType());
					}
					path_arcs.remove((path_arcs.size() - 1));
				}
			}
			path.remove((path.size() - 1));
		}
		

	}





	private boolean checkLabels(double fo, int dayFightTime, int dayServTime,int numDuties) {
		Label l = new Label(fo, dayFightTime, dayServTime, numDuties);
		
		for (int i = 0; i < labels.size(); i++) {
			if (labels.get(i).dominates(l)) {
				return true;
			}
		}

		for (int i = 0; i < labels.size(); i++) {
			if (l.dominates(labels.get(i))) {
				labels.remove(i);
				i--;
			}
		}

		this.addLabel(l);
		
		

		return false;
	}



	private boolean addLabel(Label label) {
		double mVal;
		boolean cond = true;
		int l = 0;
		int r = labels.size();
		int m = (int) ((l + r) / 2);
		if(m==0){
			labels.add(label);
			return true;
		}
		mVal = labels.get(m).sortCriterion;
		double cScore = label.sortCriterion;
		while(cond){
			if(r-l>1){
				if (cScore < mVal) {
					r = m;
					m = (int) ((l + r) / 2);
				} else if (cScore > mVal) {
					l = m;
					m = (int) ((l + r) / 2);
				} else {
					labels.add(m, label);
					//System.err.println(VertexPulse.class + " method: inser label: \n" + " this should not happend because for 2 paths with the same dist there is a dominance test" );
					return true;
				}
				mVal = labels.get(m).sortCriterion;
			}else{
				cond = false;
				if (l == m) {
					labels.add(cScore < mVal ? l : l + 1, label);
					return true;
				} else if (r == m) {
					System.out.println("esto no pasa !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!labels pulso ");
//					L.add(cScore > mVal ? r : Math.min(r + 1, L.size()), np);
				} else {
					System.err.println("Error en addLabel del pulso");
				}
				return false;
			}
		}
		return false;
		
		
		
		
		
		
		
		
		
		
		
		
//		
//		if(labels.size()>0){
//			if(label.fo<labels.get(0).fo){
//				labels.add(0,label);
//				if(labels.size()>maxNumLabels){
//					labels.remove(labels.size()-1);
//				}
//			}
//			else{
//				if(labels.size()<maxNumLabels){
//					labels.add(label);
//				}else{
//					int rnd = 1+ DataHandler.rnd.nextInt(maxNumLabels-1);
//					labels.set(rnd, label);
//				}
//			}
//		}else{
//			labels.add(label);
//		}
//		return true;
		
	}



	/**
	 * Métodos que me dan acceso al valor de los atributos
	 */
	public int getNodeId() {
		return id;
	}

	

	public int getDayWeek() {
		return dayWeek;
	}

	
	public int getFlightNumber() {
		return flightNumber;
	}


	public int getSecAircraft() {
		return secAircraft;
	}

	public String getAircraftType() {
		return airCraftType;
	}

	public int getNextday() {
		return nextDay;
	}

	
	public double getDualVar(){
		return dualVar;
		
	}
	
	public int getAbsTime(){
		return absTime;
	}
	public int getTime(){
		return time;
	}
	/**
	 * toString() method
	 */
	public String toString() {
		String tipo = type==NODE_TYPE_ARR?"Arr":type==NODE_TYPE_DEP?"DEP":"OPP";
		return ""+ id + " F: " + flightNumber + " City: " + station +  " Day: " + dayWeek + " T: " + time + " Type:" + tipo  + " absTime: " + absTime  ;
	}

	public int getType() {
		return type;
	}
	public String getTypeString(){
		String tipo = type==NODE_TYPE_ARR?"Arr":type==NODE_TYPE_DEP?"DEP":"OPP";
		return tipo;
	}

	public String getStation() {
		return station;
	}

	public int getLegId() {
		return legId;
	}
	public int getSortCriteria() {
		// TODO Auto-generated method stub
		return absTime + type;
	}
	
	private void sort(ArrayList<Integer> set) {
		QS(set, 0, set.size() - 1);
	}

	public int colocar(ArrayList<Integer> e, int b, int t) {
		int i;
		int pivote;
		double valor_pivote;
		int temp;

		pivote = b;
		
		valor_pivote = PulseAlgorithm.network.getArcs().get(e.get(pivote)).getSortCriteria();
		for (i = b + 1; i <= t; i++) {
			if ( PulseAlgorithm.network.getArcs().get(e.get(i)).getSortCriteria()< valor_pivote) {
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



	public ArrayList<Label> getLabels() {
		return labels;
	}



	public void setLabels(ArrayList<Label> labels) {
		this.labels = labels;
	}
	
}
