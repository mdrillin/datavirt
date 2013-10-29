package org.jboss.datavirtualization.server.rpc;

public final class StringUtil {

	/**
     * <p>
     * Returns whether the specified text is either empty or null.
     * </p>
     * 
     * @param text The text to check; may be null;
     * @return True if the specified text is either empty or null.
     * @since 4.0
     */
    public static boolean isEmpty( final String text ) {
        return (text == null || text.length() == 0);
    }

    /**
     * Compare string values - considered equal if either are null or empty.
     * 
     * @param thisValue the first value being compared (can be <code>null</code> or empty)
     * @param thatValue the other value being compared (can be <code>null</code> or empty)
     * @return <code>true</code> if values are equal or both values are empty
     */
    public static boolean valuesAreEqual( String thisValue,
                                          String thatValue ) {
        if (isEmpty(thisValue) && isEmpty(thatValue)) {
            return true;
        }

        return equals(thisValue, thatValue);
    }
    
    /**
     * @param thisString the first string being compared (may be <code>null</code>)
     * @param thatString the other string being compared (may be <code>null</code>)
     * @return <code>true</code> if the supplied strings are both <code>null</code> or have equal values
     */
    public static boolean equals( final String thisString,
                                  final String thatString ) {
        if (thisString == null) {
            return (thatString == null);
        }

        return thisString.equals(thatString);
    }

}
