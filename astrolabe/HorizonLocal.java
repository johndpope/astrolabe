
package astrolabe;

import java.util.Calendar;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import caa.CAA2DCoordinate;
import caa.CAACoordinateTransformation ;
import caa.CAADate;
import caa.CAANutation;

@SuppressWarnings("serial")
public class HorizonLocal extends astrolabe.model.HorizonLocal implements PostscriptEmitter, Projector {

	private Projector projector ;
	// qualifier key (QK_)
	private final static String QK_LOCALTIME	= "localtime" ;
	private final static String QK_SIDEREAL		= "sidereal" ;

	public HorizonLocal( Projector projector ) {
		this.projector = projector ;
	}

	public double date() {
		Calendar calendar ;
		CAADate datetime ;
		double jd ;

		if ( getDate() == null ) {
			calendar = Calendar.getInstance() ;

			datetime = new CAADate(
					calendar.get( Calendar.YEAR ),
					calendar.get( Calendar.MONTH ),
					calendar.get( Calendar.DAY_OF_MONTH ),
					calendar.get( Calendar.HOUR_OF_DAY ),
					calendar.get( Calendar.MINUTE ), 0, true ) ;
			jd = datetime.Julian() ;
			datetime.delete() ;

			return jd ;
		} else
			return valueOf( getDate() ) ;
	}

	public double longitude() {
		if ( getLongitude() == null )
			return 0 ;
		else
			return valueOf( getLongitude() ) ;
	}

	public double getLT( double jd ) {
		double lo, lt ;
		CAADate d ;

		d = new CAADate() ;
		d.Set( jd, true ) ;

		lt = CAACoordinateTransformation.HoursToDegrees( d.Hour()+d.Minute()/60.+d.Second()/3600 ) ;
		lo = longitude() ;

		d.delete() ;

		return lt+( lo>180?lo-360:lo ) ;
	}

	public double getST( double jd ) {
		double la0, lo0, e, ra0, lt ;
		Double Epoch ;
		double epoch ;
		CAA2DCoordinate c ;

		la0 = BodySun.meanEclipticLatitude( jd ) ;
		lo0 = BodySun.meanEclipticLongitude( jd ) ;
		Epoch = (Double) Registry.retrieve( astrolabe.Epoch.RK_EPOCH ) ;
		if ( Epoch == null )
			epoch = astrolabe.Epoch.defoult() ;
		else
			epoch = Epoch.doubleValue() ;
		e = CAANutation.MeanObliquityOfEcliptic( epoch ) ;

		c = CAACoordinateTransformation.Ecliptic2Equatorial( lo0, la0, e ) ;
		ra0 = CAACoordinateTransformation.HoursToDegrees( c.X() ) ;

		lt = getLT( jd ) ;

		return CAACoordinateTransformation.MapTo0To360Range( ra0+lt-180/*12h*/ ) ;
	}

	public void register() {
		double jd ;
		DMS dms ;

		jd = date() ;

		dms = new DMS( getLT( jd )/15 ) ;
		dms.register( this, QK_LOCALTIME ) ;
		dms.set( getST( jd )/15, -1 ) ;
		dms.register( this, QK_SIDEREAL ) ;
	}

	public void degister() {
		DMS.degister( this, QK_LOCALTIME ) ;
		DMS.degister( this, QK_SIDEREAL ) ;
	}

	public void headPS( ApplicationPostscriptStream ps ) {
		GSPaintColor practicality ;

		practicality = new GSPaintColor( getPracticality() ) ;
		practicality.headPS( ps ) ;
		practicality.emitPS( ps ) ;
		practicality.tailPS( ps ) ;
	}

