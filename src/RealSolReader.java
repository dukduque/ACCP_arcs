import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class contains a  redar for an XML that have that real pairng information of the airline.
 * @author d.duque25
 *
 */
public class RealSolReader {
	
	static ArrayList<Trip> trips;
	public static void main(String[] args) {
		RealSolReader rsr = new RealSolReader();
		trips = new ArrayList<>();
		rsr.read("CantTrips.xml");
	}

	private void read(String string) {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse (new File(string));
	
			// normalize text representation
			doc.getDocumentElement ().normalize();
			System.out.println ("Root element of the doc is: "+doc.getDocumentElement().getNodeName());
			
			System.out.println("----------------------------------------");
			
			NodeList nList = doc.getElementsByTagName("Trips");
			
			for (int i = 0; i < nList.getLength(); i++) {
				Node nTemp = nList.item(i);
				
				if(nTemp.getNodeType() == Node.ELEMENT_NODE){
					Trip t = new Trip();
					Element elem = (Element) nTemp;
					String datefrom = elem.getElementsByTagName("From").item(0).getTextContent();
	        		String dateTo = elem.getElementsByTagName("To").item(0).getTextContent();
	        		String datenum = elem.getElementsByTagName("DayNum").item(0).getTextContent();
					t.day = elem.getElementsByTagName("Day").item(0).getTextContent();
					t.crew = elem.getElementsByTagName("CrewPos").item(0).getTextContent();
					t.airCraftType = elem.getElementsByTagName("AirCraftType").item(0).getTextContent();
					t.cant = elem.getElementsByTagName("Cant").item(0).getTextContent();
					System.out.println(t);
					trips.add(t);
				}
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
