# YNAB 4 to GnuCash Migration Tool

While YNAB 4 has QIF export and GnuCash has QIF import, migrating your YNAB data to GnuCash is impossible without having to perform some QIF manipulations.
- YNAB Account names are missing from its exported QIFs.
- GnuCash expects all accounts to be placed in the category QIF transaction item ("L"), but YNAB places transfers & payees in the payee QIF transaction item ("P").
- Transfers appear twice in the exported QIFs (once for each account).
- Naming is not always correct or consistent, for example transfer to account X is named Transfer: X instead of just X.
- YNAB exports each account into its own QIF, making the import process inconvenient if there are many accounts.
<p>&nbsp;</p>
This tool does the manipulations and fixes the aforementioned issues.

# Assumptions
- Due to the way YNAB 4 exports to QIF, split transcations are supported if they contain expense sub-transcations only.
Split transcations that contain payee or transfers or expense with positive value (income) are not supported.
- YNAB account names should not contain invalid filename characters (such as '"', '?' ...)
- Ensure there is no clash between Account & Payees & Categories names in YNAB. Since everything is translated into accounts in GnuCash, having a YNAB Payee called "Presents" and a category called "Presents" will mess things up.

# Usage
1. While in "All Accounts" register view in YNAB 4, go to File -> Export. Uncheck "Export current register view only" and click Export to QIF
2. Locate the export folder and run the tool with the folder path as an argument, for example: `sbt "run C:\Users\usera\Dropbox\YNAB\Exports"`
3. A file named ynab.qif should appear inside the Export folder. Import only this file in GnuCash.

# To-Do
- Control the type of the created accounts in GnuCash. I Couldn't find how to force specific account types through QIF import in GnuCash.
