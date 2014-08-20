package Utilities;

public class Rounder {
	public static final double deviation = 0.0000000000;
	
	
	public static double  round6Dec( double rounded) {
		return (Math.round(rounded*1000000)/1000000.0);
		//return rounded;
	}
	public static double  round9Dec( double rounded) {
		return (Math.round(rounded*1000000000)/1000000000.0);
		//return rounded;
	}
	public static double  round3Dec(double d) {
		return (Math.round(d*1000)/1000.0);
	}
	
}
