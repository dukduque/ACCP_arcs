package Graph;

import java.util.Hashtable;


import IO.*;


/**
 * Clase que representa los arcos de la red
 * @author dm.eslava258
 *
 */
public class Arco {
	
	public static final int TYPE_FIGHT = 1;
	public static final int TYPE_DEADHEAD = 2;
	public static final int TYPE_WAIT = 3;
	public static final int TYPE_REST = 4;
	public static final int TYPE_DUTY = 5;
	public static final int TYPE_PAIRING_START= 6;
	public static final int TYPE_PAIRING_END= 7;
	public static final int TYPE_DUTY_REPLENISH = 8;
	/**
	 * Cabeza del arco
	 */
	private Nodo v_j;
	
	/**
	 * Cola del arco
	 */
	private Nodo v_i;
	
	/**
	 * Cost of the arc
	 */
	public double c_ij;
	
	/**
	 * Arc type
	 */
	private int type;
	
	/**
	 * Fight time related to this arc
	 */
	int flightTime;
	
	/**
	 * Rest time related to this arc
	 */
	int restTime;

	/**
	 * Wait time related to this arc
	 */
	int waitTime;
	
	/**
	 * Service time
	 */
	int serviveTime;
	
	/**
	 * 1 if the arc begin a duty
	 */
	int begDuty;
	
	/**
	 * id of the arc
	 */
	private int id;
	

	
	
	
	/**
	 * Metodo constructor del arco
	 * @param node_j
	 * @param node_i
	 */
	public Arco(Nodo node_i, Nodo node_j, int nType){
		this.id = -1;
		this.v_i = node_i;
		this.v_j = node_j;
		this.type = nType;
		c_ij = 0;
		flightTime =0;
		restTime = 0;
		waitTime = 0;
		serviveTime =0;
		begDuty = 0;
		if (type == TYPE_FIGHT) {
			flightTime = v_j.getAbsTime() - v_i.getAbsTime();
			serviveTime = flightTime;
		}else if ( type == TYPE_DEADHEAD){
			flightTime = (int)(0*( v_j.getAbsTime() - v_i.getAbsTime()));
			serviveTime = v_j.getAbsTime() - v_i.getAbsTime();
		}
		else if(type == TYPE_REST)
		{
			restTime = v_j.getAbsTime() - v_i.getAbsTime();
			if(v_j.getType()==Nodo.NODE_TYPE_DEP){
				begDuty = 1;
			}
		}else if(type == TYPE_WAIT)
		{
			waitTime = v_j.getAbsTime() - v_i.getAbsTime();
			if(v_i.getType()==Nodo.NODE_TYPE_ARR){
				serviveTime = waitTime;
			}else{
				restTime = v_j.getAbsTime() - v_i.getAbsTime();
			}
			
		}else if(type == TYPE_PAIRING_START){
			begDuty = 1;
		}else if(type == TYPE_DUTY){
			begDuty = 1;
		}else if(type == TYPE_DUTY_REPLENISH){
			
		}
		this.c_ij =0;
		
		
	}
	
	/**
	 * Devuelve la cola del arco
	 * @return objeto Leg que representa la cola del arco
	 */
	public Nodo getTail(){
		return v_i;
	}
	
	/**
	 * Devuelve la cabeza del arco
	 * @return objeto Leg que representa la cabeza del arco
	 */
	public Nodo getHead(){
		return v_j;
	}
	
	public int getType(){
		return type;
	}
	
	
	
	public String getTypeName() {
		String typeS= null;
		if(type == TYPE_PAIRING_START){
			typeS =  "Start";
		}else if (type == TYPE_PAIRING_END){
			typeS =  "End";
		}else if (type == TYPE_DUTY){
			typeS =  "Duty";
		}else if (type == TYPE_REST){
			typeS =  "Rest";
		}else if (type == TYPE_WAIT){
			typeS =  "Wait";
		}else if (type == TYPE_FIGHT){
			typeS =  "Fight";
		}else if (type == TYPE_DEADHEAD){
			typeS =  "DeadHead";
		}else if(type == TYPE_DUTY_REPLENISH){
			typeS = "24-rest";
		}
		return typeS;
	}

	public double getSortCriteria() {
		// TODO Auto-generated method stub
		return  v_j.getAbsTime();
	}

	public void checkTypeArcCorrectness(Hashtable<String, Station> stations) {
		if(type == TYPE_PAIRING_START){
			if(stations.containsKey(v_j.getStation())==false){
				System.err.println("Error in a star pairing arc: " + toString());
			}
		}else if (type == TYPE_PAIRING_END){
			if(stations.containsKey(v_i.getStation())==false){
				System.err.println("Error in an end pairing arc: " + toString());
			}
		}else if (type == TYPE_DUTY){
			if (v_i.getType() == Nodo.NODE_TYPE_OPP
					&& v_j.getType() == Nodo.NODE_TYPE_DEP
					&& v_i.getLegId() == v_j.getLegId()) {
				//Correcto
			}else{
				System.err.println("Error in a duty arc: " + toString());
			}
				
			
		}else if (type == TYPE_REST){
			if(v_i.getType() == Nodo.NODE_TYPE_ARR 
					&&(v_j.getType() == Nodo.NODE_TYPE_DEP  || v_j.getType() == Nodo.NODE_TYPE_OPP )
					&& v_i.getAircraftType().equals(v_j.getAircraftType())) {
				//Correcto
			}else{
				System.err.println("Error in a rest arc: " + toString());
			}
		}else if (type == TYPE_WAIT){
			if(v_i.getAircraftType().equals(v_j.getAircraftType())
					&& v_i.getStation().equals(v_i.getStation())){
				
			}else{
				System.err.println("Error in a wait arc: " + toString());
			}
		}else if (type == TYPE_FIGHT){
			if(v_i.getLegId() == v_j.getLegId()){
				//Correcto
			}else{
				System.err.println("Error in a fight arc: " + toString());
			}
		}else if (type == TYPE_DEADHEAD){
			if(v_i.getLegId() == v_j.getLegId()){
				//Correcto
			}else{
				System.err.println("Error in a deadhead arc: " + toString());
			}
		}
		
		
	}

	public int getId() {
		return id;
	}
	public void setId(int i) {
		id = i;
	}

	@Override
		public String toString() {
			// TODO Auto-generated method stub
			return v_i +  " | " + v_j + "|  type: " + getTypeName() +" cost: " +c_ij  + " fT: " + flightTime + " wT: " + waitTime ;
//			return getTypeName() + " -> " + c_ij;
		}

	public boolean isForbiden() {
		if(DataHandler.forbidden[v_i.id][v_j.id]==1){
			return true;
		}else{
			return false;
		}
	}
}
