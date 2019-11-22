import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
public class SimulationManager {

	static int rowCount;
	static double serviceLevel;
	static int[] rVals;
	static double[] ltdV18;
	static double[] stdV18;
	static double[] eoq;
	static double[] ss; //safety stock
	static double[] backorder;
	static double[] holding;
	static int[] l; //lead time
	static double[] minL; //mindestlosgröße
	static double[] round; //rundungsparameter
	static double[][] cons19; //Verbrauch 2019
	static double[][] plan19; //Bedarf 2019
	static double[][] optInv; //optimal inventory calculated from optimization
	static double[][] optLot; //optimal lot sizes calculated from optimization
	static double[][] inv; //inventory for cons simulation
	static double[][] q; //order quantity for cons simulation
	static double fixPlan;
	static double fixCons;
	static double[] totalPlanCost;
	static double[] totalConsCost;
	
	
		
	public static void main(String[] args) throws Exception {

		rowCount=100;
		getData();
		System.out.println("Got Data");
		//calculate r,Q values
		rVals = new int[rowCount];
		for(int i=0;i<rVals.length;i++) {			
			RQ rq = new RQ(ltdV18[i], stdV18[i], eoq[i], serviceLevel);
//			System.out.println(i);
			rVals[i]=rq.getR();
		}
		System.out.println("Calculated r,Q");

		simulatePlan();
		System.out.println("Simulated Plan");

		simulateCons();
		System.out.println("Simulated Cons");
		
		
		TotalCost t = new TotalCost(holding, backorder, fixPlan, fixCons, optInv, optLot, inv, q);
		totalPlanCost = t.getTotalPlanCost();
		totalConsCost = t.getTotalConsCost();

		writeResults();
		System.out.println("Wrote Results");

		System.out.println("done");
	}
	
	public static void simulateCons() {
		inv = new double[rowCount][240];
		q = new double[rowCount][240];
		double initialInv;
		boolean orderPending = false; //flag to check if already ordered
		
		for(int i=0; i<rowCount; i++) {
			initialInv = rVals[i]; 					//CHANGE to real initial inventory!!!!!!!!!
			for(int j=0; j<240; j++) {
				if(j==0) {
					inv[i][j] = initialInv - cons19[i][j]+q[i][j];
				} else {
					inv[i][j] = inv[i][j-1]+q[i][j]-cons19[i][j];
				}
				if(q[i][j]>0) orderPending = false;
				if(j+l[i]<240 && !orderPending) {
					while(inv[i][j]+q[i][j+l[i]]<rVals[i]) {
//						System.out.println("update q at "+i+", "+j+l[i]);
						q[i][j+l[i]]+=eoq[i];
						orderPending = true;
					}
				}
			}
		}
	}
	
	public static void simulatePlan() {
		double dif;
		
		for(int i=0; i<rowCount; i++) {
			for(int j=0; j<240; j++) {
				
				dif = plan19[i][j]-cons19[i][j]; //Abweichung vom Plan
				//setting inventory 
				if(j==0) {
					optInv[i][j] = optInv[i][j]+dif;
				} else {
					optInv[i][j] = optInv[i][j-1]+optLot[i][j]-cons19[i][j]+dif;
				}
				
				if(dif<0) {
					if(optInv[i][j]<ss[i]) {
						if(j+l[i]<240) optLot[i][j+l[i]]=optLot[i][j+l[i]]+dif;
					}
				} else {
					//wenn Planung zu hoch war --> Bestellung in t+l entsprechend reduzieren
					if(j+l[i]<240) optLot[i][j+l[i]]=optLot[i][j+l[i]]-dif;
				}
				if(j+l[i]<240) {
					//minimum Lot size must be fullfilled
					if(optLot[i][j+l[i]]<minL[i]) {
						optLot[i][j+l[i]]=minL[i];
					}
					//round parameter
					if(round[i]==0) round[i]=1;
					while(((int) optLot[i][j+l[i]])%round[i]!=0) {
//						System.out.println("it:"+i+", stuck "+round[i]+", optLot(t+l)="+optLot[i][j+l[i]]);
						optLot[i][j+l[i]]= Math.ceil(optLot[i][j+l[i]])+1;
					}
				}
			}
		}
	}
	
