
package astrolabe;

import java.util.regex.Pattern;

import org.exolab.castor.util.RegExpEvaluator;

public class CastorJREEvaluator implements RegExpEvaluator {

	private Pattern pattern ;

	public CastorJREEvaluator() {
		pattern = null ;
	}

	public boolean matches( String string ) {
		return pattern==null?true:pattern.matcher( string ).find() ;
	}

	public void setExpression(String expression ) {
		if ( expression == null ) {
			pattern = null ;
		} else {
			pattern = Pattern.compile( expression ) ;
		}
	}

}
