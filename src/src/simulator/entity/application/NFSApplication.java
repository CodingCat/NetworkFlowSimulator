package simulator.entity.application;

public class NFSApplication {
	protected double datarate = 0;// in kbps
	protected double packetsize = 0;//in bytes
	
	public NFSApplication(double dr) {
		datarate = dr;
	}
}
