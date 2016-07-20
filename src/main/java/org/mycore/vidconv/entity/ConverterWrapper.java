/*
 * $Id$ 
 * $Revision$ $Date$
 *
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
package org.mycore.vidconv.entity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.vidconv.encoder.FFMpegImpl;
import org.mycore.vidconv.service.ConverterService.ConverterJob;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement
public class ConverterWrapper implements Comparable<ConverterWrapper> {

    private String parentId;

    private String id;

    private String command;

    private String fileName;

    private boolean running;

    private boolean done;

    private Instant addTime;

    private Instant startTime;

    private Instant endTime;

    private Integer exitValue;

    private Progress progress;

    private String outputStream;

    private String errorStream;

    ConverterWrapper() {
    }

    public ConverterWrapper(final String id, final ConverterJob converter) {
        this();

        this.id = id;
        this.parentId = converter.parentId();
        this.command = converter.command();
        this.fileName = converter.fileName();
        this.running = converter.isRunning();
        this.done = converter.isDone();
        this.addTime = converter.addTime();
        this.startTime = converter.startTime();
        this.endTime = converter.endTime();
        this.exitValue = converter.exitValue();
        this.outputStream = converter.outputStream();
        this.errorStream = converter.errorStream();
    }

    @XmlAttribute(name = "parentId")
    public String getParentId() {
        return parentId;
    }

    @XmlAttribute(name = "id")
    public String getId() {
        return id;
    }

    @XmlElement(name = "command")
    public String getCommand() {
        return command;
    }

    @XmlAttribute(name = "file")
    public String getFileName() {
        return fileName;
    }

    @XmlAttribute(name = "running")
    public boolean isRunning() {
        return running;
    }

    @XmlAttribute(name = "done")
    public boolean isDone() {
        return done;
    }

    @XmlAttribute(name = "addTime")
    public String getAddTime() {
        return addTime != null ? addTime.toString() : null;
    }

    @XmlAttribute(name = "startTime")
    public String getStartTime() {
        return startTime != null ? startTime.toString() : null;
    }

    @XmlAttribute(name = "endTime")
    public String getEndTime() {
        return endTime != null ? endTime.toString() : null;
    }

    @XmlAttribute(name = "exitValue")
    public Integer getExitValue() {
        return exitValue;
    }

    @XmlElement(name = "progress")
    public Progress getProgress() {
        if (progress == null) {
            progress = Progress.buildProgress(this);
        }
        return progress;
    }

    @XmlElement(name = "outputStream")
    public String getOutputStream() {
        return outputStream;
    }

    @XmlElement(name = "errorStream")
    public String getErrorStream() {
        return errorStream;
    }

    public ConverterWrapper getBasicCopy() {
        final ConverterWrapper copy = new ConverterWrapper();

        copy.parentId = this.parentId;
        copy.id = this.id;
        copy.fileName = this.fileName;
        copy.running = this.running;
        copy.done = this.done;
        copy.addTime = this.addTime;
        copy.startTime = this.startTime;
        copy.endTime = this.endTime;
        copy.exitValue = this.exitValue;
        copy.progress = this.getProgress();

        return copy;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ConverterWrapper cw) {
        if (cw.isDone() && (!this.isDone() || this.isRunning()))
            return -1;
        if (this.isDone() && (!cw.isDone() || cw.isRunning()))
            return 1;
        if (this.isDone() && cw.isDone())
            return cw.endTime.compareTo(this.endTime);

        int ret = 0;
        if (cw.getProgress() != null && this.getProgress() != null)
            ret = cw.getProgress().compareTo(getProgress());

        return ret != 0 ? ret : this.addTime.compareTo(cw.addTime);
    }

    @XmlRootElement
    static class Progress implements Comparable<Progress> {
        private static final DateTimeFormatter DURATION_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

        @XmlAttribute(name = "percent")
        private Integer percent;

        @XmlAttribute(name = "elapsed")
        private String elapsed;

        @XmlAttribute(name = "estimate")
        private String estimate;

        Progress() {
        }

        static Progress buildProgress(final ConverterWrapper wrapper) {
            final Progress p = new Progress();

            if (wrapper.done) {
                p.percent = 100;
                p.elapsed = formatDuration(Duration.between(wrapper.startTime, wrapper.endTime));
                p.estimate = p.elapsed;
            } else if (wrapper.running) {
                p.percent = FFMpegImpl.progress(wrapper.getErrorStream());

                final Duration elapsed = Duration.between(wrapper.startTime, Instant.now());
                p.elapsed = formatDuration(elapsed);

                if (p.percent != null) {
                    long estimate = p.percent > 0 ? Long.divideUnsigned(elapsed.toNanos(), p.percent) * 100 : 0;
                    p.estimate = formatDuration(estimate);
                }
            } else {
                return null;
            }

            return p;
        }

        static String formatDuration(final Duration dur) {
            return formatDuration(dur.toNanos());
        }

        static String formatDuration(final long dur) {
            LocalTime fTime = LocalTime.ofNanoOfDay(dur);
            return fTime.format(DURATION_FORMAT);
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(Progress p) {
            return this.percent == null ? -1 : p.percent == null ? 1 : this.percent.compareTo(p.percent);
        }
    }
}
