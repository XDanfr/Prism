package me.xdan.prism.compiler

import android.content.Context
import com.android.apksig.ApkSigner
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date

class ApkSignerHelper(private val context: Context) {

    companion object {
        private const val KEY_ALIAS = "prism-generated"
        private const val KEY_FILE = "generated-app-signing-key.pk8"
        private const val CERT_FILE = "generated-app-signing-cert.der"

        private val provider = BouncyCastleProvider()

        init {
            Security.removeProvider("BC")
            Security.insertProviderAt(provider, 1)
        }
    }

    fun sign(inputApk: File, outputApk: File, keyPair: KeyPair, certificate: X509Certificate) {
        val signerConfig = ApkSigner.SignerConfig.Builder(KEY_ALIAS, keyPair.private, listOf(certificate)).build()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }

    fun getOrCreateSigningCredentials(): Pair<KeyPair, X509Certificate> {
        val keyFile = File(context.noBackupFilesDir, KEY_FILE)
        val certFile = File(context.noBackupFilesDir, CERT_FILE)
        if (keyFile.exists() && certFile.exists()) {
            return runCatching { loadCredentials(keyFile, certFile) }.getOrElse {
                keyFile.delete()
                certFile.delete()
                generateAndStore(keyFile, certFile)
            }
        }
        return generateAndStore(keyFile, certFile)
    }

    private fun generateAndStore(keyFile: File, certFile: File): Pair<KeyPair, X509Certificate> {
        val credentials = generateKeyPairAndCertificate()
        keyFile.writeBytes(credentials.first.private.encoded)
        certFile.writeBytes(credentials.second.encoded)
        return credentials
    }

    private fun loadCredentials(keyFile: File, certFile: File): Pair<KeyPair, X509Certificate> {
        val keySpec = PKCS8EncodedKeySpec(keyFile.readBytes())
        val privateKey = KeyFactory.getInstance("RSA", provider).generatePrivate(keySpec)
        val certificate = CertificateFactory.getInstance("X.509").generateCertificate(certFile.inputStream()) as X509Certificate
        return KeyPair(certificate.publicKey, privateKey) to certificate
    }

    private fun generateKeyPairAndCertificate(): Pair<KeyPair, X509Certificate> {
        val generator = KeyPairGenerator.getInstance("RSA", provider)
        generator.initialize(2048)
        val keyPair = generator.generateKeyPair()
        val issuer = X500Name("CN=Prism")
        val serial = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date()
        val notAfter = Date(notBefore.time + 30L * 365 * 24 * 60 * 60 * 1000)
        val builder = JcaX509v3CertificateBuilder(
            issuer,
            serial,
            notBefore,
            notAfter,
            issuer,
            keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(provider)
            .build(keyPair.private)
        val certificate = JcaX509CertificateConverter()
            .setProvider(provider)
            .getCertificate(builder.build(signer))
        return keyPair to certificate
    }
}
