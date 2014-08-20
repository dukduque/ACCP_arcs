package Graph;

import Utilities.Rounder;

public class Label {

	double fo;
	double dutyFlightTime;
	double dutyServTime;
	int numDuties;
	
	double sortCriterion;
	
	
//	int numLands;
	public Label(double n_fo, double n_dutyFlightTime, double n_dutyServTime, int n_numDuties) {
		fo=n_fo;
		dutyFlightTime = n_dutyFlightTime;
		dutyServTime = n_dutyServTime;
		numDuties = n_numDuties;
//		numLands = numLandings;
		sortCriterion = fo;
		
	}
	
	
	public boolean dominates(Label l){
		if(fo<=l.fo && dutyFlightTime<=l.dutyFlightTime && dutyServTime<=l.dutyServTime && numDuties<=l.numDuties ){
			return true;
		}else{
			return false;
		}
	}
	
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return ""+ Rounder.round3Dec(sortCriterion);
	}

}
