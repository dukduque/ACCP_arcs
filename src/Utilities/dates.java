package Utilities;

public class dates {

	
	public static int convertStrDayToIntDay(String dayWeek) throws Exception{
		int day = -1;
		if (dayWeek.equals("LU")) {
			day = 1;
		} else if (dayWeek.equals("MA")) {
			day = 2;
		} else if (dayWeek.equals("MI")) {
			day = 3;
		} else if (dayWeek.equals("JU")) {
			day = 4;
		} else if (dayWeek.equals("VI")) {
			day = 5;
		} else if (dayWeek.equals("SA")) {
			day = 6;
		} else if (dayWeek.equals("DO")) {
			day = 7;
		}else{
			throw new Exception("Invalid day to convert");
		}
		return day;
	}
}
