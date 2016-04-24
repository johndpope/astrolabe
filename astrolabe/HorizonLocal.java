
package astrolabe;

import caa.CAACoordinateTransformation ;
import caa.CAANutation;
import caa.CAADate;

public class HorizonLocal extends Model implements Horizon {

	private double grayscale ;
	private double la ;
	private double ST ;
	private double lo ;

	public HorizonLocal( astrolabe.model.HorizonType hoT, CAADate epoch ) {
		this.grayscale = getClassNode( hoT.getName(), "practicality" ).getDouble( hoT.getPracticality(), 0 ) ;

		try {
			la = Model.condense( hoT.getLatitude() ) ;
			ReplacementHelper.registerDMS( "latitude", la, 2 ) ;
		} catch ( ParameterNotValidException e ) {}
		try {
			lo = Model.condense( hoT.getLongitude() ) ;
			ReplacementHelper.registerDMS( "longitude", lo, 2 ) ;
		} catch ( ParameterNotValidException e ) {
			lo = 0 ;
		}
		try {
			double lt, ra0, lo0, la0, e ;
			CAADate date ;

			date = Model.condense( hoT.getDate() ) ;
			lt = date.Hour()+date.Minute()/60.+date.Second()/3600 ;
			ReplacementHelper.registerHMS( "local",
					CAACoordinateTransformation.HoursToRadians( lt ), 2 ) ;

			lo0 = CAAHelper.MeanEclipticLongitude( date.Julian() ) ;
			la0 = CAAHelper.MeanEclipticLatitude( date.Julian() ) ;
			e = CAANutation.MeanObliquityOfEcliptic( epoch.Julian() ) ;

			ra0 = CAACoordinateTransformation.Ecliptic2Equatorial( lo0, la0, e )[0] ;

			ST = CAACoordinateTransformation.HoursToRadians(
					CAACoordinateTransformation.MapTo0To24Range( ra0+lt-12 ) ) ;
			ReplacementHelper.registerHMS( "sidereal", ST, 2 ) ;
		} catch ( ParameterNotValidException e ) {
			ST = 0 ;
		}
	}

	public double[] convert( double[] holo ) {
		return convert( holo[0], holo[1] ) ;
	}

	public double[] convert( double A, double h ) {
		double[] r ;

		r = CAACoordinateTransformation.Horizontal2Equatorial(
				CAACoordinateTransformation.RadiansToDegrees( A ),
				CAACoordinateTransformation.RadiansToDegrees( h ),
				CAACoordinateTransformation.RadiansToDegrees( la ) ) ;

		// r[0] is HA is ST-lo-RA.
		r[0] = ST-CAACoordinateTransformation.HoursToRadians( r[0] ) ;
		r[1] = CAACoordinateTransformation.DegreesToRadians( r[1] ) ;

		return r ;
	}

	public void initPS( PostscriptStream ps ) {
		ps.operator.setgray( grayscale ) ; 
	}

	public double[] unconvert( double[] eq ) {
		return unconvert( eq[0], eq[1] ) ;
	}

	public double[] unconvert( double RA, double d ) {
		double[] r ;

		r = CAACoordinateTransformation.Equatorial2Horizontal(
				CAACoordinateTransformation.RadiansToHours( this.ST-RA ),
				CAACoordinateTransformation.RadiansToDegrees( d ),
				CAACoordinateTransformation.RadiansToDegrees( la ) ) ;

		r[0] = CAACoordinateTransformation.DegreesToRadians( r[0] ) ;
		r[1] = CAACoordinateTransformation.DegreesToRadians( r[1] ) ;

		return r ;
	}

	public double getLa() {
		return la ;
	}

	public double getST() {
		return ST ;
	}

	public double getLo() {
		return lo ;
	}
	public boolean isEcliptical() {
		return false ;
	}

	public boolean isEquatorial() {
		return false ;
	}

	public boolean isGalactic() {
		return false ;
	}

	public boolean isLocal() {
		return true ;
	}
}
