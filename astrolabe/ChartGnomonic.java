
package astrolabe;

@SuppressWarnings("serial")
public class ChartGnomonic extends ChartAzimuthalType {

	public ChartGnomonic( Peer peer ) throws ParameterNotValidException {
		super( peer ) ;
	}

	double thetaToDistance( double de ) {
		return Math.tan( 90-de ) ;
	}

	double distanceToTheta( double d ) {
		return 90-Math.atan( d ) ;
	}
}
