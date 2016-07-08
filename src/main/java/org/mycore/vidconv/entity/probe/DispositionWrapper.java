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
package org.mycore.vidconv.entity.probe;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "disposition")
@XmlAccessorType(XmlAccessType.FIELD)
public class DispositionWrapper {

    @XmlAttribute(name = "clean_effects")
    private String cleanEffects;

    @XmlAttribute(name = "karaoke")
    private String karaoke;

    @XmlAttribute(name = "default")
    private String _default;

    @XmlAttribute(name = "hearing_impaired")
    private String hearingImpaired;

    @XmlAttribute(name = "original")
    private String original;

    @XmlAttribute(name = "dub")
    private String dub;

    @XmlAttribute(name = "comment")
    private String comment;

    @XmlAttribute(name = "lyrics")
    private String lyrics;

    @XmlAttribute(name = "attached_pic")
    private String attachedPic;

    @XmlAttribute(name = "forced")
    private String forced;

    @XmlAttribute(name = "visual_impaired")
    private String visualImpaired;

    /**
     * @return the cleanEffects
     */
    public String getCleanEffects() {
        return cleanEffects;
    }

    /**
     * @param cleanEffects the cleanEffects to set
     */
    public void setCleanEffects(String cleanEffects) {
        this.cleanEffects = cleanEffects;
    }

    /**
     * @return the karaoke
     */
    public String getKaraoke() {
        return karaoke;
    }

    /**
     * @param karaoke the karaoke to set
     */
    public void setKaraoke(String karaoke) {
        this.karaoke = karaoke;
    }

    /**
     * @return the _default
     */
    public String getDefault() {
        return _default;
    }

    /**
     * @param _default the _default to set
     */
    public void setDefault(String _default) {
        this._default = _default;
    }

    /**
     * @return the hearingImpaired
     */
    public String getHearingImpaired() {
        return hearingImpaired;
    }

    /**
     * @param hearingImpaired the hearingImpaired to set
     */
    public void setHearingImpaired(String hearingImpaired) {
        this.hearingImpaired = hearingImpaired;
    }

    /**
     * @return the original
     */
    public String getOriginal() {
        return original;
    }

    /**
     * @param original the original to set
     */
    public void setOriginal(String original) {
        this.original = original;
    }

    /**
     * @return the dub
     */
    public String getDub() {
        return dub;
    }

    /**
     * @param dub the dub to set
     */
    public void setDub(String dub) {
        this.dub = dub;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return the lyrics
     */
    public String getLyrics() {
        return lyrics;
    }

    /**
     * @param lyrics the lyrics to set
     */
    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    /**
     * @return the attachedPic
     */
    public String getAttachedPic() {
        return attachedPic;
    }

    /**
     * @param attachedPic the attachedPic to set
     */
    public void setAttachedPic(String attachedPic) {
        this.attachedPic = attachedPic;
    }

    /**
     * @return the forced
     */
    public String getForced() {
        return forced;
    }

    /**
     * @param forced the forced to set
     */
    public void setForced(String forced) {
        this.forced = forced;
    }

    /**
     * @return the visualImpaired
     */
    public String getVisualImpaired() {
        return visualImpaired;
    }

    /**
     * @param visualImpaired the visualImpaired to set
     */
    public void setVisualImpaired(String visualImpaired) {
        this.visualImpaired = visualImpaired;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DispositionWrapper [");
        if (cleanEffects != null) {
            builder.append("cleanEffects=");
            builder.append(cleanEffects);
            builder.append(", ");
        }
        if (karaoke != null) {
            builder.append("karaoke=");
            builder.append(karaoke);
            builder.append(", ");
        }
        if (_default != null) {
            builder.append("_default=");
            builder.append(_default);
            builder.append(", ");
        }
        if (hearingImpaired != null) {
            builder.append("hearingImpaired=");
            builder.append(hearingImpaired);
            builder.append(", ");
        }
        if (original != null) {
            builder.append("original=");
            builder.append(original);
            builder.append(", ");
        }
        if (dub != null) {
            builder.append("dub=");
            builder.append(dub);
            builder.append(", ");
        }
        if (comment != null) {
            builder.append("comment=");
            builder.append(comment);
            builder.append(", ");
        }
        if (lyrics != null) {
            builder.append("lyrics=");
            builder.append(lyrics);
            builder.append(", ");
        }
        if (attachedPic != null) {
            builder.append("attachedPic=");
            builder.append(attachedPic);
            builder.append(", ");
        }
        if (forced != null) {
            builder.append("forced=");
            builder.append(forced);
            builder.append(", ");
        }
        if (visualImpaired != null) {
            builder.append("visualImpaired=");
            builder.append(visualImpaired);
        }
        builder.append("]");
        return builder.toString();
    }

}
