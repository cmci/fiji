/**
 * 
 */
package fiji.plugin.trackmate.action;

import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.DisplayerPanel;

/**
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Sep 23, 2011
 *
 */
public class ResetSpotTimeFeatureAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/time.png"));
	
	public ResetSpotTimeFeatureAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(TrackMateModel model) {
		logger.log("Reset spot time.\n");
		float dt = model.getSettings().dt;
		if (dt == 0) {
			dt = 1;
		}
		Set<Integer> frames = model.getSpots().keySet();
		for(int frame : frames) {
			List<Spot> spots = model.getSpots().get(frame);
			for(Spot spot : spots) {
				spot.putFeature(SpotFeature.POSITION_T, frame * dt); 
			}
			logger.setProgress((float) (frame + 1) / frames.size());
		}
		logger.log("Done.\n");
		logger.setProgress(0);
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"Reset the time feature of all spots: it is set to the frame number "  +
				"times the time resolution. " +
				"</html>";
	}

	@Override
	public String toString() {
		return "Reset spot time";
	}
	
}
