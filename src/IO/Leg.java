package IO;

import Graph.Nodo;

public class Leg {

	private String datefrom;
	private String dateTo;
	private int week;
	private String dayWeek;
	private String airline;
	private int flightType;
	private int flightNumer;
	private String from;
	private String to;
	private int departure;
	private int arrive;
	private int secAircraft;
	private String airCraftType;
	private int nextday;
	private int localFlight;
	private int id_global;
	private int id_ToSolve;
	private int oppNodeId;
	private int arrNodeId;
	private int depNodeId;
	
	
	
	public Leg(int id , String datefrom, String dateTo, int week, String dayWeek, String airline,
			int flightType, int flightNumer, String from, String to,
			int departure, int arrive, int secAircraft, String airCraftType,
			int nextday, int localFlight) {
		this.setId_global(id);
		this.setDatefrom(datefrom);
		this.setDateTo(dateTo);
		this.setDayWeek(dayWeek);
		this.setWeek(week);
		this.setAirline(airline);
		this.setFlightType(flightType);
		this.setFlightNumer(flightNumer);
		this.setFrom(from);
		this.to = to;
		this.departure = departure;
		this.arrive = arrive;
		this.secAircraft = secAircraft;
		this.airCraftType = airCraftType;
		this.nextday = nextday;
		this.localFlight = localFlight;
		
		
	}



	/**
	 * @return the dateTo
	 */
	public String getDateTo() {
		return dateTo;
	}



	/**
	 * @param dateTo the dateTo to set
	 */
	public void setDateTo(String dateTo) {
		this.dateTo = dateTo;
	}



	/**
	 * @return the dayWeek
	 */
	public String getDayWeek() {
		return dayWeek;
	}



	/**
	 * @param dayWeek the dayWeek to set
	 */
	public void setDayWeek(String dayWeek) {
		this.dayWeek = dayWeek;
	}



	/**
	 * @return the airline
	 */
	public String getAirline() {
		return airline;
	}



	/**
	 * @param airline the airline to set
	 */
	public void setAirline(String airline) {
		this.airline = airline;
	}



	/**
	 * @return the flightType
	 */
	public int getFlightType() {
		return flightType;
	}



	/**
	 * @param flightType the flightType to set
	 */
	public void setFlightType(int flightType) {
		this.flightType = flightType;
	}



	/**
	 * @return the flightNumer
	 */
	public int getFlightNumer() {
		return flightNumer;
	}



	/**
	 * @param flightNumer the flightNumer to set
	 */
	public void setFlightNumer(int flightNumer) {
		this.flightNumer = flightNumer;
	}



	/**
	 * @return the from
	 */
	public String getFrom() {
		return from;
	}



	/**
	 * @param from the from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}



	/**
	 * @return the to
	 */
	public String getTo() {
		return to;
	}



	/**
	 * @param to the to to set
	 */
	public void setTo(String to) {
		this.to = to;
	}



	/**
	 * @return the departure
	 */
	public int getDeparture() {
		return departure;
	}



	/**
	 * @param departure the departure to set
	 */
	public void setDeparture(int departure) {
		this.departure = departure;
	}



	/**
	 * @return the arrive
	 */
	public int getArrive() {
		return arrive;
	}



	/**
	 * @param arrive the arrive to set
	 */
	public void setArrive(int arrive) {
		this.arrive = arrive;
	}



	/**
	 * @return the secAircraft
	 */
	public int getSecAircraft() {
		return secAircraft;
	}



	/**
	 * @param secAircraft the secAircraft to set
	 */
	public void setSecAircraft(int secAircraft) {
		this.secAircraft = secAircraft;
	}



	/**
	 * @return the airCraftType
	 */
	public String getAirCraftType() {
		return airCraftType;
	}



	/**
	 * @param airCraftType the airCraftType to set
	 */
	public void setAirCraftType(String airCraftType) {
		this.airCraftType = airCraftType;
	}



	/**
	 * @return the nextday
	 */
	public int getNextday() {
		return nextday;
	}



	/**
	 * @param nextday the nextday to set
	 */
	public void setNextday(int nextday) {
		this.nextday = nextday;
	}



	/**
	 * @return the localFlight
	 */
	public int getLocalFlight() {
		return localFlight;
	}



	/**
	 * @param localFlight the localFlight to set
	 */
	public void setLocalFlight(int localFlight) {
		this.localFlight = localFlight;
	}



	public String getDatefrom() {
		return datefrom;
	}



	public void setDatefrom(String datefrom) {
		this.datefrom = datefrom;
	}



	public int getId_global() {
		return id_global;
	}



	public void setId_global(int id) {
		this.id_global = id;
	}

	/**
	 * @return the id_ToSolve
	 */
	public int getId_ToSolve() {
		return id_ToSolve;
	}



	/**
	 * @param id_ToSolve the id_ToSolve to set
	 */
	public void setId_ToSolve(int id_ToSolve) {
		this.id_ToSolve = id_ToSolve;
	}

	

	/**
	 * @return the oppNodeId
	 */
	public int getOppNodeId() {
		return oppNodeId;
	}



	/**
	 * @param oppNodeId the oppNodeId to set
	 */
	public void setOppNodeId(int oppNodeId) {
		this.oppNodeId = oppNodeId;
	}



	/**
	 * @return the arrNodeId
	 */
	public int getArrNodeId() {
		return arrNodeId;
	}



	/**
	 * @param arrNodeId the arrNodeId to set
	 */
	public void setArrNodeId(int arrNodeId) {
		this.arrNodeId = arrNodeId;
	}





	/**
	 * @return the depNodeId
	 */
	public int getDepNodeId() {
		return depNodeId;
	}



	/**
	 * @param depNodeId the depNodeId to set
	 */
	public void setDepNodeId(int depNodeId) {
		this.depNodeId = depNodeId;
	}



	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return id_global + "/" + dayWeek + "/" + flightNumer + "/" + from + "/" + to
				+ "/" + departure + "/" + arrive + "/"+ airCraftType + "/" + secAircraft;
	}



	public void updateNodesRepresentation(Nodo nodo) {
		if (nodo.getType() == Nodo.NODE_TYPE_OPP) {
			setOppNodeId(nodo.id);
		} else if (nodo.getType() == Nodo.NODE_TYPE_ARR) {
			setArrNodeId(nodo.id);
		} else if (nodo.getType() == Nodo.NODE_TYPE_DEP) {
			setDepNodeId(nodo.id);
		} else {
			System.out.println("Error in Lel update nodes");
		}
	}



	/**
	 * @return the week
	 */
	public int getWeek() {
		return week;
	}



	/**
	 * @param week the week to set
	 */
	public void setWeek(int week) {
		this.week = week;
	}







	

}
