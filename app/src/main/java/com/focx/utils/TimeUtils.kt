package com.focx.utils

object TimeUtils {
    
    /**
     * Convert seconds to human-readable time format
     * @param seconds Number of seconds
     * @return Formatted time string, e.g. "14 days" or "1 day 2 hours 30 minutes"
     */
    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "0 seconds"
        
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        
        return when {
            days > 0 -> {
                when {
                    hours > 0 -> "${days} day${if (days > 1) "s" else ""} ${hours} hour${if (hours > 1) "s" else ""}"
                    minutes > 0 -> "${days} day${if (days > 1) "s" else ""} ${minutes} minute${if (minutes > 1) "s" else ""}"
                    else -> "${days} day${if (days > 1) "s" else ""}"
                }
            }
            hours > 0 -> {
                when {
                    minutes > 0 -> "${hours} hour${if (hours > 1) "s" else ""} ${minutes} minute${if (minutes > 1) "s" else ""}"
                    else -> "${hours} hour${if (hours > 1) "s" else ""}"
                }
            }
            minutes > 0 -> {
                when {
                    remainingSeconds > 0 -> "${minutes} minute${if (minutes > 1) "s" else ""} ${remainingSeconds} second${if (remainingSeconds > 1) "s" else ""}"
                    else -> "${minutes} minute${if (minutes > 1) "s" else ""}"
                }
            }
            else -> "${remainingSeconds} second${if (remainingSeconds > 1) "s" else ""}"
        }
    }
    
    /**
     * Convert seconds to days (for display)
     * @param seconds Number of seconds
     * @return Number of days
     */
    fun secondsToDays(seconds: Long): Int {
        return (seconds / 86400).toInt()
    }
    
    /**
     * Convert seconds to hours
     * @param seconds Number of seconds
     * @return Number of hours
     */
    fun secondsToHours(seconds: Long): Int {
        return (seconds / 3600).toInt()
    }
    
    /**
     * Check if unstake request is ready for withdrawal
     * @param requestTime The time when unstake was requested (seconds)
     * @param lockupPeriod The lockup period in seconds
     * @return true if the lockup period has expired
     */
    fun isUnstakeReady(requestTime: Long, lockupPeriod: Long): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        val expiryTime = requestTime + lockupPeriod
        return currentTime >= expiryTime
    }
    
    /**
     * Get expiry time for unstake request
     * @param requestTime The time when unstake was requested (seconds)
     * @param lockupPeriod The lockup period in seconds
     * @return Expiry time in seconds
     */
    fun getUnstakeExpiryTime(requestTime: Long, lockupPeriod: Long): Long {
        return requestTime + lockupPeriod
    }
    
    /**
     * Format timestamp to readable date time
     * @param timestamp Unix timestamp in seconds
     * @return Formatted date time string
     */
    fun formatExpiryTime(timestamp: Long): String {
        val date = java.util.Date(timestamp * 1000)
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.ENGLISH)
        return formatter.format(date)
    }
}
