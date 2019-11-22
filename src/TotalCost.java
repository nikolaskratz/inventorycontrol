
public class TotalCost {

	static double[] h;
	static double[] b;
	static double fixPlan;
	static double fixCons;
	static double[][] planInv;
	static double[][] planLot;
	static double[][] consInv;
	static double[][] consLot;
	
	static double[] totalPlanCost;
	static double[] totalConsCost;
	
	
	
	public TotalCost(double[] h, double[] b, double fixPlan, double fixCons, double[][] planInv, double[][] planLot,
			double[][] consInv, double[][] consLot) {
		System.out.println("created");
		this.h = h;
		this.b = b;
		this.fixPlan = fixPlan;
		this.fixCons = fixCons;
		this.planInv = planInv;
		this.planLot = planLot;
		this.consInv = consInv;
		this.consLot = consLot;
		totalCost();
	}
	
	public static void totalCost() {
		
		totalPlanCost = new double[planInv.length];
		totalConsCost = new double[consInv.length];
		
		for(int i=0; i<totalPlanCost.length; i++) {
			for(int j=0; j<planInv[i].length; j++) {
				if(planLot[i][j]>0.001) {
					totalPlanCost[i] += fixPlan;
					System.out.println("orderCost");
				}
				if(planInv[i][j]<0) {
					totalPlanCost[i] += Math.abs(planInv[i][j])*b[i];
					
				}
				if(planInv[i][j]>0) totalPlanCost[i] += planInv[i][j]*h[i];
				
				if(consLot[i][j]>0) totalConsCost[i] += fixCons;
				if(consLot[i][j]<0) totalConsCost[i] += Math.abs(consLot[i][j])*b[i];
				if(consLot[i][j]>0) totalConsCost[i] += consLot[i][j]*h[i];
			}
			System.out.println("mat:"+i+" PCost:"+totalPlanCost[i]+", CCost:"+totalConsCost[i]);
		}
	}
	
	public static double[] getTotalPlanCost() {
		return totalPlanCost;
	}

	public static double[] getTotalConsCost() {
		return totalConsCost;
	}
	
}
