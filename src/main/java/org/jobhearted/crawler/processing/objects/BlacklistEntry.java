package org.jobhearted.crawler.processing.objects;

import org.javalite.activejdbc.Model;

import java.util.LinkedList;
import java.util.List;

/**
 * Model for the blacklist Entry class. Loads data for the blacklist class to use
 */
public class BlacklistEntry extends Model {
    // Database fields
    private static final String COL_CRAWLER_ID = "crawler_id";
    private static final String COL_WORD = "word";

    // Model validators
    static {
        validatePresenceOf(COL_CRAWLER_ID, COL_WORD);
        validateNumericalityOf(COL_CRAWLER_ID);
    }

    /**
     * Reads all entries of the blacklist for the specified crawler id from the database
     * and returns it
     *
     * @param crawlerId id of the crawler
     * @return All entries of the blacklist
     */
    public static List<BlacklistEntry> loadAllEntriesForCrawlerId(int crawlerId) {
        List<BlacklistEntry> result = new LinkedList<BlacklistEntry>();
        for (Model entry : BlacklistEntry.find(COL_CRAWLER_ID + " = ?", crawlerId)) {
            result.add((BlacklistEntry) entry);
        }
        return result;
    }
}
