package com.example.finwise

object MerchantHelper {

    private val keywordCategories = mapOf(
        // Food
        "zomato" to "Food",
        "swiggy" to "Food",
        "kfc" to "Food",
        "pizza" to "Food",
        "dominos" to "Food",
        "burger" to "Food",
        "starbucks" to "Food",
        
        // Transport
        "uber" to "Transport",
        "ola" to "Transport",
        "rapido" to "Transport",
        "petrol" to "Transport",
        "fuel" to "Transport",
        "shell" to "Transport",
        "hpcl" to "Transport",
        
        // Shopping
        "amazon" to "Shopping",
        "flipkart" to "Shopping",
        "myntra" to "Shopping",
        "zudio" to "Shopping",
        "trends" to "Shopping",
        
        // Entertainment
        "netflix" to "Entertainment",
        "prime" to "Entertainment",
        "pvr" to "Entertainment",
        "bookmyshow" to "Entertainment",
        
        // Utilities
        "jio" to "Utilities",
        "airtel" to "Utilities",
        "bescom" to "Utilities",
        "billdesk" to "Utilities",
        
        // Groceries
        "dmart" to "Groceries",
        "bigbasket" to "Groceries",
        "blinkit" to "Groceries"
    )

    fun getCategoryFromMessage(message: String): String {
        val lowerMessage = message.lowercase()
        
        for ((keyword, category) in keywordCategories) {
            if (lowerMessage.contains(keyword)) {
                return category
            }
        }
        
        return "Uncategorized"
    }
    
    /**
     * Extract merchant/vendor name from SMS message.
     * Looks for common patterns like "at [merchant]" or "to [merchant]"
     */
    fun getMerchantName(message: String): String {
        // Try to find merchant after "at" or "to" keywords
        val patterns = listOf(
            Regex("""(?:at|to)\s+([A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\s+via|\.|\s*$)""", RegexOption.IGNORE_CASE),
            Regex("""(?:spent at|paid to|paid at)\s+([A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\s+via|\.|\s*$)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        // Fallback: check for known keywords
        val lowerMessage = message.lowercase()
        for ((keyword, _) in keywordCategories) {
            if (lowerMessage.contains(keyword)) {
                return keyword.replaceFirstChar { it.uppercase() }
            }
        }
        
        return "Expense"
    }
}
