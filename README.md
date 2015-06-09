# My-Wallet-HD-Android

Next-generation HD (bip32/bip44) bitcoin wallet. 

## Code Branches 

### master

Contains first iteration of UI defined Q4 of 2014. Development on this branch stopped on 20 March 2015. Do not use.

## Build Process

Install Android Studio: https://developer.android.com/sdk/index.html

Import as Android Studio project.

Build -> Make Project

If there are build errors, in Android Studio go to Tools -> Android -> SDK Manager and install any available updates.

### Notes

info.blockchain.wallet.hd: HD classes that wrap-around BitcoinJ and provide full BIP39/BIP44 support. Fully tested for compatibility with known BIP44 wallets: Mycelium, Wallet32, Coinomi, Trezor (hardware), Ledger (hardware) Includes code for initializing accounts using XPUB values.

info.blockchain.wallet.payload: Up-to-date with latest changes from Sjors (as of 9 April 2015).

info.blockchain.wallet.util: CharSequenceX.java: secure string class that can be used for password, passphrases, etc. Wipes its memory space when going out of scope.

