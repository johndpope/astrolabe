
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
public class CatalogADC5050 extends CatalogType {

	private final static int C_CHUNK = 197+1/*0x0a*/ ;

	private final static Log log = LogFactory.getLog( CatalogADC5050.class ) ;

	private HashSet<String> restrict ;

	private astrolabe.model.CatalogADC5050 peer ;
	private Projector projector ;
	private double epoch ;

	private final static Comparator<CatalogRecord> comparator = new Comparator<CatalogRecord>() {

		public int compare( CatalogRecord a, CatalogRecord b ) {
			return ( (CatalogADC5050Record) a ).Vmag()<( (CatalogADC5050Record) b ).Vmag()?-1:
				( (CatalogADC5050Record) a ).Vmag()>( (CatalogADC5050Record) b ).Vmag()?1:0 ;
		}
	} ;

	public CatalogADC5050( Object peer, Projector projector, double epoch ) throws ParameterNotValidException {
		super( peer, projector, epoch ) ;

		String[] rv ;

		this.peer = (astrolabe.model.CatalogADC5050) peer ;
		this.projector = projector ;
		this.epoch = epoch ;

		restrict = new HashSet<String>() ;
		if ( ( (astrolabe.model.CatalogADC5050) peer ).getRestrict() != null ) {
			rv = ( (astrolabe.model.CatalogADC5050) peer ).getRestrict().split( "," ) ;
			for ( int v=0 ; v<rv.length ; v++ ) {
				restrict.add( rv[v] ) ;
			}
		}
	}

	public void emitPS( PostscriptStream ps ) {
		java.util.Vector<BodyAreal> sign ;
		astrolabe.model.BodyAreal model ;
		HashSet<String> headline ;
		String[] sv, hv, bv ;
		CatalogRecord cr ;
		BodyAreal body ;

		for ( int s=0 ; s<peer.getSignCount() ; s++ ) {
			sign = new java.util.Vector<BodyAreal>() ;

			headline = new HashSet<String>() ;
			if ( peer.getSign( s ).getHeadline() != null ) {
				hv = peer.getSign( s ).getHeadline().split( "," ) ;
				for ( int h=0 ; h<hv.length ; h++ ) {
					headline.add( hv[h] ) ;
				}
			}

			sv = peer.getSign( s ).getValue().split( "," ) ;
			for ( int b=0 ; b<sv.length ; b++ ) {
				model = new astrolabe.model.BodyAreal() ;
				model.setType( ApplicationConstant.AV_BODY_SIGN ) ;

				bv = sv[b].split( ":" ) ;
				for ( int p=0 ; p<bv.length ; p++ ) {
					if ( ( cr = entry( bv[p] ) ) == null ) {
						break ; // element missing in catalog
					}
					try {
						model.addPosition( cr.toBody( epoch ).getBodyStellar().getPosition() ) ;
					} catch ( ParameterNotValidException e ) {
						throw new RuntimeException( e.toString() ) ;
					}
				}

				if ( model.getPositionCount()<bv.length ) { // element(s) missing in catalog
					break ;
				}

				try {
					body = new BodyAreal( model, projector ) ;
				} catch ( ParameterNotValidException e ) {
					throw new RuntimeException( e.toString() ) ;
				}

				if ( peer.getSign( s ).getAnnotationCount()>0 ) {
					if ( headline.size()==0 ) {
						if ( b==0 ) {
							body.setAnnotation( peer.getSign( s ).getAnnotation() ) ;
						}
					} else {
						if ( headline.contains( new Integer( b+1 ).toString() ) ) {
							body.setAnnotation( peer.getSign( s ).getAnnotation() ) ;
						}
					}
				}

				sign.add( body ) ;
			}
			for ( int b=0 ; b<sign.size() ; b++ ) {
				body = sign.get( b ) ;

				ps.operator.gsave() ;

				body.headPS( ps ) ;
				body.emitPS( ps ) ;
				body.tailPS( ps ) ;

				ps.operator.grestore() ;
			}
		}

		super.emitPS( ps ) ;
	}

	public CatalogRecord record( java.io.Reader catalog ) {
		CatalogADC5050Record r = null ;
		String l ;
		char[] c ;
		int o ;

		c = new char[C_CHUNK] ;
		o = 0 ;

		try {
			while ( catalog.read( c, o++, 1 ) == 1 ) {
				if ( c[o-1] == '\n' ) {
					if ( o<C_CHUNK ) {
						for ( o-- ; o<C_CHUNK ; o++ ) {
							c[o] = ' ' ;
						}
						c[o-1] = '\n' ;
					}
					l = new String( c ) ;
					o = 0 ;
				} else {
					continue ;
				}

				try {
					r = new CatalogADC5050Record( l ) ;

					if ( r.matchAny( restrict ) ) {
						break ;
					} else {
						continue ;
					}
				} catch ( ParameterNotValidException e ) {
					String msg ;

					msg = ApplicationHelper.getLocalizedString( ApplicationConstant.LK_MESSAGE_PARAMETERNOTAVLID ) ;
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
