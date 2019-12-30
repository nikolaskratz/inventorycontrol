import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
public class SimulationManager {

	static int iteration;
	static int rowCount;
	static int ofset;
	static double serviceLevel;
	static double stochastikH;
	static double stochastikV;
	static int[] rVals;
	static double[] ltdV18;
	static double[] stdV18;
	static double[] stdV19;
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
	static double[] initialInv;
	
	static String version = "_default";
	
	public SimulationManager(String version, int iteration) throws Exception {
		this.version=version;
		this.iteration=iteration;
		main(null);
	}
		
	public static void main(String[] args) throws Exception {	
		System.out.println("\niteration: "+iteration+"; filename: "+version);
		rowCount=3879;
		ofset=0;
		
		getData();
		System.out.println("Got Data");
		
		rVals = new int[rowCount];
		for(int i=0;i<rVals.length;i++) {			
			RQ rq = new RQ(ltdV18[i], stdV18[i], eoq[i], serviceLevel);
			rVals[i]=rq.getR();
		}
		System.out.println("Calculated r,Q");

		simulateDem();
		System.out.println("Simulated DemandDriven");

		simulateCons();
		System.out.println("Simulated ConsumptionDriven");
		
		
		TotalCost t = new TotalCost(holding, backorder, fixPlan, fixCons, optInv, optLot, inv, q);
		totalPlanCost = t.getTotalPlanCost();
		totalConsCost = t.getTotalConsCost();
		System.out.println("Calculated total cost");
		
		writeResults();
		System.out.println("Wrote Results");
		System.out.println("done");
	}
	
	public static void simulateCons() {
		inv = new double[rowCount][240];
		q = new double[rowCount][240];
		double initInv;
		boolean orderPending = false; //flag to check if already ordered
		
		for(int i=0; i<rowCount; i++) {
			initInv = initialInv[i]; 					
			for(int j=0; j<240; j++) {
				if(j==0) {
					inv[i][j] = initInv - cons19[i][j]+q[i][j];
				} else {
					inv[i][j] = inv[i][j-1]+q[i][j]-cons19[i][j];
				}
				if(q[i][j]>0) orderPending = false;
				if(j+l[i]<240 && !orderPending) {
					while(inv[i][j]+q[i][j+l[i]]<rVals[i]) {
						q[i][j+l[i]]+=eoq[i];
						orderPending = true;
					}
				}
			}
		}
	}
	
	public static void simulateDem() {
		double dif;
		double sumDif=0;
		int periodInd=0;
		
		for(int i=0; i<rowCount; i++) {
			for(int j=0; j<240; j++) {
				
				//deviation from plan
				dif = plan19[i][j]-cons19[i][j];
				
				//setting real inventory levels 
				if(j==0) {
					optInv[i][j] = optInv[i][j]+dif;
				} else {
					optInv[i][j] = optInv[i][j-1]+optLot[i][j]-cons19[i][j]+dif;
				}
				
				//adapt next (optimal) incoming order (after lead time)
				periodInd=j+l[i];
				while(periodInd<239 && optLot[i][periodInd]<=0) { //finding next order
					periodInd++;
				}
				if(periodInd<240 && optLot[i][periodInd]>0) {
					if(dif<0) {
						optLot[i][periodInd] += Math.abs(dif);
					} else if(dif>0) {
						optLot[i][periodInd] -= dif;
					}
					if(optLot[i][periodInd]<0) {
						optLot[i][periodInd]=0;
					} else if(optLot[i][periodInd]<minL[i]) {
						optLot[i][periodInd]=minL[i];
					}
				}
			}
		}
	}
	
	public static void getData() throws Exception {
		FileInputStream fs = new FileInputStream("data_daily"+version+".xlsx");
		FileInputStream fs2 = new FileInputStream("solutions"+version+".xlsx");
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
		stdV19 = new double[rowCount];
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
		initialInv = new double[rowCount];
		
		serviceLevel = params.getRow(16).getCell(2).getNumericCellValue();
		fixPlan = params.getRow(4).getCell(2).getNumericCellValue();
		fixCons = params.getRow(5).getCell(2).getNumericCellValue();
		stochastikH = params.getRow(10).getCell(5).getNumericCellValue();
		stochastikV = params.getRow(13).getCell(5).getNumericCellValue();
		
		for(int i=0; i<rowCount; i++) {
			
			ltdV18[i] = v18.getRow(i+2+ofset).getCell(9).getNumericCellValue();
			eoq[i] = v18.getRow(i+2+ofset).getCell(10).getNumericCellValue();
			stdV18[i] = v18.getRow(i+2+ofset).getCell(8).getNumericCellValue();
			stdV19[i] = demand19.getRow(i+2+ofset).getCell(8).getNumericCellValue();
			ss[i]= diff18.getRow(i+2+ofset).getCell(10).getNumericCellValue();
			l[i]= (int) v18.getRow(i+2+ofset).getCell(1).getNumericCellValue();
			minL[i] = v18.getRow(i+2+ofset).getCell(3).getNumericCellValue();
			round[i] = v18.getRow(i+2+ofset).getCell(2).getNumericCellValue();
			backorder[i] = v18.getRow(i+2+ofset).getCell(6).getNumericCellValue();
			holding[i] = v18.getRow(i+2+ofset).getCell(5).getNumericCellValue();
			initialInv[i] = demand19.getRow(i+2+ofset).getCell(10).getNumericCellValue();
			for(int j=0; j<240; j++) {
				
				cons19[i][j] = demand19.getRow(i+2+ofset).getCell(j+11).getNumericCellValue();
				plan19[i][j] = bedarf19.getRow(i+2+ofset).getCell(j+11).getNumericCellValue();
				optInv[i][j] = optimalInv.getRow(i+2+ofset).getCell(j+11).getNumericCellValue();
				optLot[i][j] = optimalLot.getRow(i+2+ofset).getCell(j+11).getNumericCellValue();
			}
		}
	}
	
