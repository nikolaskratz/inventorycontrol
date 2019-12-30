import org.apache.commons.math3.distribution.NormalDistribution;

public class RQ {

	static double leadTimeDemand;
	static double std;
	static double eoq;
	static double serviceLevel;
	
	static int r;
	static int r0;
	static int r1;
	
	public RQ(double leadTimeDemand, double std, double eoq, double serviceLevel) {
		RQ.leadTimeDemand = leadTimeDemand;
		RQ.std = std;
		RQ.eoq = eoq;
		RQ.serviceLevel = serviceLevel;
		main(null);
	}

	public static void main(String[] args) {
		r0=(int) -eoq;
		r1= (int) (leadTimeDemand*2+std*2+eoq*2);
		r=(Math.abs(r0)+r1)/2;
		double error=100;
		double aproxSl=999;
		int artificialBreak=0;
		while(error>0.001) {
			aproxSl=sl(r);
			if(aproxSl<serviceLevel) {
				r0=r;
			} else r1=r;
			r=(Math.abs(r0)+r1)/2;
			error=Math.abs(serviceLevel-aproxSl);
			artificialBreak++;
			if(artificialBreak>1000) break;	
		}
		if(r<0) r=0;
	}
	
	public static double sl(double r) {
		double sl;
		sl = 1-(std/eoq)*g((r-leadTimeDemand)/std);
		return sl;
	}
	
	public static double g(double x) {
		double g;
		NormalDistribution normal = new NormalDistribution();
		g=normal.density(x)-x*(1-normal.cumulativeProbability(x));
		return g;
	}

	public static int getR() {
		return r;
	}
	
	

}