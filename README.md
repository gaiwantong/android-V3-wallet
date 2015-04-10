Branch 'master':

Build contains first iteration of UI defined Q4 of 2014. Development on this branch stopped on 20 March 2015. Do not use.

Branch 'UI2':

Build contains UI as defined in 'Bitcoin Wallet (Android) Design Spec 1.3'.

To build:

Download repo 'UI2' and import as Android Studio project.

Notes:

info.blockchain.wallet.hd: HD classes that wrap-around BitcoinJ and provide full BIP39/BIP44 support. Fully tested for compatibility with known BIP44 wallets: Mycelium, Wallet32, Coinomi, Trezor (hardware), Ledger (hardware) Includes code for initializing accounts using XPUB values.

info.blockchain.wallet.payload: Up-to-date with latest changes from Sjors (as of 9 April 2015).

info.blockchain.wallet.util: CharSequenceX.java: secure string class that can be used for password, passphrases, etc. Wipes its memory space when going out of scope.

