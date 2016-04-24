
package astrolabe;

import caa.CAACoordinateTransformation;

public class ReplacementHelper extends Model {

	public static void registerYMD( String key, caa.CAADate date ) throws ParameterNotValidException {

		ReplacementHelper.registerNumber( key+"Y", (int) date.Year() ) ;
		ReplacementHelper.registerNumber( key+"M", (int) date.Month() ) ;
		ReplacementHelper.registerNumber( key+"D", (int) date.Day() ) ;
	}

	public static void registerHMS( String key, double hms, double precision ) throws ParameterNotValidException {
		double h ;

		h = CAACoordinateTransformation.RadiansToHours( hms<0?hms-.000000001:hms+.000000001 ) ;

		ReplacementHelper.registerNumber( key+"hrs", (int) h ) ;
		ReplacementHelper.registerMS( key+"h", java.lang.Math.abs( hms ), precision ) ;
	}

	public static void registerDMS( String key, double dms, double precision ) throws ParameterNotValidException {
		double d ;

		d = CAACoordinateTransformation.RadiansToDegrees( dms<0?dms-.000000001:dms+.000000001 ) ;

		ReplacementHelper.registerNumber( key+"deg", (int) d ) ;
		ReplacementHelper.registerMS( key+"d", java.lang.Math.abs( dms ), precision ) ;
	}

	public static void registerMS( String key, double value, double precision ) throws ParameterNotValidException {
		double v, p, ms ;
		int m, s, f ;

		v = CAACoordinateTransformation.RadiansToHours( value ) ;

		p = java.lang.Math.pow( 10, precision ) ;
		ms = v-(int) v ;
		m = (int) ( 60*ms ) ;
		s = (int) ( 60*( 60*ms-m ) ) ;
		f = (int) ( ( ( 60*( 60*ms-m ) )-s )*p+.5 ) ;
		ReplacementHelper.registerNumber( key+"min", m ) ;
		ReplacementHelper.registerNumber( key+"sec", java.lang.Math.abs( s ) ) ;
		ReplacementHelper.registerNumber( key+"frc", java.lang.Math.abs( f ) ) ;
	}

	public static void registerNumber( String key, double value, int precision ) throws ParameterNotValidException {
		double p, v ;

		if (precision < 0 ) {
			throw new ParameterNotValidException() ;
		}

		p = java.lang.Math.pow( 10, precision ) ;
		v = (int) ( value*p+.5 )/p ;

		ReplacementHelper.registerNumber( key, v ) ;
	}

	public static void registerNumber( String key, double value ) throws ParameterNotValidException {
		Registry r ;

		r = new Registry() ;
		r.register( key, (Object) new Double( value ).toString() ) ;
	}

	public static void registerNumber( String key, int value ) throws ParameterNotValidException {
		Registry r ;

		r = new Registry() ;
		r.register( key, (Object) new Integer( value ).toString() ) ;
	}

	public static void registerName( String key, String value ) throws ParameterNotValidException {
		Registry r ;

		r = new Registry() ;
		r.register( key, new String( value ) ) ;
	}
}
