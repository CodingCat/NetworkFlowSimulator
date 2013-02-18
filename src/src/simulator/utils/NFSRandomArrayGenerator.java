package simulator.utils;

import java.util.Random;

import simulator.NetworkFlowSimulator;

public class NFSRandomArrayGenerator {

	/**
	 * generate a random array with double values
	 * @param array
	 */
	public static void getDoubleArray(double [] array) {
		Random rand = new Random(System.currentTimeMillis());
		double sum = 0.0;
		double avr = 1.0/(double) array.length;
		double inputskew = NetworkFlowSimulator.parser.getDouble(
				"fluidsim.application.mapreduce.inputskew", 0.001);
		for (int i = 0; i < array.length; i++) {
			if (i == array.length) array[i] = 1- sum;
			else {
				double p = rand.nextDouble();
				while (Math.abs(avr - p) < inputskew) p = rand.nextDouble();
				array[i] = p;
				sum += p;
			}
		}
	}

}
