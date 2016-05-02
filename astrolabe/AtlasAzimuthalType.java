
package astrolabe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;

import caa.CAACoordinateTransformation;

@SuppressWarnings("serial")
public class AtlasAzimuthalType extends astrolabe.model.AtlasAzimuthalType {

	private final static String DEFAULT_MAP = "Atlas.map" ;

	private double originRA ;
	private double originde ;
	private double extentRA ;
	private double extentde ;

	private String marshalURI ;

	private double sizex ;
	private double sizey ;

	private boolean northern ;

	private Projector projector ;

	private double spanRA = Double.NEGATIVE_INFINITY ;
	private double spande = Double.NEGATIVE_INFINITY ;

	// castor requirement for (un)marshalling
	public AtlasAzimuthalType() {
	}

	public AtlasAzimuthalType( astrolabe.model.Atlas atlas, double[] size, boolean northern, Projector projector ) {
		astrolabe.model.AngleType angle ;

		originRA = AstrolabeFactory.valueOf( atlas.getOrigin() )[1] ;
		originde = AstrolabeFactory.valueOf( atlas.getOrigin() )[2] ;
		extentRA = AstrolabeFactory.valueOf( atlas.getExtent() )[1] ;
		extentde = AstrolabeFactory.valueOf( atlas.getExtent() )[2] ;

		marshalURI = atlas.getMarshal() ;

		sizex = size[0] ;
		sizey = size[1] ;

		this.northern = northern ;

		this.projector = projector ;

		angle = atlas.getAtlasTypeChoice().getSpanDeclination() ;
		if ( angle == null ) {
			angle = atlas.getAtlasTypeChoice().getSpanRA() ;
			spanRA = AstrolabeFactory.valueOf( angle ) ;
		} else
			spande = AstrolabeFactory.valueOf( angle ) ;
	}

