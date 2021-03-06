package org.jobhearted.crawler.processing.objects;

import org.javalite.activejdbc.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The vacature data class. This is an ActiveJDBC data-object.
 */
public class Vacature extends Model implements Locatable {
    // Database fields
    public static final String COL_URL_ID = "url_id";
    public static final String COL_HASH = "hash";
    public static final String COL_TITLE = "title";
    public static final String COL_BEDRIJF = "bedrijf";
    public static final String COL_DIENSTVERBAND = "dienstverband";
    public static final String COL_PLAATS = "plaats";
    public static final String COL_OMSCHRIJVING = "omschrijving";
    public static final String COL_VERSION = "version";
    public static final String COL_ACTIVE = "active";

    // model validators
    static {
        validatePresenceOf(COL_URL_ID, COL_HASH, COL_VERSION, COL_ACTIVE, COL_OMSCHRIJVING);
        validateNumericalityOf(COL_URL_ID, COL_VERSION, COL_ACTIVE);
    }

    private static final Map<ProcessData, String> DATABASE_MAP = createDatabaseMap();
    private static Logger logger = LoggerFactory.getLogger(Vacature.class);

    /**
     * Initializes the datamap of the vacature model, which happens at startup.
     *
     * @return the Hashmap
     */
    private static Map<ProcessData, String> createDatabaseMap() {
        Map<ProcessData, String> map = new HashMap<ProcessData, String>();

        map.put(ProcessData.VAC_BEDRIJF, COL_BEDRIJF);
        map.put(ProcessData.VAC_DIENSTVERBAND, COL_DIENSTVERBAND);
        map.put(ProcessData.VAC_OMSCHRIJVING, COL_OMSCHRIJVING);
        map.put(ProcessData.VAC_TITLE, COL_TITLE);
        map.put(ProcessData.VAC_PLAATS, COL_PLAATS);

        return map;
    }

    /**
     * Sets the given data key to the given value, using the DATABASE_MAP field, which mapped all keys to their respective
     * fields.
     *
     * @param data  The key to set
     * @param value The value to set
     */
    public void putProperty(ProcessData data, String value) {
        if (data == ProcessData.REQUIREMENTFORVACATURE) {
            logger.warn("You tried to set a requirement on a vacature. This should NEVER happen!");
            return;
        }
        this.setString(DATABASE_MAP.get(data), value);
    }

    /**
     * Generates the hash field of the vacature and sets it.
     */
    public void generateHash() {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(this.getString(COL_OMSCHRIJVING).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            this.setString(COL_HASH, sb.toString());
        } catch (java.security.NoSuchAlgorithmException e) {
            logger.warn("No algorithm!", e);
        }
    }

    /**
     * Saves the Vacature safely to the database. Checking for doubles in the database by both url id and hash,
     * so there's no double data in the database.
     */
    public boolean saveSafely() {
        // Search if the url of the vacature already has one in the database
        List<Vacature> result = Vacature.find(COL_URL_ID + " = ?", this.getString(COL_URL_ID));
        if (!result.isEmpty()) {
            // already one!
            Vacature vacature = (Vacature) result.toArray()[0];
            if (!vacature.getString(COL_HASH).equals(this.getString(COL_HASH))) {
                // we got a new version! Set the old one inactive
                vacature.setInteger(COL_ACTIVE, 0);
                vacature.removeAllSkills();
                vacature.save();

                // and update the version of the new one
                this.setInteger(COL_VERSION, vacature.getInteger(COL_VERSION) + 1);
                this.save();
            }
        } else {
            // See if a vacature with the same hash is already in the database
            // If so, don't save it, some shitty company put their vacature on the webpage twice.
            List<Vacature> list = Vacature.find(COL_HASH + " = ?", this.getString(COL_HASH));
            if (list.isEmpty()) {
                this.setInteger(COL_VERSION, 1);
                this.setInteger(COL_ACTIVE, 1);
                this.save();
                return true;
            }
        }

        return false;
    }

    /**
     * Removes all the skills associated with the vacature. Mostly needed when a new version is found, or the
     * vacature has been deleted and should not be included in the matcher, which uses this relation
     */
    private void removeAllSkills() {
        for (Skill s : this.getAll(Skill.class)) {
            this.remove(s);
        }
    }

    /**
     * Removes all locations associated with the vacancy.
     */
    private void removeAllLocations() {
        for (Location location : this.getAll(Location.class)) {
            this.remove(location);
        }
    }

    /**
     * Removes all educations associated with the vacancy.
     */
    private void removeAllEducations() {
        for (Education education : this.getAll(Education.class)) {
            this.remove(education);
        }
    }

    /**
     * Adds the skill to the vacature, which is later used in the matcher.
     *
     * @param skill The skill to add
     */
    public void addSkill(Skill skill) {
        this.add(skill);
        logger.debug("Added skill {}", skill.getSkill());
    }

    /**
     * Add the Education to the vacature, which is later user in the matcher.
     *
     * @param education The education to add
     */
    public void addEducation(Education education) {
        this.add(education);
        logger.debug("Added Education {}", education.getEducation());
    }

    public void addLocation(Location location) {
        this.add(location);
        logger.info("Added Location {}", location.getName());
    }

    /**
     * Getter for the omschrijving field of the model
     *
     * @return de omschrijving
     */
    public String getOmschrijving() {
        return this.getString(COL_OMSCHRIJVING);
    }

    /**
     * Setter for the omschrijving field of the model
     *
     * @param omschrijving The value to set to
     */
    public void setOmschrijving(String omschrijving) {
        this.setString(COL_OMSCHRIJVING, omschrijving);
    }

    /**
     * Sets the Id of the url the vacature belongs to
     *
     * @param newId ID to set to
     */
    public void setUrlId(int newId) {
        this.setInteger(COL_URL_ID, newId);
    }

    /**
     * Sets the Active field of the vacature in the database. Sets 0 for inactive and 1 for active.
     *
     * @param active whether it should be set to active or not
     */
    public void setActive(boolean active) {
        if (active) {
            this.setInteger(COL_ACTIVE, 1);
        } else {
            this.setInteger(COL_ACTIVE, 0);
            removeAllSkills();
            removeAllEducations();
            removeAllLocations();
        }
    }

    /**
     * Returns the location of the vacature
     * NOTE: This is the raw location string, not the processed location
     *
     * @return location
     */
    public String getPlaats() {
        return this.getString(COL_PLAATS);
    }

}
