
package astrolabe;

import java.util.Date;
import java.util.prefs.Preferences;

import caa.CAACoordinateTransformation;

@SuppressWarnings("serial")
abstract public class ChartAzimuthalType extends astrolabe.model.ChartAzimuthalType {

	private final static double DEFAULT_UNIT = 2.834646 ;
	private final static double DEFAULT_HALO = 4 ;
	private final static double DEFAULT_HALOMIN = .08 ;
	private final static double DEFAULT_HALOMAX = .4 ;

	private double unit ;
	private double scale ;

	private double[] origin = new double[2] ;

	private double halo ;
	private double halomin ;
	private double halomax ;

	private ChartPage page ;

	public ChartAzimuthalType( Peer peer ) {
		double[] origin ;
		Preferences node ;

		peer.setupCompanion( this ) ;

		page = new ChartPage( getChartPage() ) ;

		origin = AstrolabeFactory.valueOf( getOrigin() ) ;
		this.origin[0] = origin[1] ;
		this.origin[1] = origin[2] ;

		node = Configuration.getClassNode( this, getName(), null ) ;

		unit = Configuration.getValue( node, ApplicationConstant.PK_CHART_UNIT, DEFAULT_UNIT ) ;
		scale = java.lang.Math.min( page.sizex(), page.sizey() )/2/Math.goldensection*page.getReal()/100*getScale()/100 ;

		halo = Configuration.getValue( node, ApplicationConstant.PK_CHART_HALO, DEFAULT_HALO ) ;
		halomin = Configuration.getValue( node, ApplicationConstant.PK_CHART_HALOMIN, DEFAULT_HALOMIN ) ;
		halomax = Configuration.getValue( node, ApplicationConstant.PK_CHART_HALOMAX, DEFAULT_HALOMAX ) ;
		Registry.register( ApplicationConstant.PK_CHART_HALO, new Double( halo ) ) ;
		Registry.register( ApplicationConstant.PK_CHART_HALOMIN, new Double( halomin ) ) ;
		Registry.register( ApplicationConstant.PK_CHART_HALOMAX, new Double( halomax ) ) ;

		if ( getAtlasPage() != null ) {
			( (AtlasPage) getAtlasPage() ).register() ;
		}
	}

	public double[] project( double RA, double d ) {
		return project( new double[] { RA, d } ) ;
	}

	public double[] project( double[] eq ) {
		double[] r ;
		Vector vp, vo, vZ, vY ;
		CoordinateSystem cs ;

		vp = new Vector( polarToWorld( hemisphereToPolar( eq ) ) ) ;

		vo = new Vector( polarToWorld( hemisphereToPolar( origin ) ) ) ;
		if ( vo.abs()>0 ) {
			vZ = new Vector( 0, 0, 1 ) ;
			vY = new Vector( vo ).mul( -1 ) ;
			cs = new CoordinateSystem( vo, vZ, vY ) ;
			vp.set( cs.local( vp.toArray() ) ) ;
		}

		vp.mul( scale ) ;

		r = new double[] { vp.x, vp.y } ;

		return r ;
	}

	public double[] unproject( double x, double y ) {
		return unproject( new double[] { x, y } ) ;
	}

	public double[] unproject( double[] xy ) {
		double[] r ;
		Vector vp, vo, vZ, vY ;
		CoordinateSystem cs ;

		vp = new Vector( xy ) ;
		vp.mul( 1/scale ) ;

		vo = new Vector( polarToWorld( hemisphereToPolar( origin ) ) ) ;
		if ( vo.abs()>0 ) {
			vZ = new Vector( 0, 0, 1 ) ;
			vY = new Vector( vo ).mul( -1 ) ;
			cs = new CoordinateSystem( vo, vZ, vY ) ;
			vp.set( cs.world( vp.toArray() ) ) ;
		}

		r = polarToHemisphere( worldToPolar( new double[] { vp.x, vp.y } ) ) ;

		return r ;
	}

	public double[] convert( double[] eq ) {
		return eq ;
	}

