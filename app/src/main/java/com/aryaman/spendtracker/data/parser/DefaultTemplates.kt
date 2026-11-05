package com.aryaman.spendtracker.data.parser

import com.aryaman.spendtracker.data.local.entity.SmsTemplateEntity
import com.aryaman.spendtracker.data.model.TransactionDirection

object DefaultTemplates {

    fun getList(): List<SmsTemplateEntity> {
        return listOf(
            SmsTemplateEntity(
                bankName = "HDFC Bank (Pixel Play CC UPI)",
                senderFilter = "HDFC",
                regexPattern = """(?:transaction of|spent)\s+(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s+was made using your HDFC Bank\s+(?<account>.+?)\s+at\s+(?<merchant>.+?)\s+via UPI\s+(?<ref>\d+)\s+on\s+(?<date>\d{2}/\d{2}/\d{2})\s+at\s+(?<time>\d{2}:\d{2})""",
                direction = TransactionDirection.DEBIT,
                sampleText = "A transaction of Rs. 179.49 was made using your HDFC Bank Pixel Play Credit Card at zomato.eternaltsp.payu@hdfcbank via UPI 626963138975 on 26/09/26 at 19:44",
                isCreditCard = true
            ),
            SmsTemplateEntity(
                bankName = "HDFC Bank (Credit Card Spend)",
                senderFilter = "HDFC",
                regexPattern = """(?:spent|using|spent\s+Rs\.?|paid)\s+(?:Rs\.?|INR)?\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s+(?:on|using|for)?\s*HDFC\s+Bank\s+(?:Credit\s+Card|Card)\s*(?:ending)?\s*\*?(?<account>\w+)?\s+(?:at|to|for|on)\s+(?<merchant>[A-Za-z0-9\s._-]+?)(?:\s+on|\.|\s+at|\s+via|\s+Bal|$)""",
                direction = TransactionDirection.DEBIT,
                sampleText = "Alert: You've spent Rs.1,500.00 on HDFC Bank Credit Card ending 1234 at AMAZON on 24-09-26. Bal: Rs.45,000",
                isCreditCard = true
            ),
            SmsTemplateEntity(
                bankName = "HDFC Bank (UPI Debit)",
                senderFilter = "HDFC",
                regexPattern = """Sent\s+(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s+From\s+HDFC\s+Bank\s+A/C\s*\*?(?<account>\w+)\s+To\s+(?<merchant>.+?)\s+On\s+(?<date>\d{2}/\d{2}/\d{2})\s+Ref\s+(?<ref>\d+)""",
                direction = TransactionDirection.DEBIT,
                sampleText = "Sent Rs.25.00\nFrom HDFC Bank A/C *5164\nTo ANUGRHA STORE\nOn 26/09/26\nRef 626954558817",
                isCreditCard = false
            ),
            SmsTemplateEntity(
                bankName = "HDFC Bank (UPI Credit)",
                senderFilter = "HDFC",
                regexPattern = """Received\s+(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s+in\s+HDFC\s+Bank\s+A/C\s*\*?(?<account>\w+)\s+From\s+(?<merchant>.+?)\s+On\s+(?<date>\d{2}/\d{2}/\d{2})\s+Ref\s+(?<ref>\d+)""",
                direction = TransactionDirection.CREDIT,
                sampleText = "Received Rs.500.00 in HDFC Bank A/C *5164 From RAMESH On 25/09/26 Ref 626849833983",
                isCreditCard = false
            ),
            SmsTemplateEntity(
                bankName = "HDFC Bank (Account Debit)",
                senderFilter = "HDFC",
                regexPattern = """(?:Rs\.?|INR)\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s+(?:debited|spent|deducted).*?(?:a/c|card|account)\s*(?:\*+|xx)?(?<account>\w+)?.*?(?:to|at|info)\s+(?<merchant>[A-Za-z0-9\s._-]+?)(?:\.|\s+on|\s+bal|\s+avail|$)""",
                direction = TransactionDirection.DEBIT,
                sampleText = "Rs.450.00 debited from a/c **5164 on 24-09-26 to VENDOR. Bal: Rs.12,000",
                isCreditCard = false
            ),
            SmsTemplateEntity(
                bankName = "SBI (UPI Debit)",
                senderFilter = "SBI",
                regexPattern = """Dear UPI user\s+A/C\s*\*?X*(?<account>\w+)\s+debited by\s+(?:Rs\.?|INR)?\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s+on date\s+(?<date>[A-Za-z0-9/-]+)\s+trf to\s+(?<merchant>.+?)\s+Ref\s*no\s*(?<ref>\d+)""",
                direction = TransactionDirection.DEBIT,
                sampleText = "Dear UPI user A/C X1618 debited by 25.00 on date 28Sep26 trf to HungerBox Refno 125802739536 If not u? call-1800111109 -SBI",
                isCreditCard = false
            ),
            SmsTemplateEntity(
                bankName = "SBI (UPI Credit)",
                senderFilter = "SBI",
                regexPattern = """your A/c\s*(?:X+)?(?<account>\w+)[-\s]+credited by\s+(?:Rs\.?|INR)?\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s+on\s+(?<date>[A-Za-z0-9/-]+)\s+transfer from\s+(?<merchant>.+?)\s+Ref(?:\s*No)?\s*(?<ref>\d+)""",
                direction = TransactionDirection.CREDIT,
                sampleText = "Dear UPI User, your A/c XXXXXX1618-credited by Rs.533.00 on 29-05-26 transfer from ARUSHI Ref No 614972592157 -SBI",
                isCreditCard = false
            ),
            SmsTemplateEntity(
                bankName = "SBI (Account Debit)",
                senderFilter = "SBI",
                regexPattern = """(?:A/C|Account)\s*(?:X+)?(?<account>\w+)\s+(?:debited by|is debited for)\s+(?:Rs\.?|INR)?\s*(?<amount>[\d,]+(?:\.\d{1,2})?)\s+(?:on|on date)\s+(?<date>[A-Za-z0-9/-]+)(?:\s+by transfer to|\s+to|\s+at)\s+(?<merchant>[A-Za-z0-9\s._-]+?)(?:\.|\s+Ref|\s+Bal|$)""",
                direction = TransactionDirection.DEBIT,
                sampleText = "A/C X1618 is debited for Rs.1,200.00 on 28-09-26 to SWIGGY. Bal: Rs.5,000",
                isCreditCard = false
            )
        )
    }
}
