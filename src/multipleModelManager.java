
public class multipleModelManager {

	public static void main(String[] args) throws Exception {
		int iterations = 1;
//		String[] names = new String[iterations];
		String[] names = {"_1-1_0.9999"};
		
		for(int i=0; i<iterations; i++) {
			SimulationManager sm = new SimulationManager(names[i], i+46);
		}

	}

}

//"_0.2-0.4_100-100"