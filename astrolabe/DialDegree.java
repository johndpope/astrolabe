
package astrolabe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import caa.CAACoordinateTransformation;

@SuppressWarnings("serial")
public class DialDegree extends astrolabe.model.DialDegree implements Dial {

	@SuppressWarnings("unused")
	private final static double DEFAULT_NONE_SPACE = .1 ;
	@SuppressWarnings("unused")
	private final static double DEFAULT_NONE_THICKNESS = 0 ;
	@SuppressWarnings("unused")
	private final static double DEFAULT_NONE_LINEWIDTH = 0 ;

	@SuppressWarnings("unused")
	private final static double DEFAULT_LINE_SPACE = 1 ;
	@SuppressWarnings("unused")
	private final static double DEFAULT_LINE_THICKNESS = .2 ;
	@SuppressWarnings("unused")
	private final static double DEFAULT_LINE_LINEWIDTH = 0 ;

	@SuppressWarnings("unused")
	private final static double DEFAULT_RAIL_SPACE = 1 ;
	@SuppressWarnings("unused")
	private final static double DEFAULT_RAIL_THICKNESS = 1.2 ;
	@SuppressWarnings("unused")
	private final static double DEFAULT_RAIL_LINEWIDTH = .01 ;

	private final static double DEFAULT_RISE = 3.2 ;

	private Baseline baseline ;

	private Method headPSBaseline ;
	private Method emitPSBaseline ;
	private Method tailPSBaseline ;

	private double rise ;

	private double space ;
	private double thickness ;
	private double linewidth ;

	private double unit ;

	public DialDegree( Object peer, Baseline baseline ) {
		this( peer, baseline, CAACoordinateTransformation.DegreesToRadians( 1 ) ) ;
	}

	public DialDegree( Object peer, Baseline baseline, double unit ) {
		ApplicationHelper.setupCompanionFromPeer( this, peer ) ;

		this.baseline = baseline ;	
		this.unit = unit ;

		try {
			Class c ;
			String bl, blm, blf, node ;
			double ds, dt, dl ;

			c = Class.forName( "astrolabe.DialDegree" ) ;

			bl = getBaseline() ;
			blm = bl.replaceFirst( ".", bl.substring( 0, 1 ).toUpperCase() ) ;
			headPSBaseline = c.getDeclaredMethod( "headPSBaseline"+blm, new Class[] { Class.forName( "astrolabe.PostscriptStream" ) } ) ;
			emitPSBaseline = c.getDeclaredMethod( "emitPSBaseline"+blm, new Class[] { Class.forName( "astrolabe.PostscriptStream" ) } ) ;
			tailPSBaseline = c.getDeclaredMethod( "tailPSBaseline"+blm, new Class[] { Class.forName( "astrolabe.PostscriptStream" ) } ) ;

			blf = blm.toUpperCase() ;
			ds = c.getDeclaredField( "DEFAULT_"+blf+"_SPACE" ).getDouble( this ) ;
			dt = c.getDeclaredField( "DEFAULT_"+blf+"_THICKNESS" ).getDouble( this ) ;
			dl = c.getDeclaredField( "DEFAULT_"+blf+"_LINEWIDTH" ).getDouble( this ) ;

			node = ApplicationConstant.PN_DIAL_BASELINE+"/"+blm ;
			space = ApplicationHelper.getClassNode( this, getName(),
					node ).getDouble( ApplicationConstant.PK_DIAL_SPACE, ds ) ;
			thickness = ApplicationHelper.getClassNode( this, getName(),
					node ).getDouble( ApplicationConstant.PK_DIAL_THICKNESS, dt ) ;
			linewidth = ApplicationHelper.getClassNode( this, getName(),
					node ).getDouble( ApplicationConstant.PK_DIAL_LINEWIDTH, dl ) ;
		} catch ( ClassNotFoundException e ) {
		} catch ( NoSuchMethodException e ) {
		} catch ( NoSuchFieldException e ) {
		} catch ( IllegalAccessException e ) {
		}

		rise = ApplicationHelper.getClassNode( this, getName(),
				ApplicationConstant.PN_DIAL_ANNOTATION ).getDouble( ApplicationConstant.PK_DIAL_RISE, DEFAULT_RISE ) ;
	}

	public void headPS( PostscriptStream ps ) {
		try {
			headPSBaseline.invoke( this, new Object[] { ps } ) ;
		} catch ( IllegalAccessException e ) {
		} catch ( InvocationTargetException e ) {
		}
	}

