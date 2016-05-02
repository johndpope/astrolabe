
package astrolabe;

import java.util.prefs.Preferences;

@SuppressWarnings("serial")
public class AnnotationCurved extends astrolabe.model.AnnotationCurved implements PostscriptEmitter {

	private final static double DEFAULT_SUBSCRIPTSHRINK		= .8 ;
	private final static double DEFAULT_SUBSCRIPTSHIFT		= -.3 ;
	private final static double DEFAULT_SUPERSCRIPTSHRINK	= .8 ;
	private final static double DEFAULT_SUPERSCRIPTSHIFT	= .5 ;

	private final static double DEFAULT_MARGIN				= 1.2 ;
	private final static double DEFAULT_RISE				= 1.2 ;

	private double subscriptshrink ;
	private double subscriptshift ;
	private double superscriptshrink ;
	private double superscriptshift ;

	private double margin ;
	private double rise ;

	public void register() {
		Preferences node ;

		node = Configuration.getClassNode( this, getName(), null ) ;

		subscriptshrink = Configuration.getValue( node,
				ApplicationConstant.PK_ANNOTATION_SUBSCRIPTSHRINK, DEFAULT_SUBSCRIPTSHRINK ) ;
		subscriptshift = Configuration.getValue( node,
				ApplicationConstant.PK_ANNOTATION_SUBSCRIPTSHIFT, DEFAULT_SUBSCRIPTSHIFT ) ;
		superscriptshrink = Configuration.getValue( node,
				ApplicationConstant.PK_ANNOTATION_SUPERSCRIPTSHRINK, DEFAULT_SUPERSCRIPTSHRINK ) ;
		superscriptshift = Configuration.getValue( node,
				ApplicationConstant.PK_ANNOTATION_SUPERSCRIPTSHIFT, DEFAULT_SUPERSCRIPTSHIFT ) ;

		margin = Configuration.getValue( node,
				ApplicationConstant.PK_ANNOTATION_MARGIN, DEFAULT_MARGIN ) ;
		rise = Configuration.getValue( node,
				ApplicationConstant.PK_ANNOTATION_RISE, DEFAULT_RISE ) ;
	}

	public void headPS( AstrolabePostscriptStream ps ) {
	}

	public void emitPS( AstrolabePostscriptStream ps ) {
		astrolabe.model.Script script ;
		int ns, n0 ;
		double p, height ;

		ps.operator.gsave() ;

		ps.array( true ) ;
		for ( ns=0, n0=0, height=0 ; ns<getScriptCount() ; ns++ ) {
			script = new astrolabe.model.Script() ;
			getScript( ns ).setupCompanion( script ) ;

			p = Configuration.getValue(
					Configuration.getClassNode( script, script.getName(), null ), script.getPurpose(), -1. ) ;
			if ( p<0 )
				p = Double.valueOf( script.getPurpose() ) ;

			if ( p==0 )
				n0++ ;
			else {
				AnnotationStraight.emitPS( ps, script, p, 0,
						subscriptshrink, subscriptshift, superscriptshrink, superscriptshift ) ;
				if ( height==0 )
					height = p ;
			}
		}
		ps.array( false ) ;

		if ( n0==ns ) {
			ps.operator.pop() ;
			ps.operator.grestore() ;

			return ;
		}

		ps.operator.currentpoint() ;
		ps.operator.translate() ;

		if ( new Boolean( getReverse() ).booleanValue() ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GREV ) ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
		}

