import java.io.File;

import gurobi.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 

import Graph.Arco;
import Graph.Network;
import Graph.Nodo;
import IO.DataHandler;
public class Main {
	/**
	 * Input file
	 */
	private static String inputFile;
	
	/**
	 * Results file
	 */
	private static PrintWriter stdOut;

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException, GRBException {
		
		GRBEnv env = new GRBEnv();
		env.set(GRB.IntParam.Presolve, 0);
		env.set(GRB.IntParam.DisplayInterval, 1);
		env.set(GRB.IntParam.Method, 0);
		GRBModel model  = new GRBModel(env);
		
		GRBVar x1 = model.addVar(0, 100, 100, GRB.CONTINUOUS, "x1");
		GRBVar x2 = model.addVar(0, 100, 2, GRB.CONTINUOUS, "x2");
		model.update();
		
		GRBLinExpr r1=new GRBLinExpr();
		r1.addTerm(2, x1);
		r1.addTerm(5, x2);
		model.addConstr(r1, GRB.LESS_EQUAL, 3, "R1");
		
		GRBLinExpr r2=new GRBLinExpr();
		r2.addTerm(7, x1);
		r2.addTerm(1, x2);
		model.addConstr(r2, GRB.LESS_EQUAL, 5, "R2");
		
		model.update();
		
		model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
		model.optimize();
		System.out.println("x1-> " + x1.get(GRB.DoubleAttr.X));
		System.out.println("x2-> " + x2.get(GRB.DoubleAttr.X));

		// Cambios 
		x1.set(GRB.DoubleAttr.Obj, 3);
//		model.update();
		model.optimize();
		System.out.println("x1-> " + x1.get(GRB.DoubleAttr.X));
		System.out.println("x2-> " + x2.get(GRB.DoubleAttr.X));
		
	}
}