	public void addAllAtlasPage() throws ValidationException {
		AtlasPage atlasPage ;
		int nra, nde, grid[], tcp, bcp ;
		Vector p0, p0d, p1, p1d, p2, p3 ;
		Vector va, vb, vcen, vtop, vbot ;
		double ra, de, a, b, sina, r, cen, de0, sde, ra0, sra ;
		double rado, rade, rad, ho, he, h, s, c, p, g ;
		double[] xy, eq ;
		double r90[] = new double[] {
				0, -1, 0,
				1, 0, 0,
				0, 0, 1 } ;
		double r90c[] = new double[] {
				0, 1, 0,
				-1, 0, 0,
				0, 0, 1 } ;

		r = sizex/sizey ;

		if ( spande>Double.NEGATIVE_INFINITY ) {
			de0 = originde-spande ;
			nde = (int) ( java.lang.Math.abs( extentde/spande )+1 ) ;
			sde = ( extentde-spande )/( nde-1 ) ;

			grid = new int[ nde ] ;

			for ( int num=1, cde=0 ; cde<nde ; cde++ ) {
				de = de0-cde*sde ;

				xy = projector.project( originRA, de ) ;
				p0d = new Vector( xy ) ;
				xy = projector.project( originRA, de+spande ) ;
				p1d = new Vector( xy ) ;

				vb = new Vector( p1d )
				.sub( p0d ) ;
				b = vb.abs() ;
				a = b*r ;
				va = new Vector( vb )
				.scale( a )
				.apply( northern?r90:r90c ) ;

				sina = a/( 2*p0d.abs() ) ;
				spanRA = Math.asin( sina )*2 ;

				ra0 = originRA ;
				nra = (int) ( java.lang.Math.abs( extentRA/spanRA )+1 ) ;
				sra = ( extentRA-spanRA )/( nra-1 ) ;

				grid[cde] = nra ;

				for ( int cra=0 ; cra<nra ; cra++ ) {
					ra = ra0+cra*sra ;

					xy = projector.project( ra, de ) ;
					p0 = new Vector( xy ) ;
					xy = projector.project( ra+spanRA, de ) ;
					p3 = new Vector( xy ) ;

					va = new Vector( p3 )
					.sub( p0 ) ;
					a = va.abs() ;
					b = a/r ;
					vb = new Vector( va )
					.scale( b )
					.apply( northern?r90c:r90 ) ;

					p1 = new Vector( p0 )
					.add( vb ) ;
					p2 = new Vector( p3 )
					.add( vb ) ;

					vcen = new Vector( p3 )
					.sub( p1 ) ;
					cen = vcen.abs()/2 ;
					vcen
					.scale( cen )
					.add( p1 ) ;

					vtop = new Vector( va )
					.scale( a/2 )
					.add( p1 ) ;

					vbot = new Vector( va )
					.scale( a/2 )
					.add( p0 ) ;

					atlasPage = new AtlasPage() ;

					atlasPage.setNum( num++ ) ;
					atlasPage.setName( getName() ) ;

					atlasPage.setScale( sizex/a*100 ) ;

					atlasPage.setTcp( 0 ) ;
					atlasPage.setBcp( 0 ) ;
					atlasPage.setPcp( 0 ) ;
					atlasPage.setFcp( 0 ) ;

					atlasPage.setP0x( p0.x ) ;
					atlasPage.setP0y( p0.y ) ;
					atlasPage.setP1x( p1.x ) ;
					atlasPage.setP1y( p1.y ) ;
					atlasPage.setP2x( p2.x ) ;
					atlasPage.setP2y( p2.y ) ;
					atlasPage.setP3x( p3.x ) ;
					atlasPage.setP3y( p3.y ) ;

					eq = projector.unproject( p0.x, p0.y ) ;
					atlasPage.setP0( new astrolabe.model.P0() ) ;
					atlasPage.getP0().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getP0().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getP0().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getP0().getTheta(), false, false, eq[1] ) ;

					eq = projector.unproject( p1.x, p1.y ) ;
					atlasPage.setP1( new astrolabe.model.P1() ) ;
					atlasPage.getP1().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getP1().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getP1().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getP1().getTheta(), false, false, eq[1] ) ;

					eq = projector.unproject( p2.x, p2.y ) ;
					atlasPage.setP2( new astrolabe.model.P2() ) ;
					atlasPage.getP2().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getP2().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getP2().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getP2().getTheta(), false, false, eq[1] ) ;

					eq = projector.unproject( p3.x, p3.y ) ;
					atlasPage.setP3( new astrolabe.model.P3() ) ;
					atlasPage.getP3().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getP3().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getP3().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getP3().getTheta(), false, false, eq[1] ) ;

