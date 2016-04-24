
package astrolabe;

import caa.CAACoordinateTransformation;

@SuppressWarnings("serial")
public class CircleMeridian extends astrolabe.model.CircleMeridian implements Circle {

	private final static double DEFAULT_SEGMENT = 1 ;
	private final static double DEFAULT_IMPORTANCE = .1 ;

	private double epoch ;
	private Projector projector ;

	private double segment ;
	private double linewidth ;

	private double az ;

	private double begin ;
	private double end ;

	public CircleMeridian() {
	}

	public CircleMeridian( Object peer, double epoch, Projector projector ) throws ParameterNotValidException {
		setup( peer, epoch, projector ) ;
	}

	public void setup( Object peer, double epoch, Projector projector ) throws ParameterNotValidException {
		String key ;

		ApplicationHelper.setupCompanionFromPeer( this, peer ) ;

		this.epoch = epoch ;
		this.projector = projector ;

		segment = CAACoordinateTransformation.DegreesToRadians(
				ApplicationHelper.getClassNode( this,
						getName(), null ).getDouble( ApplicationConstant.PK_CIRCLE_SEGMENT, DEFAULT_SEGMENT ) ) ;
		linewidth = ApplicationHelper.getClassNode( this,
				getName(), ApplicationConstant.PN_CIRCLE_IMPORTANCE ).getDouble( getImportance(), DEFAULT_IMPORTANCE ) ;

		fov() ;

		try {
			az = AstrolabeFactory.valueOf( getAngle() ) ;
			key = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_CIRCLE_AZIMUTH ) ;
			ApplicationHelper.registerDMS( key, az, 2 ) ;
		} catch ( ParameterNotValidException e ) {}
		try {
			begin = AstrolabeFactory.valueOf( getBegin().getImmediate() ) ;
		} catch ( ParameterNotValidException e ) {
			Circle circle ;
			boolean leading ;

			circle = (Circle) Registry.retrieve( getBegin().getReference().getCircle() ) ;

			leading = getBegin().getReference().getNode().equals( ApplicationConstant.AV_CIRCLE_LEADING ) ;

			begin = circle.isParallel()?
					intersect( (CircleParallel) circle, leading ):
						intersect( (CircleMeridian) circle, leading ) ;
		}
		try {
			end = AstrolabeFactory.valueOf( getEnd().getImmediate() ) ;
		} catch ( ParameterNotValidException e ) {
			Circle circle ;
			boolean leading ;

			circle = (Circle) Registry.retrieve( getEnd().getReference().getCircle() ) ;

			leading = getBegin().getReference().getNode().equals( ApplicationConstant.AV_CIRCLE_LEADING ) ;

			end = circle.isParallel()?
					intersect( (CircleParallel) circle, leading ):
						intersect( (CircleMeridian) circle, leading ) ;
		}
	}

	public double[] project( double al ) {
		return project( al, 0 ) ;
	}

	public double[] project( double al, double shift ) {
		double[] xy ;
		Vector v, t ;

		xy = projector.project( az, al ) ;
		v = new Vector( xy[0], xy[1] ) ;

		if ( shift != 0 ) {
			xy = tangent( al ) ;
			t = new Vector( xy[0], xy[1] ) ;
			t.apply( new double[] { 0, -1, 0, 1, 0, 0, 0, 0, 1 } ) ; // rotate 90 degrees counter clockwise
			t.scale( shift ) ;
			v.add( t ) ;
		}

		return new double[] { v.x, v.y } ;
	}

	public double[] convert( double angle ) {
		return projector.convert( az, angle ) ;
	}

	public double unconvert( double[] eq ) {
		return projector.unconvert( eq )[1] ;
	}

	public double[] tangent( double al ) {
		Vector a, b ;
		double d, xy[] ;

		d = CAACoordinateTransformation.DegreesToRadians( 10./3600 ) ;

		xy = projector.project( az, al+d ) ;
		a = new Vector( xy[0], xy[1] ) ;
		xy = projector.project( az, al ) ;
		b = new Vector( xy[0], xy[1] ) ;

		a.sub( b ) ;

		return new double[] { a.x, a.y } ;
	}

	public java.util.Vector<double[]> list() {
		return list( begin, end, 0 ) ;
	}

	public java.util.Vector<double[]> list( double shift ) {
		return list( begin, end, shift ) ;
	}

	public java.util.Vector<double[]> list( double begin, double end, double shift ) {
		java.util.Vector<double[]> r = new java.util.Vector<double[]>() ;
		double g ;

		r.add( project( begin, shift ) ) ;

		g = mapIndexToRange( begin, end ) ;
		for ( double al=begin+g ; al<end ; al=al+segment ) {
			r.add( project( al, shift ) ) ;
		}

		r.add( project( end, shift ) ) ;

		return r ;
	}

	public void headPS( PostscriptStream ps ) {
		ps.operator.setlinewidth( linewidth ) ;
	}

	public void emitPS( PostscriptStream ps ) {
		java.util.Vector<double[]> v ;
		double[] xy ;

		v = list() ;
		ps.operator.mark() ;
		for ( int n=v.size() ; n>0 ; n-- ) {
			xy = (double[]) v.get( n-1 ) ;
			ps.push( xy[0] ) ;
			ps.push( xy[1] ) ;
		}
		try {
			ps.custom( ApplicationConstant.PS_PROLOG_POLYLINE ) ;
		} catch ( ParameterNotValidException e ) {} // polyline is considered well-defined
		ps.operator.stroke() ;

		// Dial processing.
		try {
			Dial dial ;

			ps.operator.gsave() ;

			dial = AstrolabeFactory.companionOf( getDial(), epoch, this ) ;
			dial.headPS( ps ) ;
			dial.emitPS( ps ) ;
			dial.tailPS( ps ) ;

			ps.operator.grestore() ;
		} catch ( ParameterNotValidException e ) {} // optional

		try {
			ApplicationHelper.emitPS( ps, getAnnotation() ) ;
		} catch ( ParameterNotValidException e ) {} // optional
	}

	public void tailPS( PostscriptStream ps ) {
	}

	private double[] transform() {
		double[] r = new double[3] ;
		double blB, vlA, vlD, vlAl, vlDe ;
		double gneq[], gnho[], gnaz ;
		double rdhoC[], rdST, rdLa ;

		rdhoC = zenit() ;
		rdST = rdhoC[0] ;
		rdLa = rdhoC[1] ;

		blB = Math.rad90-rdLa;//horizon.getLa() ;
		if ( blB==0 ) {	// prevent infinity from tan
			blB = Math.lim0 ;
		}

		// convert actual gn into local
		gneq = projector.convert( this.az, 0 ) ;
		gnho = ApplicationHelper.equatorial2Horizontal( rdST/*horizon.getST()*/-gneq[0], gneq[1], rdLa/*horizon.getLa()*/ ) ;
		gnaz = gnho[0] ;
		if ( gnaz==0 ) {	// prevent infinity from tan
			gnaz = Math.lim0 ;
		}


		if ( java.lang.Math.tan( gnaz )>0 ) {	// QI, QIII
			vlAl = ApplicationHelper.mapTo0To90Range( gnaz ) ;

			// Rule of Napier : cos(90-a) = sin(al)*sin(c)
			vlA = java.lang.Math.acos( java.lang.Math.sin( vlAl )*java.lang.Math.sin( blB ) ) ;
			// Rule of Napier : cos(be) = cot(90-a)*cot(c)
			vlDe = java.lang.Math.acos( Math.truncate( Math.cot( vlA )*Math.cot( blB ) ) ) ;

			r[1] = rdST-Math.rad180+vlDe;//horizon.getST()-Math.rad180+vlDe ;
		} else {							// QII, QIV
			vlAl = Math.rad90-ApplicationHelper.mapTo0To90Range( gnaz ) ;

			vlA = java.lang.Math.acos( java.lang.Math.sin( vlAl )*java.lang.Math.sin( blB ) ) ;
			vlDe = java.lang.Math.acos( Math.truncate( Math.cot( vlA )*Math.cot( blB ) ) ) ;

			r[1] = rdST+Math.rad180-vlDe;//horizon.getST()+Math.rad180-vlDe ;
		}

		r[0] = Math.rad90-vlA ;

		// Rule of Napier : cos(c) = sin(90-a)*sin(90-b)
		// sin(90-b) = cos(c):sin(90-a)
		vlD = Math.rad90-java.lang.Math.asin( Math.truncate( java.lang.Math.cos( blB )/java.lang.Math.sin( vlA ) ) ) ;

		r[2] = vlD ;

		return r ;
	}

	public double transformParallelLa() {
		return transform()[0] ;
	}

	public double transformParallelST() {
		return transform()[1] ;
	}

	public double transformMeridianAl( double vlaz ) {
		double gneq[], gnho[], gnaz, gnal, vlD ;
		double rdhoC[], rdST, rdLa ;

		rdhoC = zenit() ;
		rdST = rdhoC[0] ;
		rdLa = rdhoC[1] ;

		// convert actual gn into local
		gneq = projector.convert( this.az, 0 ) ;
		gnho = ApplicationHelper.equatorial2Horizontal( rdST/*horizon.getST()*/-gneq[0], gneq[1], rdLa/*horizon.getLa()*/ ) ;
		gnaz = gnho[0] ;
		if ( gnaz==0 ) {	// prevent infinity from tan
			gnaz = Math.lim0 ;
		}

		// convert vl az into gn
		vlD = transform()[2] ;
		gnal = java.lang.Math.tan( gnaz )>0?vlaz-vlD:vlaz+vlD ;
		// convert gn az into al
		gnal = gnaz>Math.rad180?gnal-Math.rad90:-( gnal-Math.rad270 ) ;
		// map range from -180 to 180
		gnal = gnal>Math.rad180?gnal-Math.rad360:gnal ;

		return gnal ;
	}

	public double intersect( CircleParallel gn, boolean leading ) throws ParameterNotValidException {
		double blA, blB, blGa, gnB ;
		double[] gneqA, gneqO, gnhoA, gnhoO ;
		double rdhoC[], rdST, gnhoC[], gnST, gnLa ;
		double inaz[], gnaz, gnal ;

		rdhoC = zenit() ;
		rdST = rdhoC[0] ;
		gnhoC = gn.zenit() ;
		gnST = gnhoC[0] ;
		gnLa = gnhoC[1] ;

		gnal = AstrolabeFactory.valueOf( gn.getAngle() ) ;

		gnB = Math.rad90-gnal ;
		blA = Math.rad90-transformParallelLa() ;
		blB = Math.rad90-gnLa ;//gn.dotDot().getLa() ;
		blGa = gnST/*gn.dotDot().getST()*/-transformParallelST() ;

		inaz = CircleParallel.intersection( Math.rad90, gnB, blA, blB, blGa ) ;

		// convert rd az into al
		inaz[0] = transformMeridianAl( inaz[0] ) ;
		inaz[1] = transformMeridianAl( inaz[1] ) ;

		// unconvert local gn into actual
		gneqA = ApplicationHelper.horizontal2Equatorial( inaz[2], gnal, gnLa/*gn.dotDot().getLa()*/ ) ;
		gneqA[0] = rdST-gneqA[0];//horizon.getST()-gneqA[0] ;
		gnhoA = projector.unconvert( gneqA ) ;

		gneqO = ApplicationHelper.horizontal2Equatorial( inaz[3], gnal, gnLa/*gn.dotDot().getLa()*/ ) ;
		gneqO[0] = rdST-gneqO[0];//horizon.getST()-gneqO[0] ;
		gnhoO = projector.unconvert( gneqO ) ;

		// sort associated values
		if ( inaz[0]>inaz[1] ) {
			double v ;

			v = inaz[0] ;
			inaz[0] = inaz[1] ;
			inaz[1] = v ;

			v = gnhoA[0] ;
			gnhoA[0] = gnhoO[0] ;
			gnhoO[0] = v ;
		}

		gnaz = leading?gnhoO[0]:gnhoA[0] ;

		if ( ! gn.probe( ApplicationHelper.mapToNTo360Range( gnaz ) ) ) {
			throw new ParameterNotValidException() ;
		}

		return leading?inaz[1]:inaz[0] ;
	}

	public double intersect( CircleMeridian gn, boolean leading ) throws ParameterNotValidException {
		double blA, blB, blGa ;
		double inaz[], gnal ;

		blA = Math.rad90-transformParallelLa() ;
		blB = Math.rad90-gn.transformParallelLa() ;
		blGa = gn.transformParallelST()-transformParallelST() ;

		inaz = CircleParallel.intersection( Math.rad90, Math.rad90, blA, blB, blGa ) ;

		// convert az into al
		inaz[0] = transformMeridianAl( inaz[0] ) ;
		inaz[1] = transformMeridianAl( inaz[1] ) ;
		inaz[2] = gn.transformMeridianAl( inaz[2] ) ;
		inaz[3] = gn.transformMeridianAl( inaz[3] ) ;

		// sort associated values
		if ( inaz[0]>inaz[1] ) {
			double v ;

			v = inaz[0] ;
			inaz[0] = inaz[1] ;
			inaz[1] = v ;

			v = inaz[2] ;
			inaz[2] = inaz[3] ;
			inaz[3] = v ;
		}

		gnal = leading?inaz[3]:inaz[2] ;

		if ( ! gn.probe( ApplicationHelper.mapToNTo360Range( gnal ) ) ) {
			throw new ParameterNotValidException() ;
		}

		return leading?inaz[1]:inaz[0] ;
	}

	public boolean probe( double al ) {
		return CircleParallel.probe( al, begin, end ) ;
	}

	public double mapIndexToScale( int index ) {
		return CircleParallel.mapIndexToAngleOfScale( index, segment, begin, end ) ;
	}

	public double mapIndexToScale( double span ) {
		return CircleParallel.mapIndexToAngleOfScale( 0, span, begin, end ) ;
	}

	public double mapIndexToScale( int index, double span ) {
		return CircleParallel.mapIndexToAngleOfScale( index, span, begin, end ) ;
	}

	public double[] zenit() {
		return projector.convert( 0, Math.rad90 ) ;
	}

	public boolean isParallel() {
		return false ;
	}

	public boolean isMeridian() {
		return true ;
	}

	public double mapIndexToRange() {
		return CircleParallel.gap( 0, segment, begin , end ) ;
	}

	public double mapIndexToRange( double begin, double end ) {
		return CircleParallel.gap( 0, segment, begin , end ) ;
	}

	public double mapIndexToRange( int index, double begin, double end ) {
		return CircleParallel.gap( index, segment, begin , end ) ;
	}

	@SuppressWarnings("unchecked")
	public void fov() throws ParameterNotValidException {
		java.util.Vector<String> fov ;
		String[] fv ;

		if ( getName() != null ) {
			Registry.register( getName(), this ) ;

			if ( getFov() != null ) {
				fv = getFov().split( "," ) ;
				for ( int f=0 ; f<fv.length ; f++ ) {
					try {
						fov = (java.util.Vector<String>) Registry.retrieve( getFov() ) ;
					} catch ( ParameterNotValidException e ) {
						fov = new java.util.Vector<String>() ;
						Registry.register( getFov(), fov ) ;
					}
					fov.add( getName() ) ;
				}
			}
		}
	}
}