		if ( getAnchor().equals( ApplicationConstant.AV_ANNOTATION_BOTTOMLEFT ) ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( rise ) ;
			ps.operator.neg() ;
			ps.push( ApplicationConstant.PS_PROLOG_GMOVE ) ;
			ps.operator.dup() ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GLEN ) ;
			ps.operator.div( 100 ) ;
			ps.operator.mul( getDistance() ) ;
			ps.push( margin ) ;
			ps.operator.add() ;
		} else if ( getAnchor().equals( ApplicationConstant.AV_ANNOTATION_BOTTOMMIDDLE ) ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( rise ) ;
			ps.operator.neg() ;
			ps.push( ApplicationConstant.PS_PROLOG_GMOVE ) ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
			ps.operator.dup() ;
			ps.push( ApplicationConstant.PS_PROLOG_TWIDTH ) ;
			ps.operator.pop() ;
			ps.operator.div( 2 ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GLEN ) ;
			ps.operator.div( 100 ) ;
			ps.operator.mul( getDistance() ) ;
			ps.operator.exch() ;
			ps.operator.sub() ;
		} else if ( getAnchor().equals( ApplicationConstant.AV_ANNOTATION_BOTTOMRIGHT ) ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( rise ) ;
			ps.operator.neg() ;
			ps.push( ApplicationConstant.PS_PROLOG_GMOVE ) ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
			ps.operator.dup() ;
			ps.push( ApplicationConstant.PS_PROLOG_TWIDTH ) ;
			ps.operator.pop() ;
			ps.operator.add( margin ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GLEN ) ;
			ps.operator.div( 100 ) ;
			ps.operator.mul( getDistance() ) ;
			ps.operator.exch() ;
			ps.operator.sub() ;
		} else if ( getAnchor().equals( ApplicationConstant.AV_ANNOTATION_MIDDLELEFT ) ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( height/2 ) ;
			ps.operator.neg() ;
			ps.push( ApplicationConstant.PS_PROLOG_GMOVE ) ;
			ps.operator.dup() ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GLEN ) ;
			ps.operator.div( 100 ) ;
			ps.operator.mul( getDistance() ) ;
			ps.push( margin ) ;
			ps.operator.add() ;
		} else if ( getAnchor().equals( ApplicationConstant.AV_ANNOTATION_MIDDLE ) ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( height/2 ) ;
			ps.operator.neg() ;
			ps.push( ApplicationConstant.PS_PROLOG_GMOVE ) ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
			ps.operator.dup() ;
			ps.push( ApplicationConstant.PS_PROLOG_TWIDTH ) ;
			ps.operator.pop() ;
			ps.operator.div( 2 ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GLEN ) ;
			ps.operator.div( 100 ) ;
			ps.operator.mul( getDistance() ) ;
			ps.operator.exch() ;
			ps.operator.sub() ;
		} else if ( getAnchor().equals( ApplicationConstant.AV_ANNOTATION_MIDDLERIGHT ) ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( height/2 ) ;
			ps.operator.neg() ;
			ps.push( ApplicationConstant.PS_PROLOG_GMOVE ) ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
			ps.operator.dup() ;
			ps.push( ApplicationConstant.PS_PROLOG_TWIDTH ) ;
			ps.operator.pop() ;
			ps.operator.add( margin ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GLEN ) ;
			ps.operator.div( 100 ) ;
			ps.operator.mul( getDistance() ) ;
			ps.operator.exch() ;
			ps.operator.sub() ;
		} else if ( getAnchor().equals( ApplicationConstant.AV_ANNOTATION_TOPLEFT ) ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( -( height+rise ) ) ;
			ps.operator.neg() ;
			ps.push( ApplicationConstant.PS_PROLOG_GMOVE ) ;
			ps.operator.dup() ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GLEN ) ;
			ps.operator.div( 100 ) ;
			ps.operator.mul( getDistance() ) ;
			ps.push( margin ) ;
			ps.operator.add() ;
		} else if ( getAnchor().equals( ApplicationConstant.AV_ANNOTATION_TOPMIDDLE ) ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( -( height+rise ) ) ;
			ps.operator.neg() ;
			ps.push( ApplicationConstant.PS_PROLOG_GMOVE ) ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
			ps.operator.dup() ;
			ps.push( ApplicationConstant.PS_PROLOG_TWIDTH ) ;
			ps.operator.pop() ;
			ps.operator.div( 2 ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GLEN ) ;
			ps.operator.div( 100 ) ;
			ps.operator.mul( getDistance() ) ;
			ps.operator.exch() ;
			ps.operator.sub() ;
		} else if ( getAnchor().equals( ApplicationConstant.AV_ANNOTATION_TOPRIGHT ) ) {
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( -( height+rise ) ) ;
			ps.operator.neg() ;
			ps.push( ApplicationConstant.PS_PROLOG_GMOVE ) ;
			ps.operator.newpath() ;
			ps.push( ApplicationConstant.PS_PROLOG_GDRAW ) ;
			ps.operator.dup() ;
			ps.push( ApplicationConstant.PS_PROLOG_TWIDTH ) ;
			ps.operator.pop() ;
			ps.operator.add( margin ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GPATH ) ;
			ps.push( ApplicationConstant.PS_PROLOG_GLEN ) ;
			ps.operator.div( 100 ) ;
			ps.operator.mul( getDistance() ) ;
			ps.operator.exch() ;
			ps.operator.sub() ;
		}

		ps.push( ApplicationConstant.PS_PROLOG_TPATH ) ;

		ps.operator.grestore() ;
	}

	public void tailPS( AstrolabePostscriptStream ps ) {
	}
}
