package com.johnny.draglayoutexpand.interfaces;

import com.johnny.draglayoutexpand.view.SwipeLayout;

public interface SwipeLayoutInterface {

	SwipeLayout.Status getCurrentStatus();
	
	void close();
	
	void open();
}
