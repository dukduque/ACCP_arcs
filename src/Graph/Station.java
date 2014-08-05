package Graph;
import java.util.ArrayList;


public class Station {

	String name;
	ArrayList<Nodo> OppNodes;
	ArrayList<Nodo> DepNodes;
	ArrayList<Nodo> ArrNodes;
	
	public Station(String n) {
		name = n;
		OppNodes = new ArrayList<Nodo>();
		DepNodes = new ArrayList<Nodo>();
		ArrNodes = new ArrayList<Nodo>();
	}
	
	public void sortAll(){
		Sort(OppNodes);
		Sort(DepNodes);
		Sort(ArrNodes);
	}
	
	private void Sort(ArrayList<Nodo> set) {
		QS(set, 0, set.size() - 1);
	}

	public int colocar(ArrayList<Nodo> e, int b, int t) {
		int i;
		int pivote;
		double valor_pivote;
		Nodo temp;

		pivote = b;
		valor_pivote = e.get(pivote).getAbsTime()+ (e.get(pivote).getLegId()/100000.0);
		for (i = b + 1; i <= t; i++) {
			if (e.get(i).getAbsTime() + (e.get(i).getLegId()/100000.0)< valor_pivote) {
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
	
}