	public void emitPS( PostscriptStream ps ) {
		java.util.Vector<double[]> v ;
		double[] xy ;

		try {
			emitPSBaseline.invoke( this, new Object[] { ps } ) ;
		} catch ( IllegalAccessException e ) {
		} catch ( InvocationTargetException e ) {
		}

		emitPSGraduation( ps ) ;

		v = baseline.list( getReflect()?-( ( space+thickness )+rise ):( space+thickness )+rise ) ;
		ps.operator.mark() ;
		for ( int n=v.size() ; n>0 ; n-- ) {
			xy = (double[]) v.get( n-1 ) ;
			ps.push( xy[0] ) ;
			ps.push( xy[1] ) ;
		}
		try {
			ps.custom( ApplicationConstant.PS_PROLOG_POLYLINE ) ;
		} catch ( ParameterNotValidException e ) {} // ployline is considered well-defined

		try {
			ApplicationHelper.emitPS( ps, getAnnotation() ) ;
		} catch ( ParameterNotValidException e ) {} // optional
	}

	public void tailPS( PostscriptStream ps ) {
		try {
			tailPSBaseline.invoke( this, new Object[] { ps } ) ;
		} catch ( IllegalAccessException e ) {
		} catch ( InvocationTargetException e ) {
		}
	}

	@SuppressWarnings("unused")
	private void headPSBaselineNone( PostscriptStream ps ) {
	}

	@SuppressWarnings("unused")
	private void headPSBaselineLine( PostscriptStream ps ) {
		ps.operator.setlinewidth( thickness ) ;
	}

	@SuppressWarnings("unused")
	private void headPSBaselineRail( PostscriptStream ps ) {
		ps.operator.setlinewidth( linewidth ) ;
	}

	@SuppressWarnings("unused")
	private void emitPSBaselineNone( PostscriptStream ps ) {
	}

	@SuppressWarnings("unused")
	private void emitPSBaselineLine( PostscriptStream ps ) throws ParameterNotValidException {
		java.util.Vector<double[]> v ;
		double b, e ;
		int ns ;

		v = new java.util.Vector<double[]>() ;

		try { // baseline
			for ( ns=0 ; ; ns++ ) {
				b = mapIndexToScale( ns, getGraduationSpan().getSpan() ) ;
				e = mapIndexToScale( ns+1, getGraduationSpan().getSpan() ) ;

				v.addAll( baseline.list( b, e, getReflect()?-space:space ) ) ;
			}
		} catch ( ParameterNotValidException ePNV ) {
			double[] xy ;

			ps.operator.mark() ;
			for ( int n=v.size() ; n>0 ; n-- ) {
				xy = (double[]) v.get( n-1 ) ;
				ps.push( xy[0] ) ;
				ps.push( xy[1] ) ;
			}
			ps.custom( ApplicationConstant.PS_PROLOG_POLYLINE ) ;
			ps.operator.stroke() ;
		}
	}

	@SuppressWarnings("unused")
	private void emitPSBaselineRail( PostscriptStream ps ) throws ParameterNotValidException {
		java.util.Vector<double[]> vDFw = null, vDRv = null ;
		double b, e, s, span ;
		int nss = 0 ;

		span = getGraduationSpan().getSpan()/getGraduationSpan().getDivision() ;

		try { // baseline
			for ( ; ; nss++ ) {
				b = mapIndexToScale( nss, span ) ;
				e = mapIndexToScale( nss+1, span ) ;

				s = nss%2==0?space:space+linewidth/2 ;
				s = getReflect()?-s:s ;			
				vDFw = baseline.list( b, e, s ) ;

				s = space+( nss%2==0?thickness:thickness-linewidth/2 ) ;
				s = getReflect()?-s:s ;
				vDRv = baseline.list( b, e, s ) ;

				vDRv = ApplicationHelper.reverseVector( vDRv ) ;
				vDFw.addAll( vDRv ) ;

				if ( nss%2 == 0 ) { // subunit filled
					double[] xy ;

					ps.operator.mark() ;
					for ( int n=vDFw.size() ; n>0 ; n-- ) {
						xy = (double[]) vDFw.get( n-1 ) ;
						ps.push( xy[0] ) ;
						ps.push( xy[1] ) ;
					}
					ps.custom( ApplicationConstant.PS_PROLOG_POLYLINE ) ;
					ps.operator.closepath() ;
					ps.operator.fill() ;
				} else { // subunit unfilled
					java.util.Vector<double[]> fw, rv ;
					double[] xy ;

					fw = new java.util.Vector<double[]>( vDFw.subList( 0, vDFw.size()/2 ) ) ;
					rv = new java.util.Vector<double[]>( vDFw.subList( vDFw.size()/2, vDFw.size() ) ) ;

					ps.operator.mark() ;
					for ( int n=fw.size() ; n>0 ; n-- ) {
						xy = (double[]) fw.get( n-1 ) ;
						ps.push( xy[0] ) ;
						ps.push( xy[1] ) ;
					}
					ps.custom( ApplicationConstant.PS_PROLOG_POLYLINE ) ;
					ps.operator.stroke() ;
					ps.operator.mark() ;
					for ( int n=rv.size() ; n>0 ; n-- ) {
						xy = (double[]) rv.get( n-1 ) ;
						ps.push( xy[0] ) ;
						ps.push( xy[1] ) ;
					}
					ps.custom( ApplicationConstant.PS_PROLOG_POLYLINE ) ;
					ps.operator.stroke() ;
				}
			}
		} catch ( ParameterNotValidException ePNV ) {
			if ( nss%2 == 0 ) { // close unfilled subunit
				java.util.Vector<double[]> vector ;
				double[] xy ;

				vector = new java.util.Vector<double[]>( vDFw.subList( vDFw.size()/2-1, vDFw.size()/2+1 ) ) ;

				xy = (double[]) vector.get( 1 ) ;
				ps.push( xy[0] ) ;
				ps.push( xy[1] ) ;
				xy = (double[]) vector.get( 0 ) ;
				ps.push( xy[0] ) ;
				ps.push( xy[1] ) ;
				ps.custom( ApplicationConstant.PS_PROLOG_LINE ) ;
				ps.operator.stroke() ;
			}
		}
	}

