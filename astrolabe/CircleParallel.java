
package astrolabe;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exolab.castor.xml.ValidationException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import caa.CAA2DCoordinate;
import caa.CAACoordinateTransformation;

@SuppressWarnings("serial")
public class CircleParallel extends astrolabe.model.CircleParallel implements PostscriptEmitter, Baseline {

	private final static Log log = LogFactory.getLog( CircleParallel.class ) ;

	private final static double DEFAULT_INTERVAL = 1 ;

	private Projector projector ;

	private double interval ;

	private double al ;

	private double begin ;
	private double end ;

	public CircleParallel() {
	}

	public CircleParallel( Peer peer, Projector projector ) throws ParameterNotValidException {
		setup( peer, projector ) ;
	}

	public void setup( Peer peer, Projector projector ) throws ParameterNotValidException {
		String key ;

		peer.setupCompanion( this ) ;
		try {
			validate() ;
		} catch ( ValidationException e ) {
			throw new ParameterNotValidException( e.toString() ) ;
		}

		this.projector = projector ;

		interval = ApplicationHelper.getPreferencesKV(
				ApplicationHelper.getClassNode( this, getName(), null ),
				ApplicationConstant.PK_CIRCLE_INTERVAL, DEFAULT_INTERVAL ) ;

		al = AstrolabeFactory.valueOf( getAngle() ) ;
		key = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_CIRCLE_ALTITUDE ) ;
		ApplicationHelper.registerDMS( key, al ) ;

