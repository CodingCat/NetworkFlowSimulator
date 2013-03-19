package simulator.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import simulator.NetworkFlowSimulator;

public class NFSDoubleCalculator {
	
	private static int precision = NetworkFlowSimulator.parser.getInt("fluidsim.system.doubleprecision", 6);
	
	public static double sum(double d1, double d2) {
		BigDecimal bd1 = (new BigDecimal(Double.toString(d1))).setScale(precision, RoundingMode.DOWN);
		BigDecimal bd2 = (new BigDecimal(Double.toString(d2))).setScale(precision, RoundingMode.DOWN);
		return bd1.add(bd2).doubleValue();
	}
	
	public static double sub(double d1, double d2) {
		BigDecimal bd1 = (new BigDecimal(Double.toString(d1))).setScale(precision, RoundingMode.DOWN);
		BigDecimal bd2 = (new BigDecimal(Double.toString(d2))).setScale(precision, RoundingMode.DOWN);
		return bd1.subtract(bd2).doubleValue(); 
	}
	
	public static double mul(double d1, double d2) {
		BigDecimal bd1 = (new BigDecimal(Double.toString(d1))).setScale(precision, RoundingMode.DOWN);
		BigDecimal bd2 = (new BigDecimal(Double.toString(d2))).setScale(precision, RoundingMode.DOWN);
		return bd1.multiply(bd2).doubleValue();
	}
	
	public static double div(double d1, double d2) {
		try {
			if (d2 == 0.0) throw new Exception ("cannot divide anything by 0");
			BigDecimal bd1 = (new BigDecimal(Double.toString(d1))).setScale(precision, RoundingMode.DOWN);
			BigDecimal bd2 = (new BigDecimal(Double.toString(d2))).setScale(precision, RoundingMode.DOWN);
			return bd1.divide(bd2, precision, RoundingMode.DOWN).doubleValue();
		}
		catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
}
