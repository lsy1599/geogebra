package org.geogebra.web.full.gui.laf;

public class ChromeLookAndFeel extends GLookAndFeel {

	@Override
	public boolean isGraphingExamSupported() {
		return true;
	}

	@Override
	public boolean hasHeader() {
		return false;
	}
}