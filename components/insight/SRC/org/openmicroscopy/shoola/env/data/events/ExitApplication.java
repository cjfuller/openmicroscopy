/*
 * org.openmicroscopy.shoola.env.data.events.ExitApplication
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

package org.openmicroscopy.shoola.env.data.events;



//Java imports

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.env.data.util.SecurityContext;
import org.openmicroscopy.shoola.env.event.RequestEvent;

/** 
 * Event fired by the agents to exit the application.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public class ExitApplication
    extends RequestEvent
{

	/** 
	 * Flag indicating to ask a question to the user before exiting the
	 * application.
	 */
	private boolean askQuestion;
	
	/** The last selected security context.*/
	private SecurityContext ctx;
	
    /** Creates a new instance. */
    public ExitApplication() 
    {
    	this(true);
    }
    
    /**
     * Creates a new instance.
     * 
     * @param askQuestion 	Pass <code>true</code> to ask a question before
     * 						closing the application, <code>false</code>
     * 						otherwise.
     */
    public ExitApplication(boolean askQuestion) 
    {
    	this.askQuestion = askQuestion;
    	ctx = null;
    }
    
    /**
     * Sets the security context.
     * 
     * @param ctx The context to set.
     */
    public void setSecurityContext(SecurityContext ctx)
    {
    	this.ctx = ctx;
    }
    
    /**
     * Returns the security context.
     * 
     * @return See above.
     */
    public SecurityContext getContext() { return ctx; }
    
    /**
     * Returns <code>true</code> to ask a question before closing the
     * application, <code>false</code> otherwise.
     * 
     * @return See above.
     */
    public boolean isAskQuestion() { return askQuestion; }

}