	public static void writeResults() throws Exception {
		FileInputStream fs = new FileInputStream("solutions"+version+".xlsx");
		Workbook wb = WorkbookFactory.create(fs);
		Sheet sh = wb.getSheet("RQ");
		
		//write r,Q
		for(int i=0; i<rVals.length; i++) {
			Row r = sh.createRow(i+1+ofset);
			r.createCell(1).setCellValue(rVals[i]);
			r.createCell(2).setCellValue(eoq[i]);
		}
		FileOutputStream fos = new FileOutputStream("solutions"+version+".xlsx");
		wb.write(fos);
		fos.close();
		
		//totalDemand calculation
		double[] totalDemand19 = new double[rowCount];
		for(int i=0; i<rowCount; i++) {
			for(int j=0; j<240; j++) {
				totalDemand19[i] += cons19[i][j];
			}
		}
		System.out.println("wrote solutions");
		
		//write simulation results
		FileInputStream fs2 = new FileInputStream("simulation"+version+".xlsx");
		Workbook wb2 = WorkbookFactory.create(fs2);
		Sheet planInv = wb2.getSheet("plan_sim_inv");
		Sheet planLot = wb2.getSheet("plan_sim_lot");
		Sheet consInv = wb2.getSheet("cons_sim_inv");
		Sheet consLot = wb2.getSheet("cons_sim_lot");
		Sheet totalCost = wb2.getSheet("total_cost");
		for(int i=0; i<rowCount; i++) {
			Row rI = planInv.createRow(i+2+ofset);
			Row rL = planLot.createRow(i+2+ofset);
			Row rCI = consInv.createRow(i+2+ofset);
			Row rCL = consLot.createRow(i+2+ofset);
			Row rCost = totalCost.createRow(i+1+ofset);
			rCost.createCell(5).setCellValue(totalPlanCost[i]);
			rCost.createCell(6).setCellValue(totalConsCost[i]);
			rI.createCell(2).setCellValue(holding[i]);
			rI.createCell(3).setCellValue(backorder[i]);
			rI.createCell(4).setCellValue(fixPlan);
			rI.createCell(5).setCellValue(fixCons);
			rI.createCell(7).setCellValue(totalDemand19[i]);
			rI.createCell(8).setCellValue(stdV19[i]);
			rI.createCell(9).setCellValue(serviceLevel);
			rI.createCell(10).setCellValue(stochastikH);
			for(int j=0; j<240; j++) {
				rI.createCell(j+11).setCellValue(optInv[i][j]);
				rL.createCell(j+11).setCellValue(optLot[i][j]);
				rCI.createCell(j+11).setCellValue(inv[i][j]);
				rCL.createCell(j+11).setCellValue(q[i][j]);
				
			}
		}
		FileOutputStream fos2 = new FileOutputStream("simulation"+version+".xlsx");
		wb2.write(fos2);
		fos2.close();
		
		System.out.println("wrote simulation");

		
		//write info to Analysis sheet
		FileInputStream fs3 = new FileInputStream("Analysis.xlsx");
		Workbook wb3 = WorkbookFactory.create(fs3);
		Sheet s = wb3.getSheet("SingleMaterialData");
		for(int i=0; i<rowCount; i++) {
			Row r = s.createRow(i+1+ofset+iteration*3879);
			r.createCell(0).setCellValue(iteration);
			r.createCell(1).setCellValue(i);
			r.createCell(2).setCellValue(stochastikH);
			r.createCell(3).setCellValue(stochastikV);
			r.createCell(4).setCellValue(fixPlan);
			r.createCell(5).setCellValue(fixCons);
			r.createCell(6).setCellValue(fixPlan-fixCons);
			r.createCell(7).setCellValue(serviceLevel);
			r.createCell(8).setCellValue(l[i]);
			r.createCell(9).setCellValue(holding[i]/0.2*240);
			r.createCell(10).setCellValue(holding[i]);
			r.createCell(11).setCellValue(backorder[i]);
			r.createCell(12).setCellValue(totalDemand19[i]);
			r.createCell(13).setCellValue(stdV19[i]);
			r.createCell(14).setCellValue(minL[i]);
			r.createCell(15).setCellValue(initialInv[i]);
			r.createCell(16).setCellValue(ss[i]);
			r.createCell(17).setCellValue(eoq[i]);
			r.createCell(18).setCellValue(totalPlanCost[i]);
			r.createCell(19).setCellValue(totalConsCost[i]);
		}
		FileOutputStream fos3 = new FileOutputStream("Analysis.xlsx");
		wb3.write(fos3);
		fos3.close();
		System.out.println("wrote analysis");

	}
	
	

}