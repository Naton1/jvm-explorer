package com.github.naton1.jvmexplorer.helper;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.awt.*;

public class ScreenHelper {

	public static boolean isOnScreen(Rectangle rect, double minPercent) {
		return Screen.getScreens().stream().anyMatch(s -> {
			final Rectangle2D visual2d = s.getVisualBounds();
			final Rectangle visual = new Rectangle((int) visual2d.getMinX(),
			                                       (int) visual2d.getMinY(),
			                                       (int) (visual2d.getMaxX() - visual2d.getMinX()),
			                                       (int) (visual2d.getMaxY() - visual2d.getMinY()));
			if (!visual.intersects(rect)) {
				return false;
			}
			final Rectangle intersection = visual.intersection(rect);
			final int intersectionArea = intersection.width * intersection.height;
			final int windowArea = rect.width * rect.height;
			return intersectionArea >= windowArea * minPercent;
		});
	}

}
