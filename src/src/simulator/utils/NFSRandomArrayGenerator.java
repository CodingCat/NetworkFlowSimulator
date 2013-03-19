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
			if (i == array.length - 1) array[i] = NFSDoubleCalculator.sub(1, sum);
			else {
				array[i] = rand.nextDouble() % (1 - sum);
				while (Math.abs(NFSDoubleCalculator.sub(array[i], avr)) > inputskew) {
					//bug here?
					array[i] = rand.nextDouble() % (1 - sum);
				}
				sum = NFSDoubleCalculator.sum(sum, array[i]);
			}
		}
		for (int i = 0; i < array.length; i++) System.out.print(array[i] + " ");
		System.out.println();
	}

}
