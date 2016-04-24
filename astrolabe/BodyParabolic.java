
package astrolabe;

import org.exolab.castor.xml.ValidationException;

@SuppressWarnings("serial")
public class BodyParabolic extends astrolabe.model.BodyPlanet implements Body {

	public BodyParabolic( Object peer, Projector projector ) throws ParameterNotValidException {
		ApplicationHelper.setupCompanionFromPeer( this, peer ) ;
		try {
			validate() ;
		} catch ( ValidationException e ) {
			throw new ParameterNotValidException( e.toString() ) ;
		}
	}

	public void headPS( PostscriptStream ps ) {
	}

	public void emitPS( PostscriptStream ps ) {
	}

	public void tailPS( PostscriptStream ps ) {
	}
}
