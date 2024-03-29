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
package org.mycore.vidconv.frontend.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.mycore.vidconv.backend.encoder.FFMpegImpl;
import org.mycore.vidconv.backend.service.ConverterService.ConverterJob;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Output;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement
public class ConverterWrapper implements Comparable<ConverterWrapper> {

    private String id;

    private int priority;

    private String command;

    private String inputPath;

    private String fileName;

    private String completeCallback;

    private List<Output> outputs;

    private HWAccelWrapper<? extends HWAccelDeviceSpec> hwAccel;

    private boolean running;

    private boolean done;

    private Instant addTime;

    private Instant startTime;

    private Instant endTime;

    private Integer exitValue;

    private Progress progress;

    private String outputStream;

    private String errorStream;

    protected ConverterWrapper() {
    }

    public ConverterWrapper(final String id, final ConverterJob converter) {
        this();

        this.id = id;
        this.priority = converter.priority();
        this.command = converter.command();
        this.inputPath = converter.inputPath().getParent().toString();
        this.fileName = converter.inputPath().getFileName().toString();
        this.completeCallback = converter.completeCallBack();
        this.outputs = converter.outputs();
        this.hwAccel = converter.hwAccel();
        this.running = converter.isRunning();
        this.done = converter.isDone();
        this.addTime = converter.addTime();
        this.startTime = converter.startTime();
        this.endTime = converter.endTime();
        this.exitValue = converter.exitValue();
        this.outputStream = converter.outputStream();
        this.errorStream = converter.errorStream();
    }

    @XmlAttribute(name = "id")
    public String getId() {
        return id;
    }

    @XmlElement(name = "command")
    public String getCommand() {
        return command;
    }

    @XmlAttribute(name = "inputPath")
    public String getInputPath() {
        return inputPath;
    }

    @XmlAttribute(name = "file")
    public String getFileName() {
        return fileName;
    }

    @XmlElement(name = "completeCallback")
    public String getCompleteCallback() {
        return completeCallback;
    }

    @XmlElement(name = "files")
    public List<File> getFiles() {
        return Optional.ofNullable(outputs)
            .map(os -> os.stream()
                .map(o -> new File(o.getOutputPath().getFileName().toString(), o.getFormat(), o.getVideo().getScale()))
                .collect(Collectors.toList()))
            .orElse(null);
    }

    @XmlElement(name = "hwAccel")
    public HWAccelWrapper<? extends HWAccelDeviceSpec> getHwAccel() {
        return hwAccel;
    }

    public void setHwAccel(HWAccelWrapper<? extends HWAccelDeviceSpec> hwAccel) {
        this.hwAccel = hwAccel;
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

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @param inputPath the inputPath to set
     */
    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @param completeCallback the completeCallBack to set
     */
    public void setCompleteCallback(String completeCallback) {
        this.completeCallback = completeCallback;
    }

    /**
     * @param outputs the outputs to set
     */
    public void setOutputs(List<Output> outputs) {
        this.outputs = outputs;
    }

    /**
     * @param running the running to set
     */
    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * @param done the done to set
     */
    public void setDone(boolean done) {
        this.done = done;
    }

    /**
     * @param addTime the addTime to set
     */
    public void setAddTime(String addTime) {
        this.addTime = Instant.parse(addTime);
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(String startTime) {
        this.startTime = Instant.parse(startTime);
    }

    /**
     * @param endTime the endTime to set
     */
    public void setEndTime(String endTime) {
        this.endTime = Instant.parse(endTime);
    }

    /**
     * @param exitValue the exitValue to set
     */
    public void setExitValue(Integer exitValue) {
        this.exitValue = exitValue;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    /**
     * @param outputStream the outputStream to set
     */
    public void setOutputStream(String outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * @param errorStream the errorStream to set
     */
    public void setErrorStream(String errorStream) {
        this.errorStream = errorStream;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    public ConverterWrapper basicCopy() {
        final ConverterWrapper copy = new ConverterWrapper();

        copy.id = this.id;
        copy.priority = this.priority;
        copy.fileName = this.fileName;
        copy.outputs = this.outputs;
        copy.hwAccel = Optional.ofNullable(this.hwAccel).map(HWAccelWrapper::basicCopy).orElse(null);
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

        @XmlAttribute(name = "percent")
        private Integer percent;

        @XmlAttribute(name = "elapsed")
        private String elapsed;

        @XmlAttribute(name = "estimate")
        private String estimate;

        protected Progress() {
        }

        protected static Progress buildProgress(final ConverterWrapper wrapper) {
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

        protected static String formatDuration(final Duration dur) {
            return formatDuration(dur.toNanos());
        }

        protected static String formatDuration(final long nanos) {
            long absSeconds = Math.abs(nanos / 1000000000);
            String positive = String.format(Locale.ROOT, "%d:%02d:%02d", absSeconds / 3600, (absSeconds % 3600) / 60,
                absSeconds % 60);
            return nanos < 0 ? "-" + positive : positive;
        }

        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(Progress p) {
            return this.percent == null ? -1 : p.percent == null ? 1 : this.percent.compareTo(p.percent);
        }
    }

    @XmlRootElement
    static class File {

        private String fileName;

        private String format;

        private String scale;

        protected File() {
        }

        /**
         * @param fileName
         * @param format
         * @param scale
         */
        public File(String fileName, String format, String scale) {
            this.fileName = fileName;
            this.format = format;
            this.scale = scale;
        }

        /**
         * @return the fileName
         */
        @XmlAttribute(name = "name")
        public String getFileName() {
            return fileName;
        }

        /**
         * @param fileName the fileName to set
         */
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        /**
         * @return the format
         */
        @XmlAttribute(name = "format")
        public String getFormat() {
            return format;
        }

        /**
         * @param format the format to set
         */
        public void setFormat(String format) {
            this.format = format;
        }

        /**
         * @return the scale
         */
        @XmlAttribute(name = "scale")
        public String getScale() {
            return scale;
        }

        /**
         * @param scale the scale to set
         */
        public void setScale(String scale) {
            this.scale = scale;
        }

    }
}
