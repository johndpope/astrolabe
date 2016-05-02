
package astrolabe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exolab.castor.xml.ValidationException;

import caa.CAA2DCoordinate;
import caa.CAACoordinateTransformation;
import caa.CAAPrecession;

@SuppressWarnings("serial")
public class CatalogADC5050 extends astrolabe.model.CatalogADC5050 implements PostscriptEmitter {

	private final static int C_CHUNK = 197+1/*0x0a*/ ;

	private final static Log log = LogFactory.getLog( CatalogADC5050.class ) ;

	private Hashtable<String, CatalogADC5050Record> catalog ;

	private Projector projector ;

	public CatalogADC5050( Projector projector ) {
		this.projector = projector ;
	}

	@SuppressWarnings("unchecked")
	private Hashtable<String, CatalogADC5050Record> unsafecast( Object hashtable ) {
		return (Hashtable<String, CatalogADC5050Record>) hashtable ;
	}

	public void addAllCatalogRecord() {
		Reader reader ;
		CatalogADC5050Record record ;
		String key ;

		key = getClass().getSimpleName()+":"+getName() ;
		catalog = unsafecast( Registry.retrieve( key ) ) ;
		if ( catalog == null ) {
			catalog = new Hashtable<String, CatalogADC5050Record>() ;
			Registry.register( key, catalog ) ;
		} else
			return ;

		try {
			reader = reader() ;

			while ( ( record = record( reader ) ) != null ) {
				try {
					record.inspect() ;
				} catch ( ParameterNotValidException e ) {
					log.warn( ParameterNotValidError.errmsg( record.HR, e.getMessage() ) ) ;

					continue ;
				}

				record.register() ;

				for ( astrolabe.model.CatalogADC5050Record select : getCatalogADC5050Record() ) {
					select.copyValues( record ) ;
					if ( Boolean.parseBoolean( record.getSelect() ) ) {
						catalog.put( record.HR, record ) ;

						break ;
					}
				}

				record.degister() ;
			}

			reader.close() ;
		} catch ( URISyntaxException e ) {
			throw new RuntimeException( e.toString() ) ;
		} catch ( MalformedURLException e ) {
			throw new RuntimeException( e.toString() ) ;
		} catch ( IOException e ) {
			throw new RuntimeException( e.toString() ) ;
		}
	}

	public void delAllCatalogRecord() {
		catalog.clear() ;
	}

	public CatalogRecord[] getCatalogRecord() {
		return catalog.values().toArray( new CatalogRecord[0] ) ;
	}

	public void headPS( ApplicationPostscriptStream ps ) {
	}

