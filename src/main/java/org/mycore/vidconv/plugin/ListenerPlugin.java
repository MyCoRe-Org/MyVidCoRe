/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 3
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.vidconv.plugin;

import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.common.event.EventManager;
import org.mycore.vidconv.common.event.Listener;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public abstract class ListenerPlugin extends GenericPlugin implements Listener {

    private static final EventManager EVENT_MANGER = EventManager.instance();

    protected boolean enabled;

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.GenericPlugin#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.GenericPlugin#enable()
     */
    @Override
    public void enable() {
        EVENT_MANGER.addListener(this);
        enabled = true;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.GenericPlugin#disable()
     */
    @Override
    public void disable() {
        EVENT_MANGER.removeListner(this);
        enabled = false;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.common.event.Listener#handleEvent(org.mycore.vidconv.common.event.Event)
     */
    @Override
    public abstract void handleEvent(Event<?> event) throws Exception;

}