	public void emitPS( ApplicationPostscriptStream ps ) {
		astrolabe.model.Circle circle ;
		astrolabe.model.Body body ;
		PostscriptEmitter emitter ;

		for ( int an=0 ; an<getAnnotationStraightCount() ; an++ ) {
			emitter = new AnnotationStraight() ;
			getAnnotationStraight( an ).copyValues( emitter ) ;

			ps.operator.gsave() ;

			emitter.headPS( ps ) ;
			emitter.emitPS( ps ) ;
			emitter.tailPS( ps ) ;

			ps.operator.grestore() ;
		}

		for ( int cl=0 ; cl<getCircleCount() ; cl++ ) {
			circle = getCircle( cl ) ;

			if ( circle.getCircleParallel() != null ) {
				circle( ps, circle.getCircleParallel() ) ;
			} else if ( circle.getCircleMeridian() != null ) {
				circle( ps, circle.getCircleMeridian() ) ;
			} else if ( circle.getCircleSouthernPolar() != null ) {
				circle( ps, circle.getCircleSouthernPolar() ) ;
			} else if ( circle.getCircleNorthernPolar() != null ) {
				circle( ps, circle.getCircleNorthernPolar() ) ;
			} else if (  circle.getCircleSouthernTropic() != null ) {
				circle( ps, circle.getCircleSouthernTropic() ) ;
			} else { // circle.getCircleNorthernTropic() != null
				circle( ps, circle.getCircleNorthernTropic() ) ;
			}
		}

		for ( int bd=0 ; bd<getBodyCount() ; bd++ ) {
			body = getBody( bd ) ;

			if ( body.getBodyStellar() != null ) {
				body( ps, body.getBodyStellar() ) ;
			} else if ( body.getBodyAreal() != null ) {
				body( ps, body.getBodyAreal() ) ;
			} else if ( body.getBodyPlanet() != null ) {
				body( ps, body.getBodyPlanet() ) ;
			} else if ( body.getBodyMoon() != null ) {
				body( ps, body.getBodyMoon() ) ;
			} else if ( body.getBodySun() != null ) {
				body( ps, body.getBodySun() ) ;
			} else if ( body.getBodyElliptical() != null ) {
				body( ps, body.getBodyElliptical() ) ;
			} else { // body.getBodyParabolical() != null
				body( ps, body.getBodyParabolical() ) ;
			}
		}
	}

	public void tailPS( ApplicationPostscriptStream ps ) {
	}

	public double[] project( double[] ho ) {
		return project( ho[0], ho[1] ) ;
	}

	public double[] project( double A, double h ) {
		return projector.project( convert( A, h ) ) ;
	}

	public double[] unproject( double[] xy ) {
		return unproject( xy[0], xy[1] ) ;
	}

	public double[] unproject( double x, double y ) {
		return unconvert( projector.unproject( x, y ) ) ;
	}

	public double[] convert( double[] ho ) {
		return convert( ho[0], ho[1] ) ;
	}

	public double[] convert( double A, double h ) {
		double[] r = new double[2] ;
		CAA2DCoordinate c ;
		double la ;

		la = valueOf( getLatitude() ) ;

		c = CAACoordinateTransformation.Horizontal2Equatorial( A, h, la ) ;
		r[0] = CAACoordinateTransformation.HoursToDegrees( c.X() ) ;
		r[1] = c.Y() ;

		// r[0] is HA is ST-lo-RA.
		r[0] = getST( date() )-r[0] ;

		return r ;
	}

	public double[] unconvert( double[] eq ) {
		return unconvert( eq[0], eq[1] ) ;
	}

	public double[] unconvert( double RA, double d ) {
		double[] r = new double[2];
		CAA2DCoordinate c ;
		double la ;

		la = valueOf( getLatitude() ) ;

		c = CAACoordinateTransformation.Equatorial2Horizontal(
				CAACoordinateTransformation.DegreesToHours( getST( date() )-RA ), d, la ) ;
		r[0] = c.X() ;
		r[1] = c.Y() ;

		return r ;
	}

	private void circle( ApplicationPostscriptStream ps, astrolabe.model.CircleMeridian peer ) {
		CircleMeridian circle ;

		circle = new CircleMeridian( this ) ;
		peer.copyValues( circle ) ;

		circle.register() ;
		emitPS( ps, circle ) ;
		circle.degister() ;

		if ( circle.getName() != null )
			Registry.register( circle.getName(), circle ) ;

		if ( circle.getName() != null )
			circleFOV( circle.getCircleGeometry() ) ;
	}

	private void circle( ApplicationPostscriptStream ps, astrolabe.model.CircleParallel peer ) {
		CircleParallel circle ;

		circle = new CircleParallel( this ) ;
		peer.copyValues( circle ) ;

		circle.register() ;
		emitPS( ps, circle ) ;
		circle.degister() ;

		if ( circle.getName() != null )
			Registry.register( circle.getName(), circle ) ;

		if ( circle.getName() != null )
			circleFOV( circle.getCircleGeometry() ) ;
	}

