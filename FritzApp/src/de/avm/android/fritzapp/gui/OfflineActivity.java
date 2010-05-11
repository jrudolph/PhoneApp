/* 
 * Copyright 2010 by AVM GmbH <info@avm.de>
 *
 * This software contains free software; you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License ("License") as 
 * published by the Free Software Foundation  (version 3 of the License). 
 * This software is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the copy of the 
 * License you received along with this software for more details.
 */

package de.avm.android.fritzapp.gui;


/* Add to activities to show dependant on connection to the FRITZ!Box */
public interface OfflineActivity
{
	/*
	 * Implementing classes have to declare following methods to be
	 * called dynamically
	 * 
	 * public static Intent showIntent();
	 * 		The method checks if the Activity could be shown, inspects
	 * 		connection status to decide this. It delivers an Intent
	 * 		for startActivity(), or null
	 */
}
