
public class multipleModelManager {

	public static void main(String[] args) throws Exception {
		int iterations = 3;
//		String[] names = new String[iterations];
		String[] names = {"_0.2-0.4_0.8", "_0.2-0.4_0.925", "_0.2-0.4_0.9999"};
		
		for(int i=0; i<iterations; i++) {
			SimulationManager sm = new SimulationManager(names[i], i);
		}

	}

}