		try {
			begin = AstrolabeFactory.valueOf( getBegin().getImmediate() ) ;
		} catch ( ParameterNotValidException e ) {
			Object circle ;
			boolean leading ;

			circle = Registry.retrieve( getBegin().getReference().getCircle() ) ;
			leading = getBegin().getReference().getNode().equals( ApplicationConstant.AV_CIRCLE_LEADING ) ;
			try {
				begin = circle instanceof CircleParallel?
						intersect( (CircleParallel) circle, leading ):
							intersect( (CircleMeridian) circle, leading ) ;
			} catch ( ParameterNotValidException ee ) {
				String msg ;

				msg = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_MESSAGE_PARAMETERNOTAVLID ) ;
				msg = MessageFormat.format( msg, new Object[] { "\""+getBegin().getReference().getCircle()+"\"", ee.toString() } ) ;
				log.warn( msg ) ;

				begin = 0 ;
			}
		} finally {
			begin = CAACoordinateTransformation.MapTo0To360Range( begin ) ;
		}
		try {
			end = AstrolabeFactory.valueOf( getEnd().getImmediate() ) ;
		} catch ( ParameterNotValidException e ) {
			Object circle ;
			boolean leading ;

			circle = Registry.retrieve( getEnd().getReference().getCircle() ) ;
			leading = getEnd().getReference().getNode().equals( ApplicationConstant.AV_CIRCLE_LEADING ) ;
			try {
				end = circle instanceof CircleParallel?
						intersect( (CircleParallel) circle, leading ):
							intersect( (CircleMeridian) circle, leading ) ;
			} catch ( ParameterNotValidException ee ) {
				String msg ;

				msg = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_MESSAGE_PARAMETERNOTAVLID ) ;
				msg = MessageFormat.format( msg, new Object[] { "\""+getBegin().getReference().getCircle()+"\"", ee.toString() } ) ;
				log.warn( msg ) ;

				end = 360 ;
			}
		} finally {
			end = CAACoordinateTransformation.MapTo0To360Range( end ) ;
		}

		if ( getName() != null ) {
			Registry.register( getName(), this ) ;
		}

		if ( getFov() != null ) {
			LineString circle ;

			circle = new GeometryFactory().createLineString(
					new JTSCoordinateArraySequence( list() ) ) ;

			if ( circle.isRing() ) {
				Registry.register( getFov(),
						new GeometryFactory().createPolygon(
								new GeometryFactory().createLinearRing( circle.getCoordinateSequence() ), null ) ) ;
			} else { // extend
			}
		}
	}

	public double[] project( double az ) {
		return project( az, 0 ) ;
	}

	public double[] project( double az, double shift ) {
		double[] xy ;
		Vector v, t ;

		xy = projector.project( az, al ) ;
		v = new Vector( xy[0], xy[1] ) ;

		if ( shift != 0 ) {
			xy = tangent( az ) ;
			t = new Vector( xy[0], xy[1] ) ;
			t.apply( new double[] { 0, -1, 0, 1, 0, 0, 0, 0, 1 } ) ; // rotate 90 degrees counter clockwise
			t.scale( shift ) ;
			v.add( t ) ;
		}

		return new double[] { v.x, v.y } ;
	}

	public double[] convert( double angle ) {
		return projector.convert( angle, al ) ;
	}

	public double unconvert( double[] eq ) {
		return projector.unconvert( eq )[0] ;
	}

	public double[] tangent( double az ) {
		Vector a, b ;
		double xy[] ;

		xy = projector.project( az+10./3600, al ) ;
		a = new Vector( xy[0], xy[1] ) ;
		xy = projector.project( az, al ) ;
		b = new Vector( xy[0], xy[1] ) ;

		a.sub( b ) ;

		return new double[] { a.x, a.y } ;
	}

	public java.util.Vector<double[]> list( java.util.Vector<Double> list ) {
		return list( null, begin, end, 0 ) ;
	}

	public java.util.Vector<double[]> list( java.util.Vector<Double> list, double shift ) {
		return list( null, begin, end, shift ) ;
	}

	public java.util.Vector<double[]> list( java.util.Vector<Double> list, double begin, double end, double shift ) {
		java.util.Vector<double[]> r = new java.util.Vector<double[]>() ;
		double g ;

		r.add( project( begin, shift ) ) ;
		if ( list != null ) {
			list.add( begin ) ;
		}

		g = mapIndexToRange( begin, end ) ;
		for ( double az=begin+g ; begin>end?az-360<end:az<end ; az=az+interval ) {
			r.add( project( az, shift ) ) ;
			if ( list != null ) {
				list.add( az ) ;
			}
		}

		r.add( project( end, shift ) ) ;
		if ( list != null ) {
			list.add( end ) ;
		}

		return r ;
	}

	public java.util.Vector<double[]> list() {
		return list( null, begin, end, 0 ) ;
	}

	public java.util.Vector<double[]> list( double shift ) {
		return list( null, begin, end, shift ) ;
	}

	public java.util.Vector<double[]> list( double begin, double end, double shift ) {
		return list( null, begin, end, shift ) ;
	}

	public void headPS( PostscriptStream ps ) {
		ElementImportance importance ;

		importance = new ElementImportance( getImportance() ) ;
		importance.headPS( ps ) ;
		importance.emitPS( ps ) ;
		importance.tailPS( ps ) ;
	}

	public void emitPS( PostscriptStream ps ) {
		Geometry fov, circle ;

		try {
			fov = (Geometry) Registry.retrieve( ApplicationConstant.GC_FOVUNI ) ;
		} catch ( ParameterNotValidException e ) {
			throw new RuntimeException( e.toString() ) ;
		}
		circle = new GeometryFactory().createLineString(
				new JTSCoordinateArraySequence( list() ) ) ;

		emitPS( ps, !fov.covers( circle ) ) ;
	}

	public void emitPS( PostscriptStream ps, boolean cut ) {
		ListCutter cutter ;
		Geometry fov ;
		astrolabe.model.CircleParallel peer ;
		CircleParallel circle ;
		double[] lob, loe ;
		astrolabe.model.Annotation aC = null ;
		astrolabe.model.AnnotationCurved aCM, aCC ; // model/cut annotation
		astrolabe.model.AnnotationStraight aSM, aSC ; // model/cut annotation
		astrolabe.model.Text tC ;
		List<double[]> l ;
		double[] xy ;
		String ns ;

		if ( cut ) {
			try {
				fov = (Geometry) Registry.retrieve( ApplicationConstant.GC_FOVUNI ) ;
			} catch ( ParameterNotValidException e ) {
				throw new RuntimeException( e.toString() ) ;
			}
			cutter = new ListCutter( list(), fov ) ;
			for ( List<double[]> s : cutter.segmentsIntersecting( true ) ) {
				peer = new astrolabe.model.CircleParallel() ;
				// astrolabe.model.AngleType
				peer.setAngle( new astrolabe.model.Angle() ) ;
				peer.getAngle().setRational( new astrolabe.model.Rational() ) ;

				peer.setBegin( new astrolabe.model.Begin() ) ;
				// astrolabe.model.AngleType
				peer.getBegin().setImmediate( new astrolabe.model.Immediate() ) ;
				peer.getBegin().getImmediate().setRational( new astrolabe.model.Rational() ) ;
				peer.setEnd( new astrolabe.model.End() ) ;
				// astrolabe.model.AngleType
				peer.getEnd().setImmediate( new astrolabe.model.Immediate() ) ;
				peer.getEnd().getImmediate().setRational( new astrolabe.model.Rational() ) ;
				if ( getName() != null ) {
					peer.setName( ApplicationConstant.GC_NS_CUT+getName() ) ;
				}

				lob = projector.unproject( s.get( 0 ) ) ;
				xy = s.get( s.size()-1 ) ;
				loe = projector.unproject( xy ) ; 

				ns = new JTSCoordinate( xy ).quadrant(
						new JTSCoordinate( fov.getEnvelope().getCoordinates()[0] ),
						new JTSCoordinate( fov.getEnvelope().getCoordinates()[2] ) )+":" ;

				try {
					peer.getAngle().getRational().setValue( al ) ;
					peer.getBegin().getImmediate().getRational().setValue( lob[0] ) ;
					peer.getEnd().getImmediate().getRational().setValue( loe[0] ) ;
					AstrolabeFactory.modelOf( peer ) ;
				} catch ( ParameterNotValidException e ) {
					throw new RuntimeException( e.toString() ) ;
				}

				peer.setImportance( getImportance() ) ;

				peer.setDial( getDial() ) ;

				try {
					for ( astrolabe.model.Annotation annotation : getAnnotation() ) {
						aCM = annotation.getAnnotationCurved() ;
						if ( aCM != null && aCM.getName() != null ) {
							aCC = new astrolabe.model.AnnotationCurved() ;
							aCC.setName( ns+aCM.getName() ) ;

							for ( astrolabe.model.Text tM : aCM.getText() ) {
								tC = new astrolabe.model.Text() ;
								tC.setName( ns+tM.getName() ) ;
								tC.setValue( tM.getValue() ) ;
								AstrolabeFactory.modelOf( tC ) ;

								tC.setName( tM.getName() ) ;

								tC.setSubscript( tM.getSubscript() ) ;
								tC.setSuperscript( tM.getSuperscript() ) ;

								aCC.addText( tC ) ;
							}

							AstrolabeFactory.modelOf( aCC ) ;

							aCC.setName( aCM.getName() ) ;
							// setPurpose already done by AstrolabeFactory.modelOf()
							// aCC.setPurpose( aCM.getPurpose() ) ;
							// setAnchor already done by AstrolabeFactory.modelOf()
							// aCC.setAnchor( aCM.getAnchor() ) ;
							aCC.setReverse( aCM.getReverse() ) ;
							aCC.setDistance( aCM.getDistance() ) ;

							aC = new astrolabe.model.Annotation() ;
							aC.setAnnotationCurved( aCC ) ;
						}
						aSM = annotation.getAnnotationStraight() ;
						if ( aSM != null && aSM.getName() != null ) {
							aSC = new astrolabe.model.AnnotationStraight() ;
							aSC.setName( ns+aSM.getName() ) ;

							for ( astrolabe.model.Text tM : aSM.getText() ) {
								tC = new astrolabe.model.Text() ;
								tC.setName( ns+tM.getName() ) ;
								tC.setValue( tM.getValue() ) ;
								AstrolabeFactory.modelOf( tC ) ;

								tC.setName( tM.getName() ) ;

								tC.setSubscript( tM.getSubscript() ) ;
								tC.setSuperscript( tM.getSuperscript() ) ;

								aSC.addText( tC ) ;
							}

							AstrolabeFactory.modelOf( aSC ) ;

							aSC.setName( aSM.getName() ) ;
							// setPurpose already done by AstrolabeFactory.modelOf()
							// aSC.setPurpose( aSM.getPurpose() ) ;
							// setAnchor already done by AstrolabeFactory.modelOf()
							// aSC.setAnchor( aSM.getAnchor() ) ;
							aSC.setReverse( aSM.getReverse() ) ;
							aSC.setRadiant( aSM.getRadiant() ) ;

							aC = new astrolabe.model.Annotation() ;
							aC.setAnnotationStraight( aSC ) ;
						}
						peer.addAnnotation( aC ) ;
					}
				} catch ( ParameterNotValidException e ) { // AstrolabeFactory.modelOf()
					throw new RuntimeException( e.toString() ) ;
				}

				try {
					circle = new CircleParallel( peer, projector ) ;

					ps.operator.gsave() ;

					circle.headPS( ps ) ;
					circle.emitPS( ps, false ) ;
					circle.tailPS( ps ) ;

					ps.operator.grestore() ;
				} catch ( ParameterNotValidException e ) {}
			}
		} else {
			l = list() ;
			ps.operator.mark() ;
			for ( int n=l.size() ; n>0 ; n-- ) {
				xy = l.get( n-1 ) ;
				ps.push( xy[0] ) ;
				ps.push( xy[1] ) ;
			}
			try {
				ps.custom( ApplicationConstant.PS_PROLOG_POLYLINE ) ;

				// halo stroke
				ps.operator.currentlinewidth() ;
				ps.operator.dup();
				ps.push( (Double) ( Registry.retrieve( ApplicationConstant.PK_CHART_HALOMAX ) ) ) ; 
				ps.push( (Double) ( Registry.retrieve( ApplicationConstant.PK_CHART_HALOMIN ) ) ) ; 
				ps.push( (Double) ( Registry.retrieve( ApplicationConstant.PK_CHART_HALO ) ) ) ; 
				ps.custom( ApplicationConstant.PS_PROLOG_HALO ) ;
				ps.operator.mul( 2 ) ;
				ps.operator.add() ;
				ps.operator.gsave() ;
				ps.operator.setlinewidth() ;
				ps.operator.setlinecap( 2 ) ;
				ps.operator.setgray( 1 ) ;
				ps.operator.stroke() ;
				ps.operator.grestore() ;
			} catch ( ParameterNotValidException e ) {
				throw new RuntimeException( e.toString() ) ;
			}
			ps.operator.gsave() ;
			ps.operator.stroke() ;
			ps.operator.grestore() ;

			if ( getDial() != null ) {
				try {
					PostscriptEmitter dial ;

					ps.operator.gsave() ;

					dial = AstrolabeFactory.companionOf( getDial(), (Baseline) this ) ;
					dial.headPS( ps ) ;
					dial.emitPS( ps ) ;
					dial.tailPS( ps ) ;

					ps.operator.grestore() ;
				} catch ( ParameterNotValidException e ) {
					throw new RuntimeException( e.toString() ) ;
				}
			}

			if ( getAnnotation() != null ) {
				try {
					ApplicationHelper.emitPS( ps, getAnnotation() ) ;
				} catch ( ParameterNotValidException e ) {
					throw new RuntimeException( e.toString() ) ;
				}
			}
		}
	}

	public void tailPS( PostscriptStream ps ) {
	}

	public static double[] intersection( double rdB, double gnB, double blA, double blB, double blGa ) throws ParameterNotValidException {
		double blC, blAl, blBe ;
		double rdGa[], rdDe ;
		double gnGa[], gnDe ;
		double[] rdaz, gnaz ;
		double[] r ;

		rdGa = new double[2] ;
		gnGa = new double[2] ;
		rdaz = new double[2] ;
		gnaz = new double[2] ;

		r = new double[4] ;

		// i. rdST<gnST && abs( gnST-rdST )>180 is equivalent to rdST>gnST && abs ( gnST-rdST )<180
		// ii. rdST>gnST && abs( gnST-rdST )>180 is equivalent to rdST<gnST && abs ( gnST-rdST )<180
		if ( 180<blGa ) {			// case i.
			blGa = -( 2*180-blGa ) ;
		} else if ( -180>blGa ) {	// case ii.
			blGa = blGa+2*180 ;
		}

		if ( blA==0 ) {	// rdLa==90
			blC = blB ;

			// intersection test
			if ( blGa<0?!( gnB>( rdB-blC ) ):!( rdB>( gnB-blC ) ) ) {
				String msg ;

				msg = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_MESSAGE_PARAMETERNOTAVLID ) ;
				msg = MessageFormat.format( msg, new Object[] { "", "" } ) ;
				throw new ParameterNotValidException( msg ) ;
			}

			rdDe = 180-Math.lawOfEdgeCosineSolveAngle( gnB, rdB, blC) ;
			gnDe = Math.lawOfEdgeCosineSolveAngle( rdB, blC, gnB) ;

			rdGa[0] = -( rdDe+blGa ) ;
			rdGa[1] = rdDe-blGa ;
			gnGa[0] = -gnDe ;
			gnGa[1] = gnDe ;

			rdaz[0] = 180+rdGa[0] ;
			rdaz[1] = 180+rdGa[1] ;
			gnaz[0] = CAACoordinateTransformation.MapTo0To360Range( 180+gnGa[0] ) ;
			gnaz[1] = CAACoordinateTransformation.MapTo0To360Range( 180+gnGa[1] ) ;
		} else if ( blB==0 ) {	// gnLa==90
			blC = blA ;

			// intersection test
			if ( blGa<0?!( gnB>( rdB-blC ) ):!( rdB>( gnB-blC ) ) ) {
				String msg ;

				msg = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_MESSAGE_PARAMETERNOTAVLID ) ;
				msg = MessageFormat.format( msg, new Object[] { "", "" } ) ;
				throw new ParameterNotValidException( msg ) ;
			}

			rdDe = Math.lawOfEdgeCosineSolveAngle( gnB, rdB, blC) ;
			gnDe = 180-Math.lawOfEdgeCosineSolveAngle( rdB, blC, gnB) ;

			rdGa[0] = -rdDe ;
			rdGa[1] = rdDe ;
			gnGa[0] = -( gnDe-blGa ) ;
			gnGa[1] = gnDe+blGa ;

			rdaz[0] = 180+rdGa[0] ;
			rdaz[1] = 180+rdGa[1] ;
			gnaz[0] = CAACoordinateTransformation.MapTo0To360Range( 180+gnGa[0] ) ;
			gnaz[1] = CAACoordinateTransformation.MapTo0To360Range( 180+gnGa[1] ) ;
		} else if ( Math.sin( blGa )==0 ) {	/* rdST==gnST,
															rdST==gnST+180 */
			blC = java.lang.Math.abs( blA-blB*Math.cos( blGa ) ) ;

			// intersection test
			if ( blGa<0?!( gnB>( rdB-blC ) ):!( rdB>( gnB-blC ) ) ) {
				String msg ;

				msg = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_MESSAGE_PARAMETERNOTAVLID ) ;
				msg = MessageFormat.format( msg, new Object[] { "", "" } ) ;
				throw new ParameterNotValidException( msg ) ;
			}

			if ( blC>0 ) {
				rdDe = 180-Math.lawOfEdgeCosineSolveAngle( gnB, rdB, blC) ;
				gnDe = Math.lawOfEdgeCosineSolveAngle( rdB, blC, gnB) ;
			} else {
				rdDe = Math.lawOfEdgeCosineSolveAngle( gnB, rdB, blC) ;
				gnDe = 180-Math.lawOfEdgeCosineSolveAngle( rdB, blC, gnB) ;
			}

			rdGa[0] = -rdDe ;
			rdGa[1] =  rdDe ;
			gnGa[0] = -gnDe ;
			gnGa[1] = gnDe ;

			rdaz[0] = 180+rdGa[0] ;
			rdaz[1] = 180+rdGa[1] ;
			gnaz[0] = 180+gnGa[0] ;
			gnaz[1] = 180+gnGa[1] ;

		} else {	/* rdST>gnST && abs( gnST-rdST )>180,
						rdST>gnST && abs( gnST-rdST )<180,
						rdST>gnST && abs( gnST-rdST )<90,
						rdST<gnST && abs( gnST-rdST )>180,
						rdST<gnST && abs( gnST-rdST )<180,
						rdST<gnST && abs( gnST-rdST )<90 */
			blC = Math.lawOfEdgeCosine( blA, blB, blGa ) ;
			blAl = Math.lawOfEdgeCosineSolveAngle( blA, blB, blC) ;
			blBe = Math.lawOfEdgeCosineSolveAngle( blB, blC, blA) ;

			// intersection test
			if ( blGa<0?!( gnB>( rdB-blC ) ):!( rdB>( gnB-blC ) ) ) {
				String msg ;

				msg = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_MESSAGE_PARAMETERNOTAVLID ) ;
				msg = MessageFormat.format( msg, new Object[] { "", "" } ) ;
				throw new ParameterNotValidException( msg ) ;
			}

			rdDe = Math.lawOfEdgeCosineSolveAngle( gnB, rdB, blC) ;
			gnDe = Math.lawOfEdgeCosineSolveAngle( rdB, blC, gnB) ;

			if ( rdDe<( 180-blBe ) ) {	// aph	
				gnDe = 180-gnDe ;

				if ( blGa<0 ) {
					rdGa[0] = blBe+rdDe ;
					rdGa[1] = blBe-rdDe ;
					gnGa[0] = 180-blAl+gnDe ;
					gnGa[1] = 180-blAl-gnDe ;

					rdaz[0] = 180-rdGa[0] ;
					rdaz[1] = 180-rdGa[1] ;
					gnaz[0] = CAACoordinateTransformation.MapTo0To360Range( 180-gnGa[0] ) ;
					gnaz[1] = CAACoordinateTransformation.MapTo0To360Range( 180-gnGa[1] ) ;
				} else {
					rdGa[0] = blBe-rdDe ;
					rdGa[1] = blBe+rdDe ;
					gnGa[0] = 180-blAl-gnDe ;
					gnGa[1] = 180-blAl+gnDe ;

					rdaz[0] = 180+rdGa[0] ;
					rdaz[1] = 180+rdGa[1] ;
					gnaz[0] = CAACoordinateTransformation.MapTo0To360Range( 180+gnGa[0] ) ;
					gnaz[1] = CAACoordinateTransformation.MapTo0To360Range( 180+gnGa[1] ) ;
				}
			} else {						// per
				rdDe = 180-rdDe ;

				if ( blGa<0 ) {
					rdGa[0] = 180-blBe+rdDe ;
					rdGa[1] = 180-blBe-rdDe ;
					gnGa[0] = blAl+gnDe ;
					gnGa[1] = blAl-gnDe ;

					rdaz[0] = 180+rdGa[0] ;
					rdaz[1] = 180+rdGa[1] ;
					gnaz[0] = 180+gnGa[0] ;
					gnaz[1] = 180+gnGa[1] ;
				} else {
					rdGa[0] = 180-blBe-rdDe ;
					rdGa[1] = 180-blBe+rdDe ;
					gnGa[0] = blAl-gnDe ;
					gnGa[1] = blAl+gnDe ;

					rdaz[0] = 180-rdGa[0] ;
					rdaz[1] = 180-rdGa[1] ;
					gnaz[0] = 180-gnGa[0] ;
					gnaz[1] = 180-gnGa[1] ;
				}
			}
		}

		r[0] = rdaz[0] ;
		r[1] = rdaz[1] ;
		r[2] = gnaz[0] ;
		r[3] = gnaz[1] ;

		return r ;
	}

	public double intersect( CircleParallel gn, boolean leading ) throws ParameterNotValidException {
		CAA2DCoordinate cA, cO, c ;
		double blA, blB, blGa, rdB, gnB ;
		double[] rdeqA = new double[2], rdeqO = new double[2], rdhoA, rdhoO, gneq = new double[2], gnho ;
		double rdhoC[], rdST, rdLa, gnhoC[], gnST, gnLa ;
		double[] inaz ;
		double gnal ;

		rdhoC = zenit() ;
		rdST = rdhoC[0] ;
		rdLa = rdhoC[1] ;
		gnhoC = gn.zenit() ;
		gnST = gnhoC[0] ;
		gnLa = gnhoC[1] ;

		rdB = 90-al ;
		gnB = 90-gn.al ;
		blA = 90-rdLa ;//horizon.getLa() ;
		blB = 90-gnLa ;//gn.dotDot().getLa() ;
		blGa = gnST-rdST ;//gn.dotDot().getST()-horizon.getST() ;

		inaz = CircleParallel.intersection( rdB, gnB, blA, blB, blGa ) ;

		// unconvert local rd into actual
		cA = CAACoordinateTransformation.Horizontal2Equatorial( inaz[0], al, rdLa/*horizon.getLa()*/ ) ;
		rdeqA[0] = CAACoordinateTransformation.HoursToDegrees( cA.X() ) ;
		rdeqA[1] = cA.Y() ;
		rdeqA[0] = rdST-rdeqA[0] ;//horizon.getST()-rdeqA[0] ;
		rdhoA = projector.unconvert( rdeqA ) ;

		cO = CAACoordinateTransformation.Horizontal2Equatorial( inaz[1], al, rdLa/*horizon.getLa()*/ ) ;
		rdeqO[0] = CAACoordinateTransformation.HoursToDegrees( cO.X() ) ;
		rdeqO[1] = cO.Y() ;
		rdeqO[0] = rdST-rdeqO[0] ;//horizon.getST()-rdeqO[0] ;
		rdhoO = projector.unconvert( rdeqO ) ;

		// sort associated values
		if ( rdhoA[0]>rdhoO[0] ) {
			double v ;

			v = rdhoA[0] ;
			rdhoA[0] = rdhoO[0] ;
			rdhoO[0] = v ;

			v = inaz[2] ;
			inaz[2] = inaz[3] ;
			inaz[3] = v ;
		}

		// unconvert local gn into actual
		gnal = AstrolabeFactory.valueOf( gn.getAngle() ) ;
		c = CAACoordinateTransformation.Horizontal2Equatorial( leading?inaz[3]:inaz[2], gnal, gnLa/*gn.dotDot().getLa()*/ ) ;
		gneq[0] = CAACoordinateTransformation.HoursToDegrees( c.X() ) ;
		gneq[1] = c.Y() ;
		gneq[0] = gnST-gneq[0] ;//gn.dotDot().getST()-gneq[0] ;
		gnho = gn.projector.unconvert( gneq ) ;//dotDot().unconvert( gneq ) ;

		if ( ! gn.probe( CAACoordinateTransformation.MapTo0To360Range( gnho[0] ) ) ) {
			throw new ParameterNotValidException( new Double( gnho[0] ).toString() ) ;
		}

		return leading?rdhoO[0]:rdhoA[0] ;
	}

	public double intersect( CircleMeridian gn, boolean leading ) throws ParameterNotValidException {
		CAA2DCoordinate cA, cO ;
		double blA, blB, blGa, rdB ;
		double[] rdeqA = new double[2], rdeqO = new double[2], rdhoA, rdhoO ;
		double rdhoC[], rdST, rdLa ;
		double inaz[], gnal ;

		rdhoC = zenit() ;
		rdST = rdhoC[0] ;
		rdLa = rdhoC[1] ;

		rdB = 90-al ;
		blA = 90-rdLa ;//horizon.getLa() ;
		blB = 90-gn.transformParallelLa() ;
		blGa = gn.transformParallelST()-rdST ;//horizon.getST() ;

		inaz = CircleParallel.intersection( rdB, 90, blA, blB, blGa ) ;

		// unconvert local rd into actual
		cA = CAACoordinateTransformation.Horizontal2Equatorial( inaz[0], al, rdLa/*horizon.getLa()*/ ) ;
		rdeqA[0] = CAACoordinateTransformation.HoursToDegrees( cA.X() ) ;
		rdeqA[1] = cA.Y() ;
		rdeqA[0] = rdST-rdeqA[0] ;//horizon.getST()-rdeqA[0] ;
		rdhoA = projector.unconvert( rdeqA ) ;

		cO = CAACoordinateTransformation.Horizontal2Equatorial( inaz[1], al, rdLa/*horizon.getLa()*/ ) ;
		rdeqO[0] = CAACoordinateTransformation.HoursToDegrees( cO.X() ) ;
		rdeqO[1] = cO.Y() ;
		rdeqO[0] = rdST-rdeqO[0] ;//horizon.getST()-rdeqO[0] ;
		rdhoO = projector.unconvert( rdeqO ) ;

		// sort associated values
		if ( rdhoA[0]>rdhoO[0] ) {
			double v ;

			v = rdhoA[0] ;
			rdhoA[0] = rdhoO[0] ;
			rdhoO[0] = v ;

			v = inaz[2] ;
			inaz[2] = inaz[3] ;
			inaz[3] = v ;
		}

		gnal = leading?gn.transformMeridianAl( inaz[3] ):gn.transformMeridianAl( inaz[2] ) ;

		if ( ! gn.probe( gnal ) ) {
			throw new ParameterNotValidException( new Double( gnal ).toString() ) ;
		}

		return leading?rdhoO[0]:rdhoA[0] ;
	}

	public boolean probe( double az ) {
		return probe( az, begin, end ) ;
	}

	public static boolean probe( double angle, double begin, double end ) {
		return begin>end?angle>=begin||angle<=end:angle>=begin&&angle<=end ;
	}

	public double mapIndexToScale( int index ) {
		return mapIndexToAngleOfScale( index, interval, begin, end ) ;
	}

	public double mapIndexToScale( double span ) {
		return mapIndexToAngleOfScale( 0, span, begin, end ) ;
	}

	public double mapIndexToScale( int index, double span ) {
		return mapIndexToAngleOfScale( index, span, begin, end ) ;
	}

	public static double mapIndexToAngleOfScale( int index, double span, double begin, double end ) {
		double r ;
		double s ;

		if ( index<0 ) { // last
			s = java.lang.Math.abs( end-(int) ( end/span )*span ) ;
			if ( end>0 ) {
				if ( end>begin ) {
					r = end-s ;
				} else { // end<begin
					r = end+span-s ;
				}
			} else { // end<0
				if ( end>begin ) {
					r = end-span+s ;
				} else { // end<begin
					r = end+s ;
				}
			}
		} else {
			s = java.lang.Math.abs( begin-(int) ( begin/span )*span ) ;
			if ( begin>0 ) {
				if ( begin<end ) {
					r = begin+span-s+index*span ;
				} else { // begin>end
					r = begin-s-index*span ;
				}
			} else { // begin<0
				if ( begin<end ) {
					r = begin+s+index*span ;
				} else { // begin>end
					r = begin-span+s-index*span ;
				}
			}
		}

		return r ;
	}

	public double[] zenit() {
		return projector.convert( 0, 90 ) ;
	}

	public double mapIndexToRange() {
		return gap( 0, interval, begin , end ) ;
	}

	public double mapIndexToRange( double begin, double end ) {
		return gap( 0, interval, begin , end ) ;
	}

	public double mapIndexToRange( int index, double begin, double end ) {
		return gap( index, interval, begin , end ) ;
	}

	public static double gap( int index, double interval, double begin, double end ) {
		double r ;
		double d, g ;

		d = begin>end?360+end-begin:end-begin ;
		g = d-(int) ( d/interval )*interval ;

		r = ( ( Math.isLim0( g )?interval:g )/2 )+index*interval ;

		return r ;
	}
}
