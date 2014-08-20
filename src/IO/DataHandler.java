package IO;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import Utilities.Dates;


public class DataHandler {
	
	
	

	public static final Random rnd = new Random(12345);
	public static final int PILOT = 0;
	public static final int COPILOT = 1;
	public static final int AUX = 2;
	
	
	/**
	 * Max time for a duty. Pilot, Copilot, Aux
	 */
	public static double[] MaxFlightTime_Day = {7*60,8*60,11*60}; 
	
	public static double[] maxLandingsPerDuty = {12,6,100}; 
	
	public static double[]  MaxFlightTime_Week= {40*60,40*60,40*60};

	
	public static final double[] MaxServTime_Day = {12*60,11*60,11*60};
	
	

	
	/**
	 * Maximum ideal rest time
	 */
	public static final double[] maxIdealRestTime = {14*60,14*60,14*60};
		
	/**
	 * Minimum rest time
	 */
	public static final double[] minRestTime = {10*60,10*60,10*60};
	
	
	/**
	 * Minimum connection time between fights when the plane is the same
	 */
	public static final double[] minConnectionTime = {5, 60, 60};
	
	public static final double[] minConnectionTimeDifPlane = {60, 60, 60};
	
	/**
	 * Maximum number of duties per pairing
	 */
	public static final int[] maxNumDuties = {3, 5, 6 };
	
	public static int[] minDutyReplenishTime = { 1440, 1440, 1440 };
	
	

	private ArrayList<Leg> allLegs;
	
	private ArrayList<Leg> legsToSolve;
	private Hashtable<String, String> bases;
	
	public static int numLegsToSolve;
	public static int[] forbidden;
	
	
	public DataHandler(String inputFile) {
		allLegs = new ArrayList<Leg>();
		bases = new Hashtable<>(20);
	}
	
	public void ReadXLM(String inputFile) {
		try{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	        Document doc = docBuilder.parse (new File(inputFile));

	        // normalize text representation
	        doc.getDocumentElement ().normalize ();
	        System.out.println ("Root element of the doc is: "+doc.getDocumentElement().getNodeName());
	        
	        System.out.println("----------------------------------------");
	        int legIndex=0;
	        NodeList nList = doc.getElementsByTagName("leg");
	        for(int i =0;i<nList.getLength();i++){
	        	Node nTemp = nList.item(i);
	        	if(nTemp.getNodeType() == Node.ELEMENT_NODE){
	        		Element elem = (Element) nTemp;
	        		String datefrom = elem.getElementsByTagName("dateFrom").item(0).getTextContent();
	        		String dateTo = elem.getElementsByTagName("dateTo").item(0).getTextContent();
	        		String dayWeek = elem.getElementsByTagName("dayWeek").item(0).getTextContent();
	        		String airline = elem.getElementsByTagName("airline").item(0).getTextContent();
	        		int flightType = Integer.parseInt(elem.getElementsByTagName("flightType").item(0).getTextContent());
	        		int flightNumer = Integer.parseInt(elem.getElementsByTagName("flightNumber").item(0).getTextContent());
	        		String from = elem.getElementsByTagName("from").item(0).getTextContent();
	        		String to = elem.getElementsByTagName("to").item(0).getTextContent();
	        		int departure = Integer.parseInt(elem.getElementsByTagName("departure").item(0).getTextContent());
	        		int arrive = Integer.parseInt(elem.getElementsByTagName("arrive").item(0).getTextContent());
	        		int secAircraft = Integer.parseInt(elem.getElementsByTagName("secAircraft").item(0).getTextContent());
	        		String airCraftType = elem.getElementsByTagName("aircraftType").item(0).getTextContent();
	        		int nextday = Integer.parseInt(elem.getElementsByTagName("nextDay").item(0).getTextContent());
	        		int localFlight = Integer.parseInt(elem.getElementsByTagName("localFlight").item(0).getTextContent());
	        		Leg l = new Leg(legIndex, datefrom,dateTo, 1 , dayWeek,airline,flightType,flightNumer,from,to,departure,arrive,secAircraft,airCraftType,nextday,localFlight);
					allLegs.add(l);
					bases.put(from, from);
					bases.put(to, to);
					legIndex++;
				}
			}
		}catch (SAXParseException err) {
	        System.out.println ("** Parsing error" + ", line "+ err.getLineNumber () + ", uri " + err.getSystemId ());
	           System.out.println(" " + err.getMessage ());
		}catch (SAXException e) {
	           Exception x = e.getException ();
	           ((x == null) ? e : x).printStackTrace ();

	     }catch (Throwable t) {
	           t.printStackTrace ();
	     }
		System.out.println("Total flights: " + allLegs.size());
		updateBases();
	}




	private void updateBases() {
		bases.put("MDE", "EOH");
		
	}


	public ArrayList<Leg> getLegsToSolve() {
		return legsToSolve;
	}

	public void setLegsToSolve(ArrayList<Leg> legs) {
		this.legsToSolve = legs;
	}


	public Hashtable<String, String> getBases() {
		return bases;
	}

	/**
	 * @return the allLegs
	 */
	public ArrayList<Leg> getAllLegs() {
		return allLegs;
	}

	/**
	 * @param allLegs the allLegs to set
	 */
	public void setAllLegs(ArrayList<Leg> allLegs) {
		this.allLegs = allLegs;
	}

	public void buildInstanceForSolvingDay(String solving_day, ArrayList<Leg> remainingLegs) {
		legsToSolve = new ArrayList<>();
		int indexToSolve= 0;
		if (remainingLegs!=null) {
			for (int i = 0; i <remainingLegs.size(); i++) {
				Leg l = remainingLegs.get(i);
				l.setId_ToSolve(indexToSolve);
				int solveingDay = Dates.convertStrDayToIntDay(solving_day);
				int legDay = Dates.convertStrDayToIntDay(l.getDayWeek());
				if(solveingDay -legDay == 6){
					l.setWeek(l.getWeek()+1);
				}
				
				legsToSolve.add(l);	
				indexToSolve++;
			}
		}
		for (int i = 0; i < allLegs.size(); i++) {
			Leg l = allLegs.get(i);
			if(l.getDayWeek().equals(solving_day) || solving_day.equals("ALL")){
				l.setId_ToSolve(indexToSolve);
				legsToSolve.add(l);	
				indexToSolve++;
			}
		}
		numLegsToSolve = legsToSolve.size();
	}
}