	public double[] convert( double RA, double d ) {
		return new double[] { RA, d } ;
	}

	public double[] unconvert( double[] eq ) {
		return eq ;
	}

	public double[] unconvert( double RA, double d ) {
		return new double[] { RA, d } ;
	}

	public void headPS( AstrolabePostscriptStream ps ) {
		ElementPracticality practicality ;
		ElementImportance importance ;
		long seed ;

		ps.dsc.beginSetup() ;

		// Set pagesize
		ps.dict( true ) ;
		ps.push( "/PageSize" ) ;
		ps.array( true ) ;
		ps.push( page.sizex()*unit ) ;
		ps.push( page.sizey()*unit ) ;
		ps.array( false ) ;
		ps.dict( false ) ;

		ps.operator.setpagedevice() ;

		// Set origin at center of page.
		ps.custom( ApplicationConstant.PS_CUSTOM_PAGESIZE ) ;

		ps.operator.mul( .5 ) ;
		ps.operator.exch() ;
		ps.operator.mul( .5 ) ;
		ps.operator.exch() ;
		ps.operator.translate() ;

		seed = new Date().getTime()/1000 ;
		ps.operator.srand( seed ) ;

		ps.dsc.endSetup() ;

		ps.dsc.beginPageSetup() ;

		ps.operator.scale( unit ) ;

		practicality = new ElementPracticality( getPracticality() ) ;
		practicality.headPS( ps ) ;
		practicality.emitPS( ps ) ;
		practicality.tailPS( ps ) ;

		importance = new ElementImportance( getImportance() ) ;
		importance.headPS( ps ) ;
		importance.emitPS( ps ) ;
		importance.tailPS( ps ) ;

		ps.dsc.endPageSetup() ;

		ps.dsc.page( getName(), 1 ) ;
	}

	public void emitPS( AstrolabePostscriptStream ps ) {
		if ( page.sizex()>page.realx() ) {
			ps.operator.mark() ;

			ps.push( -page.realx()/2 ) ;
			ps.push( -page.realy()/2 ) ;
			ps.push( -page.realx()/2 ) ;
			ps.push( page.realy()/2 ) ;
			ps.push( page.realx()/2 ) ;
			ps.push( page.realy()/2 ) ;
			ps.push( page.realx()/2 ) ;
			ps.push( -page.realy()/2 ) ;

			ps.custom( ApplicationConstant.PS_CUSTOM_POLYLINE ) ;

			ps.operator.closepath() ;
			ps.operator.stroke() ;
		}
	}

	public void tailPS( AstrolabePostscriptStream ps ) {
		ps.operator.showpage() ;
		ps.dsc.pageTrailer() ;
	}

	public double[] hemisphereToPolar( double[] eq ) {
		return getNorthern()?flipNorthern( eq ):flipSouthern( eq ) ;
	}

	public double[] polarToHemisphere( double[] p ) {
		return getNorthern()?flipNorthern( p ):flipSouthern( p ) ;
	}

	private double[] flipNorthern( double[] eq ) {
		return new double[] { CAACoordinateTransformation.MapTo0To360Range( -eq[0] ), eq[1] } ;
	}

	private double[] flipSouthern( double[] eq ) {
		return new double[] { CAACoordinateTransformation.MapTo0To360Range( 180+eq[0] ), -eq[1] } ;
	}

	public double[] polarToWorld( double[] eq ) {
		double d ;

		d = thetaToDistance( eq[1] ) ;

		return new double[] {
				d*Math.cos( eq[0] ),
				d*Math.sin( eq[0] ) } ;
	}

	public double[] worldToPolar( double[] xy ) {
		return new double[] {
				CAACoordinateTransformation.MapTo0To360Range( Math.atan2( xy[1], xy[0] ) ),
				distanceToTheta( java.lang.Math.sqrt( xy[0]*xy[0]+xy[1]*xy[1] ) ) } ;
	}

	abstract public double thetaToDistance( double de ) ;
	abstract public double distanceToTheta( double d ) ;
}
