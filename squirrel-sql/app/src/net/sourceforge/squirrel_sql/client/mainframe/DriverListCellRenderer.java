package net.sourceforge.squirrel_sql.client.mainframe;
/*
 * Copyright (C) 2001 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
import java.awt.Color;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDriver;

/**
 * A cell renderer, that shows Drivers that could not be loaded in red,
 * and thpse that could be loaded in green..
 *
 * @author  Henner Zeller
 */
public class DriverListCellRenderer extends DefaultListCellRenderer {
	/** Color for drivers that could be loaded. */
	private Icon OK_ICON;

	/** Color for drivers that could not be loaded. */
	private Icon FAIL_ICON;

	public DriverListCellRenderer(Icon ok, Icon fail)
	{
		OK_ICON = ok;
		FAIL_ICON = fail;
	}

	public Component getListCellRendererComponent(JList list,
												Object value,
												int index,
												boolean isSelected,
												boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value,  index, isSelected,
											cellHasFocus);
		ISQLDriver drv = (ISQLDriver)value;
		setIcon((drv.isJDBCDriverClassLoaded()) ? OK_ICON: FAIL_ICON);
		return this;
	}
}
