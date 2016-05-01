
package astrolabe;

import caa.CAACoordinateTransformation;

@SuppressWarnings("serial")
public class DialHour extends DialDegree {

	public DialHour( Peer peer, Baseline baseline ) throws ParameterNotValidException {
		super( peer, baseline, CAACoordinateTransformation.HoursToDegrees( 1 ) ) ;
	}
}
