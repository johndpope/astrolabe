
package astrolabe;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings("serial")
public class CatalogADC7118 extends CatalogType implements PostscriptEmitter {

	private final static int C_CHUNK = 96+1/*0x0a*/ ;

	private final static Log log = LogFactory.getLog( CatalogADC7118.class ) ;

	private HashSet<String> restrict ;

	private Projector projector ;
	private double epoch ;

	private final static Comparator<CatalogRecord> comparator = new Comparator<CatalogRecord>() {

		public int compare( CatalogRecord a, CatalogRecord b ) {
			return ( (CatalogADC7118Record) a ).mag()<( (CatalogADC7118Record) b ).mag()?-1:
				( (CatalogADC7118Record) a ).mag()>( (CatalogADC7118Record) b ).mag()?1:0 ;
		}
	} ;

	public CatalogADC7118( Peer peer, Projector projector ) throws ParameterNotValidException {
		super( peer, projector ) ;

		String[] rv ;
		double epoch ;

		this.projector = projector ;

		epoch = ( (Double) Registry.retrieve( ApplicationConstant.GC_EPOCHE ) ).doubleValue() ;
		this.epoch = epoch ;

		restrict = new HashSet<String>() ;
		if ( ( (astrolabe.model.CatalogADC7118) peer ).getRestrict() != null ) {
			rv = ( (astrolabe.model.CatalogADC7118) peer ).getRestrict().split( "," ) ;
			for ( int v=0 ; v<rv.length ; v++ ) {
				restrict.add( rv[v] ) ;
			}
		}
	}

	public void headPS( PostscriptStream ps ) {
	}

	public void emitPS( PostscriptStream ps ) {
		Hashtable<String, CatalogRecord> catalog ;
		astrolabe.model.Annotation[] annotation ;
		astrolabe.model.BodyStellar bodyModel ;
		BodyStellar bodyStellar ;

		try {
			catalog = read() ;
		} catch ( ParameterNotValidException e ) {
			throw new RuntimeException( e.toString() ) ;
		}

		for ( CatalogRecord record : arrange( catalog ) ) {
			annotation = annotation( record ) ;

			try {
				bodyModel = record.toModel( epoch ).getBodyStellar() ;
				if ( annotation != null ) {
					bodyModel.setAnnotation( annotation( record ) ) ;

					record.register() ;
				}

				bodyStellar = new BodyStellar( bodyModel, projector ) ;
			} catch ( ParameterNotValidException e ) {
				throw new RuntimeException( e.toString() ) ;
			}

			ps.operator.gsave() ;

			bodyStellar.headPS( ps ) ;
			bodyStellar.emitPS( ps ) ;
			bodyStellar.tailPS( ps ) ;

			ps.operator.grestore() ;
		}
	}

	public void tailPS( PostscriptStream ps ) {
	}

	public CatalogRecord record( java.io.Reader catalog ) {
		CatalogADC7118Record r = null ;
		char[] c ;
		String l ;

		c = new char[C_CHUNK] ;

		try {
			while ( catalog.read( c, 0, C_CHUNK ) == C_CHUNK ) {
				l = new String( c ) ;
				l = l.substring( 0, l.length()-1 ) ;

				try {
					r = new CatalogADC7118Record( l ) ;

					if ( r.matchAny( restrict ) ) {
						break ;
					} else {
						continue ;
					}
				} catch ( ParameterNotValidException e ) {
					String msg ;

					msg = MessageCatalog.message( ApplicationConstant.GC_APPLICATION, ApplicationConstant.LK_MESSAGE_PARAMETERNOTAVLID ) ;
					msg = MessageFormat.format( msg, new Object[] { "\""+l+"\"", "" } ) ;
					log.warn( msg ) ;

					continue ;
				}
			}
		} catch ( IOException e ) {
			throw new RuntimeException( e.toString() ) ;
		}

		return r ;
	}

	public CatalogRecord[] arrange( Hashtable<String, CatalogRecord> catalog ) {
		CatalogRecord[] r ;
		List<CatalogRecord> l ;

		l = new ArrayList<CatalogRecord>( catalog.values() ) ;
		Collections.sort( l, comparator ) ;

		r = new CatalogRecord[l.size()] ;

		return l.toArray( r ) ;
	}
}
