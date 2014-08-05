
public class Trip {
	String day;
	String crew;
	String airCraftType;
	String cant;
	
	public Trip() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		
		return day+"/"+crew+"/"+airCraftType+"/"+cant;
	}
}
