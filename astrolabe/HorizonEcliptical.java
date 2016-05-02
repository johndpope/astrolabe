
package astrolabe;

import caa.CAA2DCoordinate;
import caa.CAACoordinateTransformation;
import caa.CAANutation;

@SuppressWarnings("serial")
public class HorizonEcliptical extends astrolabe.model.HorizonEcliptical implements PostscriptEmitter, Projector {

	private Projector projector ;

	public HorizonEcliptical( Projector projector ) {
		this.projector = projector ;
	}

	public void register() {
		CAA2DCoordinate c ;
		double epoch, e ;
		double eq[] = new double[2] ;
		MessageCatalog m ;
		String key ;

		epoch = ( (Double) Registry.retrieve( ApplicationConstant.GC_EPOCH ) ).doubleValue() ;

		e = CAANutation.MeanObliquityOfEcliptic( epoch ) ;
		c = CAACoordinateTransformation.Ecliptic2Equatorial( 0, 90, e ) ;
		eq[0] = CAACoordinateTransformation.HoursToDegrees( c.X() ) ;
		eq[1] = c.Y() ;

		m = new MessageCatalog( ApplicationConstant.GC_APPLICATION ) ;

		key = m.message( ApplicationConstant.LK_HORIZON_ECLIPTICEPSILON ) ;
		AstrolabeRegistry.registerDMS( key, e ) ;
		key = m.message( ApplicationConstant.LK_HORIZON_LATITUDE ) ;
		AstrolabeRegistry.registerDMS( key, eq[1] ) ;		
	}

	public void headPS( AstrolabePostscriptStream ps ) {
		GSPaintColor practicality ;

		practicality = new GSPaintColor( getPracticality(), getName() ) ;
		practicality.headPS( ps ) ;
		practicality.emitPS( ps ) ;
		practicality.tailPS( ps ) ;
	}

	public void emitPS( AstrolabePostscriptStream ps ) {
		for ( int an=0 ; an<getAnnotationStraightCount() ; an++ ) {
			AnnotationStraight annotation ;

			annotation = new AnnotationStraight() ;
			getAnnotationStraight( an ).setupCompanion( annotation ) ;
			annotation.register() ;

			ps.operator.gsave() ;

			annotation.headPS( ps ) ;
			annotation.emitPS( ps ) ;
			annotation.tailPS( ps ) ;

			ps.operator.grestore() ;
		}

		for ( int cl=0 ; cl<getCircleCount() ; cl++ ) {
			PostscriptEmitter circle ;

			circle = AstrolabeFactory.companionOf( getCircle( cl ), this ) ;

			ps.operator.gsave() ;

			circle.headPS( ps ) ;
			circle.emitPS( ps ) ;
			circle.tailPS( ps ) ;

			ps.operator.grestore() ;
		}

		for ( int bd=0 ; bd<getBodyCount() ; bd++ ) {
			PostscriptEmitter body ;

			body = AstrolabeFactory.companionOf( getBody( bd ), this ) ;

			ps.operator.gsave() ;

			body.headPS( ps ) ;
			body.emitPS( ps ) ;
			body.tailPS( ps ) ;

			ps.operator.grestore() ;
		}
	}

	public void tailPS( AstrolabePostscriptStream ps ) {
	}

	public double[] project( double[] ho ) {
		return project( ho[0], ho[1] ) ;
	}

	public double[] project( double l, double b ) {
		return projector.project( convert( l, b ) ) ;
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

	public double[] convert( double l, double b ) {
		CAA2DCoordinate c ;
		double[] r = new double[2] ;
		double epoch, e ;

		epoch = ( (Double) Registry.retrieve( ApplicationConstant.GC_EPOCH ) ).doubleValue() ;

		e = CAANutation.MeanObliquityOfEcliptic( epoch ) ;
		c = CAACoordinateTransformation.Ecliptic2Equatorial( l, b, e ) ;
		r[0] = CAACoordinateTransformation.HoursToDegrees( c.X() ) ;
		r[1] = c.Y() ;

		return r ;
	}

	public double[] unconvert( double[] eq ) {
		return unconvert( eq[0], eq[1] ) ;
	}

	public double[] unconvert( double RA, double d ) {
		CAA2DCoordinate c ;
		double[] r = new double[2] ;
		double epoch, e ;

		epoch = ( (Double) Registry.retrieve( ApplicationConstant.GC_EPOCH ) ).doubleValue() ;

		e = CAANutation.MeanObliquityOfEcliptic( epoch ) ;
		c = CAACoordinateTransformation.Equatorial2Ecliptic( CAACoordinateTransformation.DegreesToHours( RA ), d, e ) ;
		r[0] = c.X() ;
		r[1] = c.Y() ;

		return r ;
	}
}
