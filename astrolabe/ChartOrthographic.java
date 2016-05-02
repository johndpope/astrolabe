
package astrolabe;

import org.exolab.castor.xml.ValidationException;

@SuppressWarnings("serial")
public class ChartOrthographic extends ChartAzimuthalType {

	public void emitPS( ApplicationPostscriptStream ps ) {
		ChartPage page ;
		double[] view ;
		AtlasOrthographic atlas ;

		super.emitPS( ps ) ;

		if ( getAtlas() != null ) {
			page = new ChartPage() ;
			getChartPage().copyValues( page ) ;

			view = page.view() ;

			try {
				atlas = new AtlasOrthographic( getAtlas(), new double[] { view[0], view[1] }, getNorthern(), this ) ;
				atlas.addAllAtlasPage() ;
			} catch ( ValidationException e ) {
				throw new RuntimeException( e.toString() ) ;
			}

			atlas.headAUX() ;
			atlas.emitAUX() ;
			atlas.tailAUX() ;

			ps.operator.gsave() ;

			atlas.headPS( ps ) ;
			atlas.emitPS( ps ) ;
			atlas.tailPS( ps ) ;

			ps.operator.grestore() ;
		}
	}

	public double thetaToDistance( double de ) {
		return Math.cos( de ) ;
	}

	public double distanceToTheta( double d ) {
		return Math.acos( d ) ;
	}
}
