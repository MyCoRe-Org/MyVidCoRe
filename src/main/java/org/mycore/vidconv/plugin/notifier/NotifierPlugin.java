/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
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
package org.mycore.vidconv.plugin.notifier;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientProperties;
import org.mycore.vidconv.backend.service.ConverterService;
import org.mycore.vidconv.backend.service.ConverterService.ConverterJob;
import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.common.util.JsonUtils;
import org.mycore.vidconv.frontend.entity.ConverterWrapper;
import org.mycore.vidconv.plugin.ListenerPlugin;
import org.mycore.vidconv.plugin.annotation.Plugin;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Plugin(name = "Notifier Plugin", description = "Notifies external callback URL.")
public class NotifierPlugin extends ListenerPlugin {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ScheduledExecutorService NOTIFY_SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.ListenerPlugin#handleEvent(org.mycore.vidconv.common.event.Event)
     */
    @Override
    public void handleEvent(Event<?> event) throws Exception {
        if (ConverterService.EVENT_CONVERT_DONE.equals(event.getType())
            && event.getSource().equals(ConverterService.class)) {
            notifyCallBack((ConverterJob) event.getObject());
        }
    }

    private void notifyCallBack(ConverterJob job) {
        if (job.completeCallBack() != null) {
            NOTIFY_SCHEDULER.schedule(new NotifyJob(job), 1, TimeUnit.MINUTES);
        }
    }

    private static class NotifyJob implements Runnable {

        private final static int MAX_TRIES = 10;

        private final ConverterJob job;

        private final AtomicInteger tries;

        public NotifyJob(ConverterJob job) {
            this.job = job;
            this.tries = new AtomicInteger(0);
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            try {
                if (!send()) {
                    if (MAX_TRIES > tries.incrementAndGet()) {
                        LOGGER.warn(
                            "Try to resend notification for job \"{}\" to callback url \"{}\" after 60 seconds.",
                            job.id(), job.completeCallBack());
                        NOTIFY_SCHEDULER.schedule(this, 1, TimeUnit.MINUTES);
                    } else {
                        LOGGER.error("Max retires ({}) reached for job \"{}\" to callback url \"{}\"", MAX_TRIES,
                            job.id(), job.completeCallBack());
                    }
                }
            } catch (JAXBException | IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        private boolean send() throws JAXBException, IOException {
            Client client = ClientBuilder.newClient();
            client.property(ClientProperties.CONNECT_TIMEOUT, 30000);
            client.property(ClientProperties.READ_TIMEOUT, 30000);

            WebTarget target = client.target(job.completeCallBack());
            Response response = target.request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(JsonUtils.toJSON(new ConverterWrapper(job.id(), job)), MediaType.APPLICATION_JSON));

            return response.getStatus() == 200;
        }
    }
}
