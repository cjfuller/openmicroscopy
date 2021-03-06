/*
 * org.openmicroscopy.shoola.svc.transport.HttpChannel 
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
package org.openmicroscopy.shoola.svc.transport;



//Java imports
import java.io.IOException;

//Third-party libraries
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.openmicroscopy.shoola.svc.proxy.Reply;
import org.openmicroscopy.shoola.svc.proxy.Request;

//Application-internal dependencies

/** 
 * Top-channel that all channels using <code>HTTP</code> communication should 
 * extend. 
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public abstract class HttpChannel
{

	/** The default channel type. */
	public static final int DEFAULT = 0;
	
	/** The type identifying that one channel is created per request. */
    public static final int CONNECTION_PER_REQUEST = 1;
    
    /** Default property key for proxy host. */
    static final String		PROXY_HOST = "http.proxyHost";
    
    /** Default property key for proxy port. */
    static final String		PROXY_PORT = "http.proxyPort";
    
    /** 
     * Returns the channel corresponding to the passed type.
     * 
     * @return See above.
     */ 
    protected abstract HttpClient getCommunicationLink();
    
    /**
     * Returns the path derived from the server's URL.
     * 
     * @return See above.
     */
    protected abstract String getRequestPath();
    
    /**
     * Posts the request and catches the reply.
     * 
     * @param out	The request to post.
     * @param in	The reply to fill.
     * @throws TransportException If an error occurred while transferring data.
     * @throws IOException	If an error occurred while unmarshalling the method.
     */
    public void exchange(Request out, Reply in) 
    	throws TransportException, IOException
	{
	    //Sanity checks.
	    if (out == null) throw new NullPointerException("No request.");
	    if (in == null) throw new NullPointerException("No reply.");
	    
	    //Build HTTP request, send it, and wait for response.
	    //Then read the response into the Reply object.
	    HttpClient comLink = getCommunicationLink();
	    HttpMethod method = null;
	    try {
	        method = out.marshal();
	        method.setPath(getRequestPath());
	        comLink.executeMethod(method);
	        in.unmarshal(method, this);
	    } finally {
	        //Required by Http Client library.
	        if (method != null) method.releaseConnection();
	    }
	}
    
}
