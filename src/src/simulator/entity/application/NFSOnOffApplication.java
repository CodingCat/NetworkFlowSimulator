package simulator.entity.application;

public class NFSOnOffApplication extends NFSApplication {

	double onDurationUpbound = 0.0;
	double offDurationUpbound = 0.0;
	
	public NFSOnOffApplication(double dr, double onduration, double offduration) {
		super(dr);
		onDurationUpbound = onduration;
		offDurationUpbound = offduration;
	}
	
	public void send() {
		//TODO: start the new flow
	}
}
