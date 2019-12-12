import org.apache.commons.math3.distribution.NormalDistribution;

public class RQ {

	static double leadTimeDemand=0.125;
	static double std=0.01;
	static double eoq=2.27;
	static double serviceLevel=0.98;
	
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
		if(leadTimeDemand<0) {
			r=0;
			System.out.println("sl="+sl(r)+", r="+r);
			return;
		}
		r0=(int) -eoq;
//		r0=0;
		r1= (int) (leadTimeDemand*2+std*2+eoq*2);
		r=(r0+r1)/2;
		double error=100;
		double aproxSl=999;
		int artificialBreak=0;
		while(error>0.01) {
//			System.out.println(artificialBreak+" r="+r+", r0="+r0+", r1="+r1+", sl="+aproxSl);
			aproxSl=sl(r);
//			if(aproxSl<serviceLevel) {
//				r0+=0.1*eoq;
//			} else r1-=0.1*eoq;
			if(aproxSl<serviceLevel) {
				r0=r;
			} else r1=r;
			r=(Math.abs(r0)+r1)/2;
			error=Math.abs(serviceLevel-aproxSl);
			artificialBreak++;
			if(artificialBreak>1000) break;
//			System.out.println("r0="+r0+" r1="+r1 +" r="+r+" sl="+aproxSl);			
		}
		if(r<0) r=0;
		String s = "";
		if(error>0.3) s=" Large error";
//		System.out.println("sl="+aproxSl+", r="+r+s);
	}
	
	public static double sl(double r) {
		double sl;
		sl = 1-(std/eoq)*g((r-leadTimeDemand)/std);
//		System.out.println(sl+" r:"+r);
		return sl;
	}
	
	public static double g(double x) {
		double g;
		NormalDistribution normal = new NormalDistribution();
		g=normal.density(x)-x*(1-normal.cumulativeProbability(x));
//		System.out.println(g);
		return g;
	}

	public static int getR() {
		return r;
	}
	
	

}