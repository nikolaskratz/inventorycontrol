
public class multipleModelManager {

	public static void main(String[] args) throws Exception {
		int iterations = 1;
//		String[] names = new String[iterations];
		String[] names = {"_fcd-199"};
		
		for(int i=0; i<iterations; i++) {
			SimulationManager sm = new SimulationManager(names[i], i+21);
		}

	}

}

//"_0.2-0.4_100-100"
//"_0.9999", "_0.95", "_0.925", "_0.9", "_0.8", "_0.5", 
//"_0.2-0.4", "_0.4-0.55", "_0.6-0.7", "_0.8-0.85", "_1-1", "_0-0"
// "_0.5","_0.8","_0.9","_0.925","_0.95","_0.99","_0.99"