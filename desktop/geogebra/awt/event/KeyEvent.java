package geogebra.awt.event;

import geogebra.common.main.AbstractApplication;

import java.util.LinkedList;

public class KeyEvent extends geogebra.common.awt.event.KeyEvent{

	public static LinkedList<KeyEvent> pool = new LinkedList<KeyEvent>();
	private java.awt.event.KeyEvent event;

	private KeyEvent(java.awt.event.KeyEvent e) {
		AbstractApplication.debug("possible missing release()");
		this.event = e;
	}
	
	public static geogebra.awt.event.KeyEvent wrapEvent(java.awt.event.KeyEvent e) {
		if(!pool.isEmpty()){
			KeyEvent wrap = pool.getLast();
			wrap.event = e;
			pool.removeLast();
			return wrap;
		}
		return new KeyEvent(e);
	}
	
	public void release() {
		KeyEvent.pool.add(this);
	}
	
	@Override
	public char getKeyChar() {
		return event.getKeyChar();
	}

}