	private void circle( ApplicationPostscriptStream ps, astrolabe.model.CircleNorthernPolar peer ) {
		CircleNorthernPolar circle ;

		circle = new CircleNorthernPolar( this ) ;
		peer.copyValues( circle ) ;

		circle.register() ;
		emitPS( ps, circle ) ;
		circle.degister() ;

		if ( circle.getName() != null )
			Registry.register( circle.getName(), circle ) ;

		if ( circle.getName() != null )
			circleFOV( circle.getCircleGeometry() ) ;
	}

	private void circle( ApplicationPostscriptStream ps, astrolabe.model.CircleNorthernTropic peer ) {
		CircleNorthernTropic circle ;

		circle = new CircleNorthernTropic( this ) ;
		peer.copyValues( circle ) ;

		circle.register() ;
		emitPS( ps, circle ) ;
		circle.degister() ;

		if ( circle.getName() != null )
			Registry.register( circle.getName(), circle ) ;

		if ( circle.getName() != null )
			circleFOV( circle.getCircleGeometry() ) ;
	}

	private void circle( ApplicationPostscriptStream ps, astrolabe.model.CircleSouthernTropic peer ) {
		CircleSouthernTropic circle ;

		circle = new CircleSouthernTropic( this ) ;
		peer.copyValues( circle ) ;

		circle.register() ;
		emitPS( ps, circle ) ;
		circle.degister() ;

		if ( circle.getName() != null )
			Registry.register( circle.getName(), circle ) ;

		if ( circle.getName() != null )
			circleFOV( circle.getCircleGeometry() ) ;
	}

	private void circle( ApplicationPostscriptStream ps, astrolabe.model.CircleSouthernPolar peer ) {
		CircleSouthernPolar circle ;

		circle = new CircleSouthernPolar( this ) ;
		peer.copyValues( circle ) ;

		circle.register() ;
		emitPS( ps, circle ) ;
		circle.degister() ;

		if ( circle.getName() != null )
			Registry.register( circle.getName(), circle ) ;

		if ( circle.getName() != null )
			circleFOV( circle.getCircleGeometry() ) ;
	}

	private void circleFOV( LineString line ) {
		LinearRing ring ;
		Polygon poly ;

		if ( line.isRing() ) {
			ring = new GeometryFactory().createLinearRing( line.getCoordinates() ) ;
			poly = new GeometryFactory().createPolygon( ring, null ) ;

			Registry.register( FOV.RK_FOV, poly ) ;
		}
	}

	private void body( ApplicationPostscriptStream ps, astrolabe.model.BodyStellar peer ) {
		BodyStellar body ;

		body = new BodyStellar( this ) ;
		peer.copyValues( body ) ;

		body.register() ;
		emitPS( ps, body ) ;
		body.degister() ;
	}

	private void body( ApplicationPostscriptStream ps, astrolabe.model.BodyAreal peer ) {
		BodyAreal body ;

		body = new BodyAreal( this ) ;
		peer.copyValues( body ) ;

		body.register() ;
		emitPS( ps, body ) ;
		body.degister() ;
	}

	private void body( ApplicationPostscriptStream ps, astrolabe.model.BodySun peer ) {
		BodySun body ;

		body = new BodySun( this ) ;
		peer.copyValues( body ) ;

		emitPS( ps, body ) ;
	}

	private void body( ApplicationPostscriptStream ps, astrolabe.model.BodyMoon peer ) {
		BodyMoon body ;

		body = new BodyMoon( this ) ;
		peer.copyValues( body ) ;

		emitPS( ps, body ) ;
	}

	private void body( ApplicationPostscriptStream ps, astrolabe.model.BodyPlanet peer ) {
		BodyPlanet body ;

		body = new BodyPlanet( this ) ;
		peer.copyValues( body ) ;

		emitPS( ps, body ) ;
	}

	private void body( ApplicationPostscriptStream ps, astrolabe.model.BodyElliptical peer ) {
		BodyElliptical body ;

		body = new BodyElliptical( this ) ;
		peer.copyValues( body ) ;

		emitPS( ps, body ) ;
	}

	private void body( ApplicationPostscriptStream ps, astrolabe.model.BodyParabolical peer ) {
		BodyParabolical body ;

		body = new BodyParabolical( this ) ;
		peer.copyValues( body ) ;

		emitPS( ps, body ) ;
	}

	private void emitPS( ApplicationPostscriptStream ps, PostscriptEmitter emitter ) {
		ps.operator.gsave() ;

		emitter.headPS( ps ) ;
		emitter.emitPS( ps ) ;
		emitter.tailPS( ps ) ;

		ps.operator.grestore() ;
	}
}