	public static void getData() throws Exception {
//		FileInputStream fs = new FileInputStream("C:\\Users\\nikol\\Desktop\\VT_Garching_Lotsizing\\Daily\\data_daily.xlsx");
		FileInputStream fs = new FileInputStream("data_daily.xlsx");
		FileInputStream fs2 = new FileInputStream("solutions.xlsx");
		Workbook wb2 = WorkbookFactory.create(fs2);
		Workbook wb = WorkbookFactory.create(fs);
		Sheet params = wb.getSheet("Simulation_parameter");		
		Sheet v18 = wb.getSheet("Verbrauch18");
		Sheet diff18 = wb.getSheet("Differenz_VerbrBedarf18");
		Sheet demand19 = wb.getSheet("Verbrauch19");
		Sheet bedarf19 = wb.getSheet("Generierter_Bedarf19");
		Sheet optimalInv = wb2.getSheet("Opt_Inv");
		Sheet optimalLot = wb2.getSheet("Opt_Lot");
		
		ltdV18 = new double[rowCount];
		stdV18 = new double[rowCount];
		eoq = new double[rowCount];
		cons19 = new double[rowCount][240];
		plan19 = new double[rowCount][240];
		optInv = new double[rowCount][240];
		optLot = new double[rowCount][240];
		ss = new double[rowCount];
		l = new int[rowCount];
		minL = new double[rowCount];
		round = new double[rowCount];
		backorder = new double[rowCount];
		holding = new double[rowCount];
		
		serviceLevel = params.getRow(16).getCell(2).getNumericCellValue();
		fixPlan = params.getRow(4).getCell(2).getNumericCellValue();
		fixCons = params.getRow(5).getCell(2).getNumericCellValue();
		
		for(int i=0; i<rowCount; i++) {
			
			ltdV18[i] = v18.getRow(i+2).getCell(9).getNumericCellValue();
			eoq[i] = v18.getRow(i+2).getCell(10).getNumericCellValue();
			stdV18[i] = v18.getRow(i+2).getCell(8).getNumericCellValue();
			ss[i]= diff18.getRow(i+2).getCell(10).getNumericCellValue();
			l[i]= (int) v18.getRow(i+2).getCell(1).getNumericCellValue();
			minL[i] = v18.getRow(i+2).getCell(3).getNumericCellValue();
			round[i] = v18.getRow(i+2).getCell(2).getNumericCellValue();
			backorder[i] = v18.getRow(i+2).getCell(6).getNumericCellValue();
			holding[i] = v18.getRow(i+2).getCell(5).getNumericCellValue();
			System.out.println(l[i]);
			for(int j=0; j<240; j++) {
				cons19[i][j] = demand19.getRow(i+2).getCell(j+11).getNumericCellValue();
				plan19[i][j] = bedarf19.getRow(i+2).getCell(j+11).getNumericCellValue();
				optInv[i][j] = optimalInv.getRow(i+2).getCell(j+11).getNumericCellValue();
				optLot[i][j] = optimalLot.getRow(i+2).getCell(j+11).getNumericCellValue();
			}
		}
	}
	
	public static void writeResults() throws Exception {
		FileInputStream fs = new FileInputStream("solutions.xlsx");
		Workbook wb = WorkbookFactory.create(fs);
		Sheet sh = wb.getSheet("RQ");
		
		//write r,Q
		for(int i=0; i<rVals.length; i++) {
			Row r = sh.createRow(i+1);
			r.createCell(1).setCellValue(rVals[i]);
			r.createCell(2).setCellValue(eoq[i]);
		}
		FileOutputStream fos = new FileOutputStream("solutions.xlsx");
		wb.write(fos);
//		fos.close();
		
		//write simulation results
		FileInputStream fs2 = new FileInputStream("simulation.xlsx");
		Workbook wb2 = WorkbookFactory.create(fs2);
		Sheet planInv = wb2.getSheet("plan_sim_inv");
		Sheet planLot = wb2.getSheet("plan_sim_lot");
		Sheet consInv = wb2.getSheet("cons_sim_inv");
		Sheet consLot = wb2.getSheet("cons_sim_lot");
		Sheet totalCost = wb2.getSheet("total_cost");
		for(int i=0; i<rowCount; i++) {
			Row rI = planInv.createRow(i+2);
			Row rL = planLot.createRow(i+2);
			Row rCI = consInv.createRow(i+2);
			Row rCL = consLot.createRow(i+2);
			Row rCost = totalCost.createRow(i+1);
			rCost.createCell(5).setCellValue(totalPlanCost[i]);
			rCost.createCell(6).setCellValue(totalConsCost[i]);
			for(int j=0; j<240; j++) {
				rI.createCell(j+11).setCellValue(optInv[i][j]);
				rL.createCell(j+11).setCellValue(optLot[i][j]);
				rCI.createCell(j+11).setCellValue(inv[i][j]);
				rCL.createCell(j+11).setCellValue(q[i][j]);
				
			}
		}
		FileOutputStream fos2 = new FileOutputStream("simulation.xlsx");
		wb2.write(fos2);
//		fos2.close();
	}

}