	public void emitPS( ApplicationPostscriptStream ps ) {
		List<CatalogADC5050Record> catalog ;
		Comparator<CatalogADC5050Record> comparator = new Comparator<CatalogADC5050Record>() {
			public int compare( CatalogADC5050Record a, CatalogADC5050Record b ) {
				double amag, bmag ;

				amag = Double.valueOf( a.Vmag ).doubleValue() ;
				bmag = Double.valueOf( b.Vmag ).doubleValue() ;

				return amag<bmag?-1:
					amag>bmag?1:
						0 ;
			}
		} ;
		astrolabe.model.Body body ;
		BodyStellar bodyStellar ;
		astrolabe.model.Position pm ;
		CAA2DCoordinate cpm, ceq ;
		double epoch, ra, de, pmRA, pmDE ;
		Double Epoch ;

		Epoch = (Double) Registry.retrieve( astrolabe.Epoch.RK_EPOCH ) ;
		if ( Epoch == null )
			epoch = astrolabe.Epoch.defoult() ;
		else
			epoch = Epoch.doubleValue() ;

		catalog = Arrays.asList( this.catalog
				.values()
				.toArray( new CatalogADC5050Record[0] ) ) ;
		Collections.sort( catalog, comparator ) ;

		for ( CatalogADC5050Record record : catalog ) {
			pmRA = 0 ;
			if ( record.pmRA.length()>0 )
				pmRA = new Double( record.pmRA ).doubleValue() ;
			pmDE = 0 ;
			if ( record.pmDE.length()>0 )
				pmDE = new Double( record.pmDE ).doubleValue() ;
			cpm = CAAPrecession.AdjustPositionUsingUniformProperMotion(
					epoch-2451545., record.RA(), record.de(), pmRA, pmDE ) ;
			ceq = CAAPrecession.PrecessEquatorial( cpm.X(), cpm.Y(), 2451545./*J2000*/, epoch ) ;
			ra = CAACoordinateTransformation.HoursToDegrees( ceq.X() ) ;
			de = ceq.Y() ;
			cpm.delete() ;
			ceq.delete() ;

			record.register() ;

			body = new astrolabe.model.Body() ;
			body.setBodyStellar( new astrolabe.model.BodyStellar() ) ;
			body.getBodyStellar().setName( record.HR ) ;
			body.getBodyStellar().initValues() ;

			body.getBodyStellar().setScript( record.getScript() ) ;
			body.getBodyStellar().setAnnotation( record.getAnnotation() ) ;

			pm = new astrolabe.model.Position() ;
			// astrolabe.model.SphericalType
			pm.setR( new astrolabe.model.R() ) ;
			pm.getR().setValue( 1 ) ;
			// astrolabe.model.AngleType
			pm.setPhi( new astrolabe.model.Phi() ) ;
			pm.getPhi().setRational( new astrolabe.model.Rational() ) ;
			pm.getPhi().getRational().setValue( ra ) ;  
			// astrolabe.model.AngleType
			pm.setTheta( new astrolabe.model.Theta() ) ;
			pm.getTheta().setRational( new astrolabe.model.Rational() ) ;
			pm.getTheta().getRational().setValue( de ) ;  

			body.getBodyStellar().setPosition( pm ) ;

			try {
				body.validate() ;
			} catch ( ValidationException e ) {
				throw new RuntimeException( e.toString() ) ;
			}

			bodyStellar = new BodyStellar( projector ) ;
			body.getBodyStellar().copyValues( bodyStellar ) ;

			bodyStellar.register() ;
			ps.operator.gsave() ;

			bodyStellar.headPS( ps ) ;
			bodyStellar.emitPS( ps ) ;
			bodyStellar.tailPS( ps ) ;

			ps.operator.grestore() ;
			bodyStellar.degister() ;

			record.degister() ;
		}
	}

	public void tailPS( ApplicationPostscriptStream ps ) {
	}

	public Reader reader() throws URISyntaxException, MalformedURLException {
		URI cURI ;
		URL cURL ;
		File cFile ;
		InputStream cCon ;
		GZIPInputStream cGZ ;

		cURI = new URI( getUrl() ) ;
		if ( cURI.isAbsolute() ) {
			cFile = new File( cURI ) ;	
		} else {
			cFile = new File( cURI.getPath() ) ;
		}
		cURL = cFile.toURL() ;

		try {
			cCon = cURL.openStream() ;

			cGZ = new GZIPInputStream( cCon ) ;
			return new InputStreamReader( cGZ ) ;
		} catch ( IOException egz ) {
			try {
				cCon = cURL.openStream() ;

				return new InputStreamReader( cCon ) ;
			} catch ( IOException eis ) {
				throw new RuntimeException ( egz.toString() ) ;
			}
		}
	}

	public CatalogADC5050Record record( java.io.Reader catalog ) {
		CatalogADC5050Record r = null ;
		char[] cl ;
		int o ;
		String rl ;

		cl = new char[C_CHUNK] ;
		o = 0 ;

		try {
			while ( catalog.read( cl, o++, 1 ) == 1 ) {
				if ( cl[o-1] == '\n' ) {
					if ( o<C_CHUNK ) {
						for ( o-- ; o<C_CHUNK ; o++ ) {
							cl[o] = ' ' ;
						}
						cl[o-1] = '\n' ;
					}
					rl = new String( cl ) ;
					o = 0 ;
					if ( ( r = record( rl ) ) != null )
						break ;
				}
			}
		} catch ( IOException e ) {
			throw new RuntimeException( e.toString() ) ;
		}

		return r ;
	}

	private CatalogADC5050Record record( String record ) {
		CatalogADC5050Record r = null ;

		try {
			r = new CatalogADC5050Record( record ) ;
		} catch ( ParameterNotValidException e ) {
			log.warn( ParameterNotValidError.errmsg( '"'+record+'"', e.getMessage() ) ) ;
		}

		return r ;
	}
}