	@SuppressWarnings("unused")
	private void tailPSBaselineNone( PostscriptStream ps ) {
	}

	@SuppressWarnings("unused")
	private void tailPSBaselineLine( PostscriptStream ps ) {
	}

	@SuppressWarnings("unused")
	private void tailPSBaselineRail( PostscriptStream ps ) {
	}

	private void emitPSGraduation( PostscriptStream ps ) {
		int ns ;
		double s, a ;
		Vector bc, ec, bd ;
		double[] o, t, xy ;
		Graduation g ;

		// prepare circle closed check
		s = getGraduationSpan().getSpan() ;
		xy = baseline.project( baseline.mapIndexToScale( 0 ) ) ;
		bc = new Vector( xy[0], xy[1] ) ;
		xy = baseline.project( baseline.mapIndexToScale( -1 ) ) ;
		ec = new Vector( xy[0], xy[1] ) ;
		ec.sub( bc ) ;

		// prepare dial start aligned with circle begin check
		try {
			xy = baseline.project( mapIndexToScale( 0, s ) ) ;
			bd = new Vector( xy[0], xy[1] ) ;
			bc.sub( bd ) ;
		} catch ( ParameterNotValidException e ) {} // cannot happen with index 0

		if ( Math.isLim0( ec.abs() ) && Math.isLim0( bc.abs() ) ) {
			ns = 1 ;
		} else {
			ns = 0 ;
		}

		for ( ; ; ns++ ) {
			try {
				a = mapIndexToScale( ns, s ) ;
			} catch ( ParameterNotValidException e ) {
				break ;
			}

			register( ns ) ;

			o = baseline.project( a, getReflect()?-( space+thickness ):space+thickness ) ;
			t = baseline.tangent( a ) ;
			if ( getReflect() ) {
				t[0] = -t[0] ;
				t[1] = -t[1] ;
			}

			g = new GraduationSpan( getGraduationSpan(), o, t ) ;
			try { // half
				if ( isIndexAligned( ns, getGraduationHalf().getSpan() ) ) {
					g = new GraduationHalf( getGraduationHalf(), o, t ) ;
				}
			} catch ( NullPointerException e ) {}
			try { // full
				if ( isIndexAligned( ns, getGraduationFull().getSpan() ) ) {
					g = new GraduationFull( getGraduationFull(), o, t ) ;
				}
			} catch ( NullPointerException e ) {}

			ps.operator.gsave() ;

			g.headPS( ps ) ;
			g.emitPS( ps ) ;
			g.tailPS( ps ) ;

			ps.operator.grestore() ;
		}
	}

	public double mapIndexToScale( int index, double span ) throws ParameterNotValidException {
		double r ;

		r = baseline.mapIndexToScale( index, span*unit ) ;
		if ( ! baseline.probe( r ) || r>Math.rad360 ) {
			throw new ParameterNotValidException() ;
		}

		return r ;
	}

	public boolean isIndexAligned( int index, double span ) {
		double a, b ;

		a = baseline.mapIndexToScale( index, getGraduationSpan().getSpan()*unit ) ;
		b = span*unit ;

		return Math.isLim0( a-(int) ( a/b )*b ) ;
	}

	public void register( int index ) {
		double a ;
		String key ;

		try {
			a = baseline.mapIndexToScale( index, getGraduationSpan().getSpan()*unit ) ;

			key = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_DIAL_DEGREE ) ;
			ApplicationHelper.registerDMS( key, a, 2 ) ;
			key = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_DIAL_HOUR ) ;
			ApplicationHelper.registerTime( key, a, 2 ) ;
			key = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_DIAL_AZIMUTHTIME ) ;
			ApplicationHelper.registerTime( key, a+Math.rad180/*12h*/, 2 ) ;
		} catch ( ParameterNotValidException  e ) {}
	}
}