					// atlas page equatorial center (origin)
					atlasPage.setCenterx( vcen.x ) ;
					atlasPage.setCentery( vcen.y ) ;
					eq = projector.unproject( vcen.x, vcen.y ) ;
					eq[0] = CAACoordinateTransformation.DegreesToHours( eq[0] ) ;
					atlasPage.setCenter( new astrolabe.model.Center() ) ;
					atlasPage.getCenter().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getCenter().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getCenter().getPhi(), true, true, eq[0] ) ;
					modelOf( atlasPage.getCenter().getTheta(), true, false, eq[1] ) ;

					// declination in middle center of atlas page top
					atlasPage.setTopx( vtop.x ) ;
					atlasPage.setTopy( vtop.y ) ;
					eq = projector.unproject( vtop.x, vtop.y ) ;
					atlasPage.setTop( new astrolabe.model.Top() ) ;
					atlasPage.getTop().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getTop().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getTop().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getTop().getTheta(), false, false, eq[1] ) ;

					atlasPage.setBottomx( vbot.x ) ;
					atlasPage.setBottomy( vbot.y ) ;
					eq = projector.unproject( vbot.x, vbot.y ) ;
					atlasPage.setBottom( new astrolabe.model.Bottom() ) ;
					atlasPage.getBottom().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getBottom().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getBottom().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getBottom().getTheta(), false, false, eq[1] ) ;

					atlasPage.validate() ;

					addAtlasPage( atlasPage ) ;
				}
			}
		} else {
			ra0 = originRA ;
			nra = (int) ( java.lang.Math.abs( extentRA/spanRA )+1 ) ;
			sra = ( extentRA-spanRA )/( nra-1 ) ;

			rado = new Vector( projector.project( originRA, originde ) )
			.abs() ;
			rade = new Vector( projector.project( originRA+extentRA, originde-extentde ) )
			.abs() ;

			ho = Math.cos( spanRA/2 )*rado ;
			he = Math.cos( spanRA/2 )*rade ;

			h = he ;
			a = 2*Math.sin( spanRA/2 )*rade ;
			b = a/r ;
			p = b/h ;

			s = b ;
			for ( nde=1 ; ! ( he-s<ho ) ; nde++ ) {
				h = h-b ;
				b = h*p ;
				s = s+b ;
			}
			c = ( h-ho )/b ;

			grid = new int[ nde ] ;

			for ( int num=1, cde=0 ; cde<nde ; cde++ ) {
				h = cde==0?ho/( 1-p ):	// 1st ring
					cde==nde-1?he:		// Nth ring
						h/( 1-p )*( 1-( ( 1-c )/( nde-1 )*p ) ) ;

				rad = h/Math.cos( spanRA/2 ) ;
				de = projector.unproject( rad, 0 )[1] ;

				grid[cde] = nra ;

				for ( int cra=0 ; cra<nra ; cra++ ) {
					ra = ra0+cra*sra ;

					xy = projector.project( ra, de ) ;
					p0 = new Vector( xy ) ;
					xy = projector.project( ra+spanRA, de ) ;
					p3 = new Vector( xy ) ;

					va = new Vector( p3 )
					.sub( p0 ) ;
					a = va.abs() ;
					b = a/r ;
					vb = new Vector( va )
					.scale( b )
					.apply( northern?r90c:r90 ) ;

					p1 = new Vector( p0 )
					.add( vb ) ;
					p2 = new Vector( p3 )
					.add( vb ) ;

					vcen = new Vector( p3 )
					.sub( p1 ) ;
					cen = vcen.abs()/2 ;
					vcen
					.scale( cen )
					.add( p1 ) ;

					vtop = new Vector( va )
					.scale( a/2 )
					.add( p1 ) ;

					vbot = new Vector( va )
					.scale( a/2 )
					.add( p0 ) ;

					atlasPage = new AtlasPage() ;

					atlasPage.setNum( num++ ) ;

					atlasPage.setScale( sizex/a*100 ) ;

					atlasPage.setTcp( 0 ) ;
					atlasPage.setBcp( 0 ) ;
					atlasPage.setPcp( 0 ) ;
					atlasPage.setFcp( 0 ) ;

					atlasPage.setP0x( p0.x ) ;
					atlasPage.setP0y( p0.y ) ;
					atlasPage.setP1x( p1.x ) ;
					atlasPage.setP1y( p1.y ) ;
					atlasPage.setP2x( p2.x ) ;
					atlasPage.setP2y( p2.y ) ;
					atlasPage.setP3x( p3.x ) ;
					atlasPage.setP3y( p3.y ) ;

					eq = projector.unproject( p0.x, p0.y ) ;
					atlasPage.setP0( new astrolabe.model.P0() ) ;
					atlasPage.getP0().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getP0().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getP0().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getP0().getTheta(), false, false, eq[1] ) ;

					eq = projector.unproject( p1.x, p1.y ) ;
					atlasPage.setP1( new astrolabe.model.P1() ) ;
					atlasPage.getP1().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getP1().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getP1().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getP1().getTheta(), false, false, eq[1] ) ;

					eq = projector.unproject( p2.x, p2.y ) ;
					atlasPage.setP2( new astrolabe.model.P2() ) ;
					atlasPage.getP2().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getP2().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getP2().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getP2().getTheta(), false, false, eq[1] ) ;

					eq = projector.unproject( p3.x, p3.y ) ;
					atlasPage.setP3( new astrolabe.model.P3() ) ;
					atlasPage.getP3().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getP3().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getP3().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getP3().getTheta(), false, false, eq[1] ) ;

					// atlas page equatorial center (origin)
					atlasPage.setCenterx( vcen.x ) ;
					atlasPage.setCentery( vcen.y ) ;
					eq = projector.unproject( vcen.x, vcen.y ) ;
					eq[0] = CAACoordinateTransformation.DegreesToHours( eq[0] ) ;
					atlasPage.setCenter( new astrolabe.model.Center() ) ;
					atlasPage.getCenter().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getCenter().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getCenter().getPhi(), true, true, eq[0] ) ;
					modelOf( atlasPage.getCenter().getTheta(), true, false, eq[1] ) ;

					// declination in middle center of atlas page top
					atlasPage.setTopx( vtop.x ) ;
					atlasPage.setTopy( vtop.y ) ;
					eq = projector.unproject( vtop.x, vtop.y ) ;
					atlasPage.setTop( new astrolabe.model.Top() ) ;
					atlasPage.getTop().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getTop().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getTop().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getTop().getTheta(), false, false, eq[1] ) ;

					atlasPage.setBottomx( vbot.x ) ;
					atlasPage.setBottomy( vbot.y ) ;
					eq = projector.unproject( vbot.x, vbot.y ) ;
					atlasPage.setBottom( new astrolabe.model.Bottom() ) ;
					atlasPage.getBottom().setPhi( new astrolabe.model.Phi() ) ;
					atlasPage.getBottom().setTheta( new astrolabe.model.Theta() ) ;
					modelOf( atlasPage.getBottom().getPhi(), false, false, eq[0] ) ;
					modelOf( atlasPage.getBottom().getTheta(), false, false, eq[1] ) ;

					atlasPage.validate() ;

					addAtlasPage( atlasPage ) ;
				}
			}
		}

		for ( int cra=0 ; cra<grid[0] ; cra++ ) {
			atlasPage = (AtlasPage) getAtlasPage( cra ) ;
			atlasPage.setPcp( cra ) ;
			atlasPage.setFcp( cra+2 ) ;
		}
		getAtlasPage( 0 ).setPcp( grid[0] ) ;
		getAtlasPage( grid[0]-1 ).setFcp( 1 ) ;

		for ( int cde=1, num=0 ; cde<grid.length ; cde++ ) {
			g = (double) grid[cde-1]/grid[cde] ;

			if ( cde>1 )
				num = num+grid[cde-2] ;

			for ( int cra=0 ; cra<grid[cde] ; cra++ ) {
				tcp = num+(int) ( g*cra+.5 )+1 ;
				bcp = num+grid[cde-1]+cra+1 ;
				getAtlasPage( tcp-1 ).setBcp( bcp ) ;
				atlasPage = (AtlasPage) getAtlasPage( bcp-1 ) ;
				atlasPage.setTcp( tcp ) ;
				atlasPage.setPcp( bcp-1 ) ;
				atlasPage.setFcp( bcp+1 ) ;
			}
			getAtlasPage( num+grid[cde-1] ).setPcp( num+grid[cde-1]+grid[cde] ) ;
			getAtlasPage( num+grid[cde-1]+grid[cde]-1 ).setFcp( num+grid[cde-1]+1 ) ;
		}
	}

	public void headAUX() {
	}

	public void emitAUX() {
		URI xmlu ;
		File xmlf ;

		try {
			xmlu = new URI( marshalURI ) ;
			if ( xmlu.isAbsolute() ) {
				xmlf = new File( xmlu ) ;	
			} else {
				xmlf = new File( xmlu.getPath() ) ;
			}
			while ( ! xmlf.createNewFile() ) {
				xmlf.delete() ;
			}

			marshal( new FileOutputStream( xmlf ), "UTF-8" ) ;
		} catch ( URISyntaxException e ) {
			throw new RuntimeException( e.toString() ) ; // URI constructor
		} catch ( IOException e ) {
			throw new RuntimeException( e.toString() ) ; // File.createNewFile()
		}
	}

	public void tailAUX() {
	}

	public void headPS( AstrolabePostscriptStream ps ) {
	}

	public void emitPS( AstrolabePostscriptStream ps ) {
		AtlasPage atlasPage ;

		for ( int ap=0 ; ap<getAtlasPageCount() ; ap++ ) {
			atlasPage = (AtlasPage) getAtlasPage( ap ) ;

			ps.operator.gsave() ;

			atlasPage.headPS( ps ) ;
			atlasPage.emitPS( ps ) ;
			atlasPage.tailPS( ps ) ;

			ps.operator.grestore() ;
		}
	}

	public void tailPS( AstrolabePostscriptStream ps ) {
	}

	private void marshal( OutputStream xmls, String charset ) {
		Marshaller marshaller ;
		Mapping mapping ;
		String map ;
		Writer xmlw ;

		// 1. make AtlasStereographic.map (e.g.)
		// 2. remove unused class definitions from AtlasStereographic.map
		// 3. remove unused field definitions from AtlasStereographic and AtlasPage
		// 4. remove required attribute from field definitions for Phi and Theta
		// 5. remove package model from class definitions AtlasStereographic, AtlasPage, DMS and Rational
		// 6. rename AtlasStereographic to Atlas
		// 7. rename AtlasStereographic.map to Atlas.map
		try {
			map = Configuration.getValue(
					Configuration.getClassNode( this, getName(), null ),
					ApplicationConstant.PK_ATLAS_MAP, DEFAULT_MAP ) ;

			mapping = new Mapping() ;
			mapping.loadMapping( map ) ;

			xmlw = new OutputStreamWriter( xmls, charset ) ;

			marshaller = new Marshaller( xmlw ) ;
			marshaller.setMapping( mapping );
			marshaller.setEncoding( charset ) ;

			marshaller.setSuppressNamespaces( true ) ;

			// suppress xsi:type attribute (implies xmlns:xsi attribute) in marshaller output
			marshaller.setSuppressXSIType( true ) ;

			marshaller.marshal( this );

			xmls.flush() ;
			xmls.close() ;
		} catch ( MappingException e ) {
			throw new RuntimeException( e.toString() ) ; // Mapping constructor
		} catch ( ValidationException e ) {
			throw new RuntimeException( e.toString() ) ; // Marshaller.marshal
		} catch ( MarshalException e ) {
			throw new RuntimeException( e.toString() ) ; // Marshaller.marshal
		} catch ( IOException e ) {
			throw new RuntimeException( e.toString() ) ; // Mapping.loadMapping(), Marshaller constructor
		}
	}

	private static void modelOf( astrolabe.model.AngleType angle, boolean discrete, boolean time, double value ) {
		Rational v ;
		DMS dms ;

		v = new Rational( value ) ;

		if ( discrete ) {
			dms = new DMS( value ) ;
			if ( time ) {
				angle.setHMS( new astrolabe.model.HMS() ) ;
				angle.getHMS().setNeg( dms.getNeg() ) ;
				angle.getHMS().setHrs( dms.getDeg() ) ;
				angle.getHMS().setMin( dms.getMin() ) ;
				angle.getHMS().setSec( dms.getSec() ) ;
			} else {
				angle.setDMS( dms ) ;
			}
		} else {
			angle.setRational( v ) ;
		}
	}
}
