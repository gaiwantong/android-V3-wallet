# My-Wallet-HD-Android

Next-generation HD (BIP32, BIP39, BIP44) bitcoin wallet. 

## Build Process

Install Android Studio: https://developer.android.com/sdk/index.html

Import as Android Studio project.

Build -> Make Project

If there are build errors, in Android Studio go to Tools -> Android -> SDK Manager and install any available updates.

### Notes

Bitcoinj version used is 0.13.2 . Compiled from repo source.

HD classes extending Bitcoinj for BIP44 supplied using https://github.com/blockchain/bitcoinj-bip44-extension : full BIP39/BIP44 support, including passphrases. Fully tested for compatibility with known BIP44 wallets: Mycelium, Wallet32, Coinomi, Trezor (hardware), Ledger (hardware) Includes support for initializing accounts using XPUB values.

info.blockchain.wallet.payload: Up-to-date with latest changes from Sjors (as of 9 April 2015).

info.blockchain.wallet.util: CharSequenceX.java: secure string class that can be used for password, passphrases, etc. Wipes its memory space when going out of scope.

## Unit Tests

Unit tests can be run through Android Studio using the Android emulator with results viewed in the console.

Source code is located at src/androidTest

Included test classes are:

AESTest.java : tests AES-256 encryption/decryption
CreateHDWalletTest.java : tests creating BIP44 wallets with and without passphrases
RestoreHDWalletTest.java : tests restoring BIP44 wallets with and without passphrases
PairingTest.java : tests formatting of strings returned by server when pairing device
DoubleEncryptionTest.java : tests hash calculation and double password encryption/decryption
SSLVerifierTest.java : certificate pinning test and hostname verification test
BlockchainWalletTest.java : tests login via password and pin code
SendTest.java : tests reading of unspent outputs and making transactions

### Running Unit Tests

Run package info.blockchain.wallet.ApplicationTest from Android Studio using emulator. Unit test results will appear in device console.

Use AssertUtil.java to exit or not test suite upon assertion failure.

Use LogUtil.java turn on/off logging in device console.

### Notes

BlockchainWalletTest.java requires an actual paired wallet to function correctly. Currently, the test suite uses the wallet corresponding to user guid 524b5e9f-72ea-4690-b28c-8c1cfce65ca0. The password and other values shared with the server are in info.blockchain.credentials.WalletUtil.java

The wallet environment is established by setting the following Android shared preferences to their correct values: 

KEY_GUID
KEY_SHARED_KEY
KEY_PIN_IDENTIFIER
KEY_ENCRYPTED_PASSWORD

KEY_PIN_IDENTIFIER and KEY_ENCRYPTED_PASSWORD will change as soon as the wallet is re-paired. In the event of a wallet re-pair the source code must be changed with live values read on the console from AccessFactory.java and Payload.java.

SendTest.java : uses the same test wallet as BlockchainWalletTest.java and requires funds to be available for retrieving unspent outputs and making a transaction for a spend of 0.0001 BTC. The HD and legacy addresses that must be funded are in info.blockchain.credentials.WalletUtil.java

### Security

Security issues can be reported to us in the following venues:
* Email: security@blockchain.info
* Bug Bounty: https://www.crowdcurity.com/blockchain-info
