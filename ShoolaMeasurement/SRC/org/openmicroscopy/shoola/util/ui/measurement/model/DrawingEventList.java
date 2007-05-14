/*
 * measurement.model.Properties 
 *
  *------------------------------------------------------------------------------
 *  Copyright (C) 2006 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.util.ui.measurement.model;


//Java imports

//Third-party libraries

//Application-internal dependencies

/** 
 * 
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author	Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * 	<a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class DrawingEventList 
{
	public static final String UIMODEL_FIGUREADDED = "uiModel_FigureAdded"; 
	public static final String UIMODEL_FIGUREREMOVED = "uiModel_FigureRemoved"; 
	public static final String UIMODEL_FIGURESELECTED = "uiModel_FigureSelected"; 
	public static final String UIMODEL_FIGUREATTRIBUTECHANGED = "uiModel_FigureAttributeChanged"; 
	public static final String OBJECTMANAGER_FIGURESELECTED = "objectManager_FigureSelected"; 
	public static final String UIMODEL_ACTIVECHANNELCHANGED = "uiModel_ActiveChannelChanged";
	
	private DrawingEventList()
	{
	    // no code req'd
	}

	public static DrawingEventList get()
	{	
		if (ref == null)
			 // it's ok, we can call this constructor
			ref = new DrawingEventList();		
		return ref;
	}

	public Object clone()
		throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException(); 
		 // that'll teach 'em
	}

	private static DrawingEventList ref;